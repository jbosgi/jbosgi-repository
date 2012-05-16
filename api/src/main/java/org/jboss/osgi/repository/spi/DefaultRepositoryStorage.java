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

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.osgi.repository.RepositoryMessages;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * A {@link RepositoryStorage} that maintains its state in local memory
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class DefaultRepositoryStorage implements RepositoryStorage {

    private final Map<CacheKey, XResource> resourceCache = new HashMap<CacheKey, XResource>();
    private final Map<CacheKey, Set<Capability>> capabilityCache = new HashMap<CacheKey, Set<Capability>>();

    @Override
    public Iterator<XResource> getResources() {
        // synchronize on capabilityCache intentionally
        synchronized (capabilityCache) {
            return resourceCache.values().iterator();
        }
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        Set<Capability> result = findCachedProviders(CacheKey.create(req), false);
        LOGGER.tracef("Find cached providers: %s => %s", req, result);
        return result;
    }

    @Override
    public XResource addResource(XResource res) throws RepositoryStorageException {
        if (res == null)
            throw MESSAGES.illegalArgumentNull("resource");

        XIdentityCapability icap = res.getIdentityCapability();
        CacheKey ikey = CacheKey.create(icap);
        synchronized (capabilityCache) {
            if (capabilityCache.get(ikey) != null)
                throw MESSAGES.illegalStateResourceAlreadyExists(res);

            resourceCache.put(ikey, res);
            for (Capability cap : res.getCapabilities(null)) {
                CacheKey cachekey = CacheKey.create(cap);
                Set<Capability> capset = findCachedProviders(cachekey, true);
                capset.add(cap);
            }
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

        XResource result = null;
        XIdentityCapability icap = res.getIdentityCapability();
        CacheKey ikey = CacheKey.create(icap);
        synchronized (capabilityCache) {
            result = resourceCache.remove(ikey);
            if (result != null) {
                for (Capability cap : result.getCapabilities(null)) {
                    CacheKey cachekey = CacheKey.create(cap);
                    Set<Capability> capset = findCachedProviders(cachekey, true);
                    capset.remove(cap);
                }
                LOGGER.infoResourceRemoved(res);
            }
        }
        return result != null;
    }

    private Set<Capability> findCachedProviders(CacheKey cachekey, boolean create) {
        synchronized (capabilityCache) {
            Set<Capability> result = capabilityCache.get(cachekey);
            if (result == null) {
                result = new HashSet<Capability>();
                if (create) {
                    capabilityCache.put(cachekey, result);
                }
            }
            return result;
        }
    }

    /**
     * An key to cached capabilities
     */
    static class CacheKey {

        private final String key;

        /**
         * Create a cache key from the given capability
         */
        static CacheKey create(Capability capability) {
            String namespace = capability.getNamespace();
            return new CacheKey(namespace, (String) capability.getAttributes().get(namespace));
        }

        /**
         * Create a cache key from the given requirement
         */
        static CacheKey create(Requirement requirement) {
            String namespace = requirement.getNamespace();
            return new CacheKey(namespace, (String) requirement.getAttributes().get(namespace));
        }

        private CacheKey(String namespace, String value) {
            key = namespace + ":" + value;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            return key.equals(other.key);
        }

        @Override
        public String toString() {
            return "[" + key + "]";
        }
    }
}