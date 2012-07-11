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

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

/**
 * An extension of the {@link Repository} interface
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public interface XRepository extends Repository {

    /**
     * The service names that repositories are registered under
     */
    String[] SERVICE_NAMES = new String[] { XRepository.class.getName(), Repository.class.getName() };

    /**
     * Artifact coordinates may be given in simple groupId:artifactId:version form,
     * or they may be fully qualified in the form groupId:artifactId:type:version[:classifier]
     */
    String MAVEN_IDENTITY_NAMESPACE = "maven.identity";

    /**
     * Artifact coordinates may be given by {@link org.jboss.modules.ModuleIdentifier}
     */
    String MODULE_IDENTITY_NAMESPACE = "module.identity";

    /**
     * Get the name for this repository
     */
    String getName();

    /**
     * Find the capabilities that match the specified requirement.
     *
     * @param requirement The requirements for which matching capabilities
     *        should be returned. Must not be {@code null}.
     * @return A collection of matching capabilities for the specified requirements.
     *         If there are no matching capabilities an empty collection is returned.
     *         The returned collection is the property of the caller and can be modified by the caller.
     */
    Collection<Capability> findProviders(Requirement requirement);
}
