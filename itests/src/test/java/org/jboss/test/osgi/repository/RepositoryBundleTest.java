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
package org.jboss.test.osgi.repository;

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
