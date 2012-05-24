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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
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
public class RepositoryBundleTestCase {

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleActivator.class, Repository.class, Resource.class);
                builder.addImportPackages(XResource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testMavenCoordinates() throws Exception {

        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        Requirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Assert.assertNotNull("Requirement not null", req);

        Collection<Capability> providers = getRepository().findProviders(req);
        Assert.assertEquals("Capability not null", 1, providers.size());

        XIdentityCapability xcap = (XIdentityCapability) providers.iterator().next();
        Assert.assertEquals("org.apache.felix.configadmin", xcap.getSymbolicName());
        RepositoryContent content = (RepositoryContent) xcap.getResource();
        InputStream input = content.getContent();
        try {
            Bundle bundle = context.installBundle(xcap.getSymbolicName(), input);
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
        Assert.assertEquals("Capability not null", 1, providers.size());

        XPackageCapability pcap = (XPackageCapability) providers.iterator().next();
        Assert.assertEquals("org.apache.felix.cm", pcap.getPackageName());
        Assert.assertEquals(Version.parseVersion("1.0.0"), pcap.getVersion());
    }

    private XRepository getRepository() {
        ServiceReference sref = context.getServiceReference(XRepository.class.getName());
        return sref != null ? (XRepository) context.getService(sref) : null;
    }
}
