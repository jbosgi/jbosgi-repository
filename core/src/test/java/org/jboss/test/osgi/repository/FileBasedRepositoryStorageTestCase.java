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

import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_MIME_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryContentHelper;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XContentCapability;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XPackageCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.spi.BundleInfo;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.repository.RepositoryContent;

/**
 * Test the default resolver integration.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class FileBasedRepositoryStorageTestCase extends AbstractRepositoryTest {

    private File storageDir;
    private XRepository repository;
    private RepositoryStorage storage;
    private File bundleAjar;
    private File bundleAtxt;

    @Before
    public void setUp() throws Exception {
        storageDir = new File("./target/repository/" + System.currentTimeMillis()).getCanonicalFile();
        repository = Mockito.mock(XRepository.class);
        Mockito.when(repository.getName()).thenReturn("MockedRepo");
        storage = new FileBasedRepositoryStorage(repository, storageDir);
        
        // Write the bundle to the location referenced by repository-testA.xml
        bundleAjar = new File("./target/bundleA.jar");
        getBundleA().as(ZipExporter.class).exportTo(bundleAjar, true);
        
        // Write some text to the location referenced by repository-testB.xml
        bundleAtxt = new File("./target/bundleA.txt");
        PrintWriter bw = new PrintWriter(new FileWriter(bundleAtxt));
        BundleInfo infoA = BundleInfo.createBundleInfo(bundleAjar.toURI().toURL());
        bw.println(infoA.toString());
        bw.close();
    }

    @After
    public void tearDown() {
        deleteRecursive(storageDir);
        bundleAjar.delete();
        bundleAtxt.delete();
    }

    @Test
    public void testAddResourceFromStream() throws Exception {

        Assert.assertNull("Empty repository", storage.getRepositoryReader().nextResource());

        // Add a resource from input stream
        InputStream input = getBundleA().as(ZipExporter.class).exportAsInputStream();
        XResource resource = storage.addResource("application/vnd.osgi.bundle", input);

        verifyResource(resource);
        verifyProviders(storage);

        Assert.assertTrue(storage.removeResource(resource));

        XCapability ccap = (XCapability) resource.getCapabilities(CONTENT_NAMESPACE).get(0);
        URL fileURL = new URL((String) ccap.getAttribute(CAPABILITY_URL_ATTRIBUTE));
        Assert.assertFalse("File removed: " + fileURL, new File(fileURL.getPath()).exists());
    }

    @Test
    public void testAddResourceFromXML() throws Exception {
        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        XResource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);
    }

    @Test
    public void testAddResourceWithMultipleContent() throws Exception {
        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testB.xml");
        storage.addResource(reader.nextResource());

        XRequirement req = XRequirementBuilder.create(IDENTITY_NAMESPACE, "bundleA").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        XIdentityCapability icap = cap.adapt(XIdentityCapability.class);
        Assert.assertEquals("bundleA", icap.getSymbolicName());
        
        XResource resource = (XResource) cap.getResource();
        verifyDefaultContent(resource);
        
        List<Capability> ccaps = resource.getCapabilities(CONTENT_NAMESPACE);
        Assert.assertEquals(2, ccaps.size());
        XContentCapability ccap = ((XCapability) ccaps.get(0)).adapt(XContentCapability.class); 
    }

    @Test
    public void testAddResourceFromOSGiMetadata() throws Exception {
        
        XResourceBuilder builder = XResourceBuilderFactory.create();
        BundleInfo info = BundleInfo.createBundleInfo(bundleAjar.toURI().toURL());
        builder.loadFrom(info.getOSGiMetadata());

        Map<String, Object> atts = new HashMap<String, Object>();
        atts.put(CONTENT_NAMESPACE, XContentCapability.DEFAULT_DIGEST);
        atts.put(CAPABILITY_MIME_ATTRIBUTE, "application/vnd.osgi.bundle");
        atts.put(CAPABILITY_URL_ATTRIBUTE, "file:./target/bundleA.jar");
        builder.addCapability(CONTENT_NAMESPACE, atts, null);

        XResource resource = storage.addResource(builder.getResource());
        verifyResource(resource);
        verifyProviders(storage);
    }

    @Test
    public void testFileStorageRestart() throws Exception {

        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        XResource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);

        RepositoryStorage other = new FileBasedRepositoryStorage(repository, storageDir);
        verifyProviders(other);
    }

    @Test
    public void testBundleInfo() throws Exception {

        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        XResource resource = storage.addResource(reader.nextResource());

        XCapability ccap = (XCapability) resource.getCapabilities(CONTENT_NAMESPACE).get(0);
        URL fileURL = new URL((String) ccap.getAttribute(CAPABILITY_URL_ATTRIBUTE));

        BundleInfo info = BundleInfo.createBundleInfo(fileURL);
        Assert.assertEquals("bundleA", info.getOSGiMetadata().getBundleSymbolicName());
    }

    @Test
    public void testCustomNamespace() throws Exception {

        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        XResource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);

        List<Capability> allcaps = resource.getCapabilities(null);
        Assert.assertEquals("Six capabilities", 6, allcaps.size());

        XRequirement req = XRequirementBuilder.create("custom.namespace", "custom.value").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertEquals("One provider", 1, providers.size());

        req = XRequirementBuilder.create("custom.namespace", "bogus").getRequirement();
        providers = storage.findProviders(req);
        Assert.assertEquals("No provider", 0, providers.size());
    }

    private void verifyResource(XResource resource) throws Exception {
        verifyDefaultContent(resource);
        Assert.assertEquals(6, resource.getCapabilities(null).size());
    }

    private void verifyDefaultContent(XResource resource) throws Exception {
        InputStream input = ((RepositoryContent)resource).getContent();
        String digest = RepositoryContentHelper.getDigest(input);
        Assert.assertNotNull("RepositoryContent not null", input);
        input.close();

        XCapability cap = (XCapability) resource.getCapabilities(CONTENT_NAMESPACE).get(0);
        XContentCapability ccap = cap.adapt(XContentCapability.class);
        Assert.assertEquals(digest, ccap.getDigest());
        Assert.assertEquals(digest, cap.getAttribute(CONTENT_NAMESPACE));
        Assert.assertEquals("application/vnd.osgi.bundle", ccap.getMimeType());
        Assert.assertEquals("application/vnd.osgi.bundle", cap.getAttribute(CAPABILITY_MIME_ATTRIBUTE));
        Assert.assertEquals(new Long(400), ccap.getSize());
        Assert.assertEquals(new Long(400), (Long)cap.getAttribute(CAPABILITY_SIZE_ATTRIBUTE));
        URL fileURL = new URL((String) ccap.getAttribute(CAPABILITY_URL_ATTRIBUTE));
        File contentFile = new File(fileURL.getPath());
        Assert.assertTrue("File exists: " + fileURL, contentFile.exists());
        Assert.assertTrue("Path starts with: " + storageDir.getPath(), fileURL.getPath().startsWith(storageDir.getCanonicalPath()));
    }

    private void verifyProviders(RepositoryStorage storage) throws Exception {
        XRequirement req = XRequirementBuilder.create(PACKAGE_NAMESPACE, "org.acme.foo").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        XCapability cap = (XCapability) providers.iterator().next();
        XPackageCapability pcap = cap.adapt(XPackageCapability.class);
        Assert.assertNotNull(pcap);
        Assert.assertEquals("org.acme.foo", pcap.getPackageName());

        verifyResource((XResource) pcap.getResource());
    }

    private JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundleA");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addExportPackages("org.acme.foo");
                builder.addProvidedCapabilities("custom.namespace;custom.namespace=custom.value");
                return builder.openStream();
            }
        });
        return archive;
    }
}