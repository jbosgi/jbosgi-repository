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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.spi.MavenIdentityRepository;
import org.jboss.osgi.repository.spi.MavenIdentityRepository.ConfigurationPropertyProvider;
import org.jboss.osgi.repository.impl.ExpressionCombinerImpl;
import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.repository.RequirementBuilder;

/**
 * Test the {@link MavenIdentityRepository}
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 16-Jan-2012
 */
public class MavenIdentityRepositoryTestCase extends AbstractRepositoryTest {

    private XRepository repository;
    private RepositoryStorage storage;

    @Before
    public void setUp() throws IOException {
        File storageDir = new File("./target/repository");
        deleteRecursive(storageDir);
        repository = new MavenIdentityRepository();
        ConfigurationPropertyProvider config = Mockito.mock(ConfigurationPropertyProvider.class);
        storage = new FileBasedRepositoryStorage(repository, storageDir, config);
    }

    @Test
    public void testResourceWithValidMetaData() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        Assert.assertEquals("One capability", 1, caps.size());
        XIdentityCapability cap = (XIdentityCapability) caps.iterator().next();

        XResource resource = cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        Assert.assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, icap.getNamespace());
        Assert.assertEquals("org.apache.felix.configadmin", icap.getName());
        Assert.assertEquals(Version.parseVersion("1.2.8"), icap.getVersion());
        Assert.assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());

        // Add the resource to storage and verify again
        resource = storage.addResource(resource);
        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        Assert.assertEquals("One capability", 1, caps.size());

        RepositoryContent content = (RepositoryContent) resource;
        Manifest manifest = new JarInputStream(content.getContent()).getManifest();
        OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
        Assert.assertEquals("org.apache.felix.configadmin", metaData.getBundleSymbolicName());
        Assert.assertEquals(Version.parseVersion("1.2.8"), metaData.getBundleVersion());
    }

    @Test
    public void testResourceWithInvalidMetaData() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("org.hibernate.javax.persistence:hibernate-jpa-2.0-api:1.0.1.Final");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        Assert.assertEquals("No capability", 0, caps.size());
    }

    @Test
    public void testFindProvidersFails() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("foo:bar:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        Assert.assertEquals("No capability", 0, caps.size());
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