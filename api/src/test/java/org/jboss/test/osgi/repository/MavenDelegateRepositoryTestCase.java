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
package org.jboss.test.osgi.repository;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.impl.ExpressionCombinerImpl;
import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.jboss.osgi.repository.spi.MavenDelegateRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.repository.RequirementBuilder;

/**
 * Test the {@link MavenDelegateRepository}
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 16-Jan-2012
 */
public class MavenDelegateRepositoryTestCase {

    private XRepository repository;

    @Before
    public void setUp() throws IOException {
        repository = new MavenDelegateRepository();
    }

    @Test
    public void testFindProviders() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        XCapability cap = (XCapability) caps.iterator().next();

        Assert.assertTrue("Capability matches", req.matches(cap));

        XResource resource = cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        assertEquals("org.apache.felix.configadmin", icap.getName());
        assertEquals(Version.parseVersion("1.2.8"), icap.getVersion());
        assertEquals("jar", icap.getType());

        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("One capability", 1, caps.size());

        RepositoryContent content = (RepositoryContent) resource;
        Manifest manifest = new JarInputStream(content.getContent()).getManifest();
        OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
        assertEquals("org.apache.felix.configadmin", metaData.getBundleSymbolicName());
        assertEquals(Version.parseVersion("1.2.8"), metaData.getBundleVersion());
    }

    @Test
    public void testFindProvidersFails() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("foo:bar:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("No capability", 0, caps.size());
    }

    @Test
    public void testGetRequirementBuilder() {
        RequirementBuilder builder = repository.newRequirementBuilder("toastie");
        Assert.assertTrue(builder instanceof RequirementBuilderImpl);
        Requirement req = builder.build();
        Assert.assertEquals("toastie", req.getNamespace());
    }

    @Test
    public void testGetExpressionCombiner() {
        Assert.assertTrue(repository.getExpressionCombiner() instanceof ExpressionCombiner);
        Assert.assertTrue(repository.getExpressionCombiner() instanceof ExpressionCombinerImpl);
    }
}