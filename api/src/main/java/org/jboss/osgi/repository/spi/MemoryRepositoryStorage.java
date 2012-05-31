/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.osgi.repository.RepositoryMessages;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.spi.AbstractRequirement;
import org.osgi.framework.Filter;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A {@link RepositoryStorage} that maintains its state in local memory
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class MemoryRepositoryStorage implements RepositoryStorage {

    private final XRepository repository;
    private final AtomicLong increment = new AtomicLong();
    private final Map<String, Map<String, Set<XCapability>>> capabilityCache = new HashMap<String, Map<String, Set<XCapability>>>();

    public MemoryRepositoryStorage(XRepository repository) {
        if (repository == null)
            throw MESSAGES.illegalArgumentNull("repository");
        this.repository = repository;
    }

    protected AtomicLong getAtomicIncrement() {
        return increment;
    }

    @Override
    public XRepository getRepository() {
        return repository;
    }

    @Override
    public RepositoryReader getRepositoryReader() {
        synchronized (capabilityCache) {
            return new RepositoryReader() {
                private final Iterator<XResource> iterator;
                {
                    synchronized (capabilityCache) {
                        final Set<XCapability> icaps = getCachedCapabilities(IDENTITY_NAMESPACE, null);
                        final Iterator<XCapability> capit = icaps.iterator();
                        iterator = new Iterator<XResource>() {
                            @Override
                            public boolean hasNext() {
                                return capit.hasNext();
                            }

                            @Override
                            public XResource next() {
                                return (XResource) capit.next().getResource();
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                }

                @Override
                public Map<String, String> getRepositoryAttributes() {
                    HashMap<String, String> attributes = new HashMap<String, String>();
                    attributes.put("name", getRepository().getName());
                    attributes.put("increment", new Long(increment.get()).toString());
                    return Collections.unmodifiableMap(attributes);
                }

                @Override
                public XResource nextResource() {
                    return iterator.hasNext() ? iterator.next() : null;
                }

                @Override
                public void close() {
                    // do nothing
                }
            };
        }
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        Set<Capability> result = findCachedProviders(req);
        LOGGER.tracef("Find cached providers: %s => %s", req, result);
        return result;
    }

    @Override
    public XResource addResource(XResource res) throws RepositoryStorageException {
        if (res == null)
            throw MESSAGES.illegalArgumentNull("resource");

        XIdentityCapability icap = res.getIdentityCapability();
        synchronized (capabilityCache) {
            Set<XCapability> icaps = getCachedCapabilities(IdentityNamespace.IDENTITY_NAMESPACE, icap.getSymbolicName());
            for (XCapability aux : icaps) {
                XIdentityCapability iaux = aux.adapt(XIdentityCapability.class);
                if (icap.getVersion().equals(iaux.getVersion())) {
                    throw MESSAGES.illegalStateResourceAlreadyExists(res);
                }
            }
            for (Capability cap : res.getCapabilities(null)) {
                addCachedCapability((XCapability) cap);
            }
            increment.incrementAndGet();
            LOGGER.infoResourceAdded(res);
        }
        return res;
    }

    @Override
    public XResource addResource(String mime, InputStream input) throws RepositoryStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeResource(XResource res) throws RepositoryStorageException {
        if (res == null)
            throw RepositoryMessages.MESSAGES.illegalArgumentNull("resource");

        boolean found = false;
        synchronized (capabilityCache) {
            for (Capability cap : res.getCapabilities(null)) {
                XCapability xcap = (XCapability) cap;
                String namespace = cap.getNamespace();
                String nsvalue = (String) xcap.getAttribute(namespace);
                Iterator<XCapability> capit = getCachedCapabilities(namespace, nsvalue).iterator();
                while (capit.hasNext()) {
                    Resource auxres = capit.next().getResource();
                    if (res == auxres) {
                        capit.remove();
                        found = true;
                    }
                }
            }
            LOGGER.infoResourceRemoved(res);
        }
        return found;
    }

    private Set<Capability> findCachedProviders(Requirement req) {
        synchronized (capabilityCache) {
            Set<Capability> result = new HashSet<Capability>();
            Set<XCapability> caps = getCachedCapabilities(req.getNamespace(), null);
            for (XCapability cap : caps) {
                if (matches(req, cap))
                    result.add(cap);
            }
            return result;
        }
    }

    private void addCachedCapability(XCapability cap) {
        synchronized (capabilityCache) {
            String namespace = cap.getNamespace();
            Map<String, Set<XCapability>> capmap = capabilityCache.get(namespace);
            if (capmap == null) {
                capmap = new HashMap<String, Set<XCapability>>();
                capabilityCache.put(namespace, capmap);
            }
            String nsvalue = (String) cap.getAttribute(namespace);
            Set<XCapability> capset = capmap.get(nsvalue);
            if (capset == null) {
                capset = new HashSet<XCapability>();
                capmap.put(nsvalue, capset);
            }
            capset.add(cap);
        }
    }

    private Set<XCapability> getCachedCapabilities(String namespace, String nsvalue) {
        synchronized (capabilityCache) {
            Map<String, Set<XCapability>> caps = capabilityCache.get(namespace);
            if (caps == null) {
                caps = new HashMap<String, Set<XCapability>>();
                capabilityCache.put(namespace, caps);
            }
            Set<XCapability> result;
            if (nsvalue != null) {
                result = caps.get(nsvalue);
                if (result == null) {
                    result = new HashSet<XCapability>();
                }
            } else {
                Set<XCapability> allcaps = new HashSet<XCapability>();
                for (Set<XCapability> set : caps.values()) {
                    allcaps.addAll(set);
                }
                result = allcaps;
            }
            return result;
        }
    }

    private boolean matches(Requirement req, Capability cap) {
        boolean result;
        if (req instanceof XRequirement) {
            XRequirement xreq = (XRequirement) req;
            result = xreq.matches(cap);
        } else {
            result = req.getNamespace().equals(cap.getNamespace()) && matchFilter(req, cap);
        }
        return result;
    }

    private boolean matchFilter(Requirement req, Capability cap) {
        Filter filter = AbstractRequirement.getFilterFromDirective(req);
        return filter != null ? filter.match(new Hashtable<String, Object>(cap.getAttributes())) : true;
    }
}