package org.jboss.osgi.repository;
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

import java.util.Collection;

import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Repository resource storage
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public interface RepositoryStorage {

    /**
     * Get the associated reposistory;
     */
    XRepository getRepository();

    /**
     * Find the capabilities that match the specified requirement.
     *
     * @param requirement The requirements for which matching capabilities should be returned. Must not be {@code null}.
     * @return A collection of matching capabilities for the specified requirements.
     *         If there are no matching capabilities an empty collection is returned.
     *         The returned collection is the property of the caller and can be modified by the caller.
     */
    Collection<Capability> findProviders(Requirement requirement);

    /**
     * Get the repository reader for this storage
     */
    RepositoryReader getRepositoryReader();

    /**
     * Get the resource for the given identity capability.
     *
     * @return The resource or null
     */
    XResource getResource(XIdentityCapability icap);

    /**
     * Add the given resource to storage
     *
     * @param resource The resource to add
     * @return The resource being added, which may be a modified copy of the give resource
     * @throws RepositoryStorageException If there is a problem storing the resource
     */
    XResource addResource(XResource resource) throws RepositoryStorageException;

    /**
     * Remove a the given resource from the cache.
     *
     * @param resource The resource to remove
     * @return true if the resource could be found and removed
     * @throws RepositoryStorageException If there is a problem removing the resource from storage
     */
    boolean removeResource(XResource resource) throws RepositoryStorageException;
}
