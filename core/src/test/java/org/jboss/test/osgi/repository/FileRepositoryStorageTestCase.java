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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import junit.framework.Assert;

import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XPackageCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.repository.ContentNamespace;

/**
 * Test the default resolver integration.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class FileRepositoryStorageTestCase extends AbstractRepositoryTest {

    private File storageDir;
    private RepositoryStorage storage;

    @Before
    public void setUp() throws IOException {
        storageDir = new File("./target/repository");
        deleteRecursive(storageDir);
        XRepository repo = Mockito.mock(XRepository.class);
        Mockito.when(repo.getName()).thenReturn("MockedRepo");
        storage = new FileBasedRepositoryStorage(repo, storageDir);
    }

    @Test
    public void testAddResourceFromStream() throws Exception {

        // Assert empty repository
        Assert.assertNull(storage.getRepositoryReader().nextResource());

        // Add a bundle resource
        InputStream input = getBundleA().as(ZipExporter.class).exportAsInputStream();
        XResource resource = storage.addResource("application/vnd.osgi.bundle", input);
        XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
        URL fileURL = new URL((String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));

        verifyResource(resource);

        Assert.assertTrue(storage.removeResource(resource));
        Assert.assertFalse("File removed: " + fileURL, new File(fileURL.getPath()).exists());
    }

    @Test
    public void testAddResource() throws Exception {

        // Assert empty repository
        Assert.assertNull(storage.getRepositoryReader().nextResource());

        // Write the bundle to the location referenced by repository-testA.xml
        getBundleA().as(ZipExporter.class).exportTo(new File("./target/bundleA.jar"), true);

        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        XResource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
    }

    private void verifyResource(XResource resource) throws MalformedURLException {
        XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
        Assert.assertEquals("application/vnd.osgi.bundle", ccap.getAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE));
        Assert.assertNotNull(ccap.getAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
        Assert.assertNotNull(ccap.getAttribute(ContentNamespace.CONTENT_NAMESPACE));
        URL fileURL = new URL((String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
        Assert.assertTrue("File exists: " + fileURL, new File(fileURL.getPath()).exists());

        XRequirement req = XRequirementBuilder.create(PackageNamespace.PACKAGE_NAMESPACE, "org.acme.foo").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        XPackageCapability cap = (XPackageCapability) providers.iterator().next();
        Assert.assertNotNull(cap);
        Assert.assertEquals("org.acme.foo", cap.getPackageName());
        Assert.assertSame(resource, cap.getResource());
    }

    private JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundleA");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addExportPackages("org.acme.foo");
                return builder.openStream();
            }
        });
        return archive;
    }
}