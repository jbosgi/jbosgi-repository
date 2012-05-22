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

import java.util.Collection;

import junit.framework.Assert;

import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.MemoryRepositoryStorage;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Capability;

/**
 * Test the {@link MemoryRepositoryStorage}
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class DefaultRepositoryStorageTestCase extends AbstractRepositoryTest {

    private RepositoryStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new MemoryRepositoryStorage(Mockito.mock(XRepository.class));
        RepositoryReader reader = getRepositoryReader("xml/sample-repository.xml");
        storage.addResource(reader.nextResource());
    }

    @Test
    public void testRequireBundle() throws Exception {

        RepositoryReader reader = storage.getRepositoryReader();
        XResource resource = reader.nextResource();
        Assert.assertNotNull("Resource not null", resource);
        Assert.assertNull("One resource only", reader.nextResource());

        XRequirement req = XRequirementBuilder.createRequirement(BundleNamespace.BUNDLE_NAMESPACE, "org.acme.pool");

        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        Assert.assertNotNull(cap);
        Assert.assertSame(resource, cap.getResource());
    }
}