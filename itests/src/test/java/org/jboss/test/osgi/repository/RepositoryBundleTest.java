package org.jboss.test.osgi.repository;
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XResource;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Test OSGi repository access
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2012
 */
public abstract class RepositoryBundleTest {

    private static final AtomicBoolean initialized = new AtomicBoolean();

    @Before
    public void setUp () throws Exception {
        if (initialized.compareAndSet(false, true)) {
            initializeRepository((XPersistentRepository) getRepository());
        }
    }

    protected void initializeRepository(XPersistentRepository repo) throws Exception {
        // remove all resources
        RepositoryStorage storage = (repo).getRepositoryStorage();
        RepositoryReader reader = storage.getRepositoryReader();
        XResource resource = reader.nextResource();
        while (resource != null) {
            storage.removeResource(resource);
            resource = reader.nextResource();
        }
    }

    abstract BundleContext getBundleContext();

    protected XRepository getRepository() {
        BundleContext context = getBundleContext();
        ServiceReference sref = context.getServiceReference(XRepository.class.getName());
        return sref != null ? (XRepository) context.getService(sref) : null;
    }

}
