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
public class MemoryRepositoryStorageTestCase extends AbstractRepositoryTest {

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

        XRequirementBuilder builder = XRequirementBuilder.create(BundleNamespace.BUNDLE_NAMESPACE, "org.acme.pool");
        XRequirement req = builder.getRequirement();

        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull("Providers not null", providers);
        Assert.assertEquals("One provider", 1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        Assert.assertNotNull("Capability not null", cap);
        Assert.assertSame(resource, cap.getResource());
    }

    @Test
    public void testRequireBundleWithFilter() throws Exception {

        XRequirementBuilder builder = XRequirementBuilder.create(BundleNamespace.BUNDLE_NAMESPACE);
        builder.getDirectives().put(BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.wiring.bundle=org.acme.pool)");
        XRequirement req = builder.getRequirement();

        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull("Providers not null", providers);
        Assert.assertEquals("One provider", 1, providers.size());
    }
}