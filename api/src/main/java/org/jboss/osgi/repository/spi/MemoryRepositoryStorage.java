package org.jboss.osgi.repository.spi;
/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

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
import org.jboss.osgi.repository.RepositoryStorageFactory;
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

    public static final class Factory implements RepositoryStorageFactory {
        @Override
        public RepositoryStorage create(XRepository repository) {
            return new MemoryRepositoryStorage(repository);
        }
    }

    public MemoryRepositoryStorage(XRepository repository) {
        if (repository == null)
            throw MESSAGES.illegalArgumentNull("repository");
        this.repository = repository;
    }

    protected AtomicLong getResourceIndex() {
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
                                return capit.next().getResource();
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
                    throw MESSAGES.resourceAlreadyExists(res);
                }
            }
            for (Capability cap : res.getCapabilities(null)) {
                addCachedCapability((XCapability) cap);
            }
            increment.incrementAndGet();
            LOGGER.debugf("Resource added: %s", res);
        }
        return res;
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
            LOGGER.debugf("Resource removed: %s", res);
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