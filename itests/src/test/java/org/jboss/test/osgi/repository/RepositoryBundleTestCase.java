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

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XPackageCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

/**
 * Test simple OSGi repository access
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Jan-2012
 */
@RunWith(Arquillian.class)
public class RepositoryBundleTestCase extends RepositoryBundleTest {

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.addClasses(RepositoryBundleTest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Repository.class, Resource.class);
                builder.addImportPackages(XRepository.class, XResource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Override
    BundleContext getBundleContext() {
        return context;
    }

    @Test
    public void testMavenCoordinates() throws Exception {

        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Assert.assertNotNull("Requirement not null", req);

        Collection<Capability> providers = getRepository().findProviders(req);
        Assert.assertEquals("One capability", 1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        Assert.assertTrue("Capability matches", req.matches(cap));

        XRequirementBuilder builder = XRequirementBuilder.create(PackageNamespace.PACKAGE_NAMESPACE, "org.apache.felix.cm");
        builder.getAttributes().put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "[1.0,2.0)");
        req = builder.getRequirement();

        providers = getRepository().findProviders(req);
        Assert.assertEquals("One capability", 1, providers.size());

        cap = (XCapability) providers.iterator().next();
        XPackageCapability pcap = cap.adapt(XPackageCapability.class);
        Assert.assertEquals("org.apache.felix.cm", pcap.getPackageName());
        Assert.assertEquals(Version.parseVersion("1.0.0"), pcap.getVersion());

        XResource resource = (XResource) cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        Assert.assertEquals("org.apache.felix.configadmin", icap.getSymbolicName());
        RepositoryContent content = (RepositoryContent) icap.getResource();
        InputStream input = content.getContent();
        try {
            Bundle bundle = context.installBundle(icap.getSymbolicName(), input);
            try {
                bundle.start();
                Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
            } finally {
                bundle.uninstall();
            }
        } finally {
            input.close();
        }
    }

    @Test
    public void testRepositoryReader() throws Exception {

        RepositoryStorage storage = ((XPersistentRepository)getRepository()).getRepositoryStorage();
        RepositoryReader reader = storage.getRepositoryReader();
        Map<String, String> attributes = reader.getRepositoryAttributes();
        Assert.assertNotNull("Increment not null", attributes.get("increment"));
        Assert.assertNotNull("Name not null", attributes.get("name"));

        XResource resource = reader.nextResource();
        Assert.assertNotNull("Resource not null", resource);
        Assert.assertNull("One resource only", reader.nextResource());
    }


    @Test
    public void testRepositoryRestart() throws Exception {

        Bundle bundle = ((BundleReference)getRepository().getClass().getClassLoader()).getBundle();
        Assert.assertEquals("jbosgi-repository", bundle.getSymbolicName());

        bundle.stop();
        Assert.assertNull(getRepository());
        bundle.start();
        Assert.assertNotNull(getRepository());

        XRequirementBuilder builder = XRequirementBuilder.create(PackageNamespace.PACKAGE_NAMESPACE, "org.apache.felix.cm");
        builder.getAttributes().put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "[1.0,2.0)");
        XRequirement req = builder.getRequirement();

        Collection<Capability> providers = getRepository().findProviders(req);
        Assert.assertEquals("One capability", 1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        XPackageCapability pcap = cap.adapt(XPackageCapability.class);
        Assert.assertEquals("org.apache.felix.cm", pcap.getPackageName());
        Assert.assertEquals(Version.parseVersion("1.0.0"), pcap.getVersion());
    }
}
