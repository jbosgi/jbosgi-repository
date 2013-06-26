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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.spi.MavenIdentityRepository.ConfigurationPropertyProvider;
import org.jboss.osgi.repository.spi.ModuleIdentityRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.repository.module.a.Foo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.repository.ContentNamespace;

/**
 * Test the {@link ModuleIdentityRepository}
 *
 * @author thomas.diesler@jboss.com
 * @since 25-Jun-2012
 */
public class ModuleIdentityRepositoryTestCase extends AbstractRepositoryTest {

    private ModuleLoaderSupport moduleLoader;
    private XRepository repository;
    private RepositoryStorage storage;

    @Before
    public void setUp() throws IOException {
        File storageDir = new File("./target/repository");
        deleteRecursive(storageDir);
        moduleLoader = new ModuleLoaderSupport();
        repository = new ModuleIdentityRepository(moduleLoader);
        ConfigurationPropertyProvider config = Mockito.mock(ConfigurationPropertyProvider.class);
        storage = new FileBasedRepositoryStorage(repository, storageDir, config);
    }

    @Test
    public void testResourceWithValidMetaData() throws Exception {
        ModuleIdentifier moduleId = addModuleSpec(moduleLoader, getModuleA());
        XRequirement req = XRequirementBuilder.create(moduleId).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        XIdentityCapability cap = (XIdentityCapability) caps.iterator().next();

        XResource resource = cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, icap.getNamespace());
        assertEquals("moduleA", icap.getName());
        assertEquals(Version.parseVersion("1.0.0"), icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());
        
        // Verify that we have a moduleId attribute 
        assertEquals(moduleId.toString(), icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE));

        // Add the resource to storage and verify again
        resource = storage.addResource(resource);
        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("No capability", 0, caps.size());

        icap = resource.getIdentityCapability();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, icap.getNamespace());
        assertEquals("moduleA", icap.getName());
        assertEquals(Version.parseVersion("1.0.0"), icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());
        
        // Verify that we have a moduleId attribute 
        assertEquals(moduleId.toString(), icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE));
    }

    @Test
    public void testResourceWithInvalidMetaData() throws Exception {
        ModuleIdentifier moduleId = addModuleSpec(moduleLoader, getModuleB());
        XRequirement req = XRequirementBuilder.create(moduleId).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        XIdentityCapability cap = (XIdentityCapability) caps.iterator().next();

        XResource resource = cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, icap.getNamespace());
        assertEquals("moduleB", icap.getName());
        assertEquals(Version.emptyVersion, icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());
        
        // Verify that we have a moduleId attribute 
        assertEquals(moduleId.toString(), icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE));

        // Add the resource to storage and verify again
        resource = storage.addResource(resource);
        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("No capability", 0, caps.size());

        icap = resource.getIdentityCapability();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, icap.getNamespace());
        assertEquals("moduleB", icap.getName());
        assertEquals(Version.emptyVersion, icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());
        
        // Verify that we have a moduleId attribute 
        assertEquals(moduleId.toString(), icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE));
    }

    @Test
    public void testFindProvidersFails() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("foo:bar:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("No capability", 0, caps.size());
    }

    private JavaArchive getModuleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "moduleA");
        archive.addClasses(Foo.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addExportPackages(Foo.class);
               return builder.openStream();
            }
        });
        return archive;
    }


    private JavaArchive getModuleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "moduleB");
        archive.addClasses(Foo.class);
        return archive;
    }
}