/*
 * #%L
 * JBossOSGi Repository: Integration Tests
 * %%
 * Copyright (C) 2011 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.test.osgi.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.repository.tb1.pkg1.TestInterface;
import org.jboss.test.osgi.repository.tb1.pkg2.TestInterface2;
import org.jboss.test.osgi.repository.tb2.TestClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

/**
 * Tests based on the OSGi Repository TCK
 *
 * @author David Bosschaert
 */
@RunWith(Arquillian.class)
public class RepositoryTCKBasedTestCase extends RepositoryBundleTest {

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createTestDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "testcase-deployment");
        archive.addClasses(RepositoryBundleTest.class);
        URL xmlURL = RepositoryTCKBasedTestCase.class.getResource("/xml/test-repository1.xml");
        archive.addAsResource(xmlURL, "/xml/test-repository1.xml");
        archive.addAsResource(new Asset() {
            @Override
            public InputStream openStream() {
                return createTestBundle1().as(ZipExporter.class).exportAsInputStream();
            }
        }, "tb1.jar");
        archive.addAsResource(new Asset() {
            @Override
            public InputStream openStream() {
                return createTestBundle2().as(ZipExporter.class).exportAsInputStream();
            }
        }, "tb2.jar");
        archive.setManifest(new Asset () {
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

    public static JavaArchive createTestBundle1() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "tb1");
        archive.addClasses(TestInterface.class, TestInterface2.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName("org.jboss.test.cases.repository.tb1");
                builder.addBundleVersion("1.0.0.test");
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(TestInterface.class, TestInterface2.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    public static JavaArchive createTestBundle2() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "tb2");
        archive.addClass(TestClass.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName("org.jboss.test.cases.repository.tb2");
                builder.addBundleVersion("1.0");
                builder.addBundleManifestVersion(2);
                String exp = TestClass.class.getPackage().getName() + ";version=\"1.2.3.qualified\"";
                builder.addExportPackages(exp);
                String imp = TestInterface.class.getPackage().getName() + ";version=\"[0.9, 1)\"";
                builder.addImportPackages(imp);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Override
    BundleContext getBundleContext() {
        return context;
    }

    @Override
    protected void initializeRepository(XPersistentRepository repo) throws Exception {
        super.initializeRepository(repo);

        RepositoryStorage rs = repo.getRepositoryStorage();

        URL xmlURL = getClass().getResource("/xml/test-repository1.xml");
        String xml = new String(readFully(xmlURL.openStream()));
        xml = fillInTemplate(xml, "tb1");
        xml = fillInTemplate(xml, "tb2");

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        RepositoryReader reader = RepositoryXMLReader.create(bais);

        XResource resource = reader.nextResource();
        while (resource != null) {
            rs.addResource(resource);
            resource = reader.nextResource();
        }
    }

    @Test
    public void testQueryByBundleID() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle",
                "(&(osgi.wiring.bundle=org.jboss.test.cases.repository.tb1)(bundle-version=1.0.0.test))");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertEquals(1, result.size());
        assertEquals(requirement, result.keySet().iterator().next());

        assertEquals(1, result.values().size());
        Collection<Capability> matchingCapabilities = result.values().iterator().next();
        assertEquals(1, matchingCapabilities.size());
        Capability capability = matchingCapabilities.iterator().next();

        assertEquals(requirement.getNamespace(), capability.getNamespace());
        assertEquals("org.jboss.test.cases.repository.tb1", capability.getAttributes().get("osgi.wiring.bundle"));
        assertEquals(Version.parseVersion("1.0.0.test"), capability.getAttributes().get("bundle-version"));
    }

    @Test
    public void testQueryNoMatch() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle",
                "(&(osgi.wiring.bundle=org.jboss.test.cases.repository.tb1)(foo=bar))");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertEquals(1, result.size());
        assertEquals(requirement, result.keySet().iterator().next());

        Collection<Capability> matchingCapabilities = result.get(requirement);
        assertEquals(0, matchingCapabilities.size());
    }

    @Test
    public void testQueryNoFilter() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(requirement);
        assertEquals(2, matches.size());

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("osgi.wiring.bundle").equals("org.jboss.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("osgi.wiring.bundle").equals("org.jboss.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertTrue(foundtb1);
        assertTrue(foundtb2);
    }

    @Test
    public void testQueryOnNonMainAttribute() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.identity",
                "(license=http://www.opensource.org/licenses/Apache-2.0)");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(requirement);
        assertEquals(1, matches.size());
        Capability capability = matches.iterator().next();
        assertEquals("osgi.identity", capability.getNamespace());
        assertEquals("org.jboss.test.cases.repository.tb2", capability.getAttributes().get("osgi.identity"));
    }

    @Test
    public void testDisconnectedQueries() throws Exception {
        Requirement req1 = new RequirementImpl("osgi.wiring.bundle",
                "(osgi.wiring.bundle=org.jboss.test.cases.repository.tb1)");
        Requirement req2 = new RequirementImpl("osgi.wiring.bundle",
                "(osgi.wiring.bundle=org.jboss.test.cases.repository.tb2)");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req1, req2);
        assertEquals(2, result.size());

        Collection<Capability> match1 = result.get(req1);
        assertEquals(1, match1.size());
        Capability cap1 = match1.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb1", cap1.getAttributes().get("osgi.wiring.bundle"));

        Collection<Capability> match2 = result.get(req2);
        assertEquals(1, match2.size());
        Capability cap2 = match2.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb2", cap2.getAttributes().get("osgi.wiring.bundle"));
    }

    @Test
    public void testComplexQuery() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(|(osgi.wiring.package=org.jboss.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.jboss.test.cases.repository.tb2))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);

        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(2, matches.size());

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("bundle-symbolic-name").equals("org.jboss.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("bundle-symbolic-name").equals("org.jboss.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertTrue(foundtb1);
        assertTrue(foundtb2);
    }

    @Test
    public void testComplexQueryWithCustomAttributeSpecificValue() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(&(|(osgi.wiring.package=org.jboss.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.jboss.test.cases.repository.tb2))(approved=yes))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(1, matches.size());
        Capability capability = matches.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb2", capability.getAttributes().get("bundle-symbolic-name"));
    }

    @Test
    public void testComplexQueryWithCustomAttributeDefined() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(&(|(osgi.wiring.package=org.jboss.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.jboss.test.cases.repository.tb2))(approved=*))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);

        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(2, matches.size());

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("bundle-symbolic-name").equals("org.jboss.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("bundle-symbolic-name").equals("org.jboss.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertTrue(foundtb1);
        assertTrue(foundtb2);
    }

    @Test
    public void testQueryCustomNamespace() throws Exception {
        Requirement req = new RequirementImpl("osgi.foo.bar", "(myattr=myotherval)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(1, matches.size());
        Capability capability = matches.iterator().next();
        assertEquals("myotherval", capability.getAttributes().get("myattr"));
    }

    @Test
    public void testRepositoryContent() throws Exception {
        Requirement req = new RequirementImpl("osgi.identity", "(osgi.identity=org.jboss.test.cases.repository.tb2)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(1, matches.size());
        Capability capability = matches.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb2", capability.getAttributes().get("osgi.identity"));

        Resource resource = capability.getResource();

        // test getCapabilities();
        List<Capability> identityCaps = resource.getCapabilities("osgi.identity");
        assertEquals(1, identityCaps.size());
        Capability identityCap = identityCaps.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb2", identityCap.getAttributes().get("osgi.identity"));
        assertEquals(Version.parseVersion("1"), identityCap.getAttributes().get("version"));
        assertEquals("osgi.bundle", identityCap.getAttributes().get("type"));
        assertEquals("http://www.opensource.org/licenses/Apache-2.0", identityCap.getAttributes().get("license"));

        List<Capability> contentCaps = resource.getCapabilities("osgi.content");
        assertEquals(1, contentCaps.size());
        Capability contentCap = contentCaps.iterator().next();
        // content and SHA is checked below

        assertEquals(1, resource.getCapabilities("osgi.wiring.bundle").size());

        List<Capability> wiringCaps = resource.getCapabilities("osgi.wiring.package");
        assertEquals(1, wiringCaps.size());
        Capability wiringCap = wiringCaps.iterator().next();
        assertEquals("org.jboss.test.cases.repository.tb2", wiringCap.getAttributes().get("osgi.wiring.package"));
        assertEquals(Version.parseVersion("1.2.3.qualified"), wiringCap.getAttributes().get("version"));
        assertEquals(Version.parseVersion("1"), wiringCap.getAttributes().get("bundle-version"));
        assertEquals("org.jboss.test.cases.repository.tb2", wiringCap.getAttributes().get("bundle-symbolic-name"));
        assertEquals("yes", wiringCap.getAttributes().get("approved"));
        assertEquals("org.jboss.test.cases.repository.tb1.pkg1", wiringCap.getDirectives().get("uses"));

        // Read the requirements
        assertEquals(0, resource.getRequirements("org.osgi.nonexistent").size());
        List<Requirement> wiringReqs = resource.getRequirements("osgi.wiring.package");
        assertEquals(1, wiringReqs.size());
        Requirement wiringReq = wiringReqs.iterator().next();
        assertEquals(2, wiringReq.getDirectives().size());
        assertEquals("custom directive", wiringReq.getDirectives().get("custom"));
        assertEquals("(&(osgi.wiring.package=org.jboss.test.cases.repository.tb1.pkg1)(version>=1.1)(!(version>=2)))",
                wiringReq.getDirectives().get("filter"));
        assertEquals(1, wiringReq.getAttributes().size());
        assertEquals(new Long(42), wiringReq.getAttributes().get("custom"));

        assertEquals("Only the wiring requirements exist", wiringReqs, resource.getRequirements(null));

        // Check content and SHA
        RepositoryContent repositoryContent = (RepositoryContent) resource;
        byte[] contentBytes = readFully(repositoryContent.getContent());
        assertTrue(contentBytes.length > 0);
        assertEquals(new Long(contentBytes.length), contentCap.getAttributes().get("size"));
        assertEquals(getSHA256(contentBytes), contentCap.getAttributes().get("osgi.content"));
        // The previous line fails sometimes (depending on the value of the SHA-256).
        // if the SHA contains bytes with a value < 16 the leading 0 is not added to the output
        // for example:
        // expected:<04fe1203ebeff5c59c3795d52bff4a10a31a0bdbc4c6ec0334f78104bb1f2485>
        // but was: <4fe123ebeff5c59c3795d52bff4a10a31abdbc4c6ec334f7814bb1f2485>
    }

    @Test
    public void testAttributeDataTypes() throws Exception {
        Requirement req = new RequirementImpl("osgi.test.namespace", "(osgi.test.namespace=a testing namespace)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertEquals(1, result.size());
        Collection<Capability> matches = result.get(req);
        assertEquals(1, matches.size());
        Capability cap = matches.iterator().next();

        assertEquals(req.getNamespace(), cap.getNamespace());
        assertEquals("a testing namespace", cap.getAttributes().get("osgi.test.namespace"));
        assertEquals("", cap.getAttributes().get("testString"));
        assertEquals(Version.parseVersion("1.2.3.qualifier"), cap.getAttributes().get("testVersion"));
        assertEquals(new Long(Long.MAX_VALUE), cap.getAttributes().get("testLong"));
        assertEquals(new Double(Math.PI), cap.getAttributes().get("testDouble"));
        assertEquals(Arrays.asList("a", "b and c", "d"), cap.getAttributes().get("testStringList"));
        assertEquals(Arrays.asList(Version.parseVersion("1.2.3"), Version.parseVersion("4.5.6")), cap.getAttributes().get("testVersionList"));
        assertEquals(Collections.singletonList(-1L), cap.getAttributes().get("testLongList"));
        assertEquals(Arrays.asList(Math.E, Math.E), cap.getAttributes().get("testDoubleList"));
    }

    private Map<Requirement, Collection<Capability>> findProvidersAllRepos(Requirement ... requirements) throws InterruptedException {
        return getRepository().findProviders(Arrays.asList(requirements));
    }

    private String fillInTemplate(String xml, String bundleName) throws IOException, NoSuchAlgorithmException {
        URL url = getClass().getResource("/" + bundleName + ".jar");
        byte[] bytes = readFully(url.openStream());

        xml = xml.replaceAll("@@" + bundleName + "SHA256@@", getSHA256(bytes));
        xml = xml.replaceAll("@@" + bundleName + "URL@@", url.toExternalForm());
        xml = xml.replaceAll("@@" + bundleName + "Size@@", "" + bytes.length);
        return xml;
    }

    private static String getSHA256(byte[] bytes) throws NoSuchAlgorithmException {
        StringBuilder builder = new StringBuilder();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (byte b : md.digest(bytes)) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static void readFully(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[4096];

        int length = 0;
        int offset = 0;

        while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += length;

            if (offset == bytes.length) {
                os.write(bytes, 0, bytes.length);
                offset = 0;
            }
        }
        if (offset != 0) {
            os.write(bytes, 0, offset);
        }
    }

    public static byte [] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            readFully(is, baos);
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    private static class RequirementImpl implements Requirement {
        private final String namespace;
        private final Map<String, String> directives = new HashMap<String, String>();

        public RequirementImpl(String ns) {
            namespace = ns;
        }

        public RequirementImpl(String ns, String filter) {
            namespace = ns;
            directives.put("filter", filter);
        }

        public String getNamespace() {
            return namespace;
        }

        public Map<String, String> getDirectives() {
            return directives;
        }

        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        public Resource getResource() {
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((directives == null) ? 0 : directives.hashCode());
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RequirementImpl other = (RequirementImpl) obj;
            if (directives == null) {
                if (other.directives != null)
                    return false;
            } else if (!directives.equals(other.directives))
                return false;
            if (namespace == null) {
                if (other.namespace != null)
                    return false;
            } else if (!namespace.equals(other.namespace))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Requirement[" + namespace+ ",dirs=" + directives + "]";
        }
    }
}
