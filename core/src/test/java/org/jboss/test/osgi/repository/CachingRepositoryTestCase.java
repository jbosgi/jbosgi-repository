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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.core.MavenArtifactRepository;
import org.jboss.osgi.repository.spi.DefaultStorageRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * Test the {@link DefaultStorageRepository}
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class CachingRepositoryTestCase extends AbstractRepositoryTest {

    private XRepository repository;
    private File cacheFile;

    @Before
    public void setUp() throws IOException {
        cacheFile = new File("./target/repository").getCanonicalFile();
        FileBasedRepositoryStorage storage = new FileBasedRepositoryStorage(cacheFile);
        MavenArtifactRepository delegate = new MavenArtifactRepository();
        repository = new DefaultStorageRepository(storage, delegate) {};
        deleteRecursive(cacheFile);
    }

    @Test
    public void testFindProvidersByMavenId() throws Exception {

        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        Requirement req = XRequirementBuilder.createArtifactRequirement(mavenid);
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        XCapability cap = (XCapability) caps.iterator().next();

        XIdentityCapability icap = (XIdentityCapability) cap;
        assertEquals("org.apache.felix.configadmin", icap.getSymbolicName());
        assertEquals(Version.parseVersion("1.2.8"), icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());

        XResource resource = (XResource) icap.getResource();
        RepositoryContent content = (RepositoryContent) resource;
        Manifest manifest = new JarInputStream(content.getContent()).getManifest();
        OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
        assertEquals("org.apache.felix.configadmin", metaData.getBundleSymbolicName());
        assertEquals(Version.parseVersion("1.2.8"), metaData.getBundleVersion());

        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("One capability", 1, caps.size());
        cap = (XCapability) caps.iterator().next();
        URL url = (URL) cap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        Assert.assertTrue("Local path: " + url, url.getPath().startsWith(cacheFile.getAbsolutePath()));
    }
}