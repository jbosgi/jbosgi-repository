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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.impl.ExpressionCombinerImpl;
import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.jboss.osgi.repository.spi.AbstractPersistentRepository;
import org.jboss.osgi.repository.spi.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.spi.MavenIdentityRepository;
import org.jboss.osgi.repository.spi.MavenIdentityRepository.ConfigurationPropertyProvider;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;

/**
 * Test the {@link AbstractPersistentRepository}
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 16-Jan-2012
 */
public class PersistentRepositoryTestCase extends AbstractRepositoryTest {

    private XPersistentRepository repository;
    private File storageDir;

    @Before
    public void setUp() throws IOException {
        storageDir = new File("./target/repository");
        deleteRecursive(storageDir);
        RepositoryStorageFactory storageFactory = new RepositoryStorageFactory() {
            public RepositoryStorage create(XRepository repository) {
                return new FileBasedRepositoryStorage(repository, storageDir, Mockito.mock(ConfigurationPropertyProvider.class));
            }
        };

        repository = new AbstractPersistentRepository(storageFactory);
        repository.addRepositoryDelegate(new MavenIdentityRepository());
    }

    @Test
    public void testFindProvidersByMavenId() throws Exception {

        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();
        Collection<Capability> caps = repository.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        XCapability cap = (XCapability) caps.iterator().next();

        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        // Verify that the resource is in storage
        XRequirementBuilder builder = XRequirementBuilder.create(IdentityNamespace.IDENTITY_NAMESPACE, "org.apache.felix.configadmin");
        builder.getAttributes().put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "[1.0,2.0)");
        req = builder.getRequirement();
        caps = storage.findProviders(req);
        assertEquals("One capability", 1, caps.size());
        cap = (XIdentityCapability) caps.iterator().next();

        // Verify the content capability
        XResource resource = cap.getResource();
        caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("One capability", 1, caps.size());

        verifyCapability(cap);
    }

    private void verifyCapability(XCapability cap) throws IOException, MalformedURLException, BundleException {

        XResource resource = cap.getResource();
        XIdentityCapability icap = resource.getIdentityCapability();
        assertEquals("org.apache.felix.configadmin", icap.getName());
        assertEquals(Version.parseVersion("1.2.8"), icap.getVersion());
        assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());

        Collection<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals("One capability", 1, caps.size());
        cap = (XCapability) caps.iterator().next();
        URL url = new URL((String) cap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));

        String absolutePath = storageDir.getAbsolutePath();
        // Convert the absolute path so that it works on Windows too
        absolutePath = absolutePath.replace('\\', '/');
        if (!absolutePath.startsWith("/"))
            absolutePath = "/" + absolutePath;

        Assert.assertTrue("Local path: " + url, url.getPath().startsWith(absolutePath));

        RepositoryContent content = (RepositoryContent) resource;
        Manifest manifest = new JarInputStream(content.getContent()).getManifest();
        OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
        assertEquals("org.apache.felix.configadmin", metaData.getBundleSymbolicName());
        assertEquals(Version.parseVersion("1.2.8"), metaData.getBundleVersion());
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

    @Test
    public void testFindSimpleRequirementExpression() throws Exception {
        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        XRequirement req = XRequirementBuilder.create(mavenid).getRequirement();

        RequirementExpression re = repository.getExpressionCombiner().expression(req);
        Collection<Resource> resources = repository.findProviders(re);
        Assert.assertEquals(1, resources.size());
        XResource res = (XResource) resources.iterator().next();
        XIdentityCapability icap = res.getIdentityCapability();
        assertEquals("org.apache.felix.configadmin", icap.getName());
        assertEquals(Version.parseVersion("1.2.8"), icap.getVersion());
    }

    @Test
    public void testFindOrRequirementExpression() throws Exception {
        XRequirement req1 = XRequirementBuilder.create(MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8")).getRequirement();
        XRequirement req2 = XRequirementBuilder.create(MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.4.0")).getRequirement();
        RequirementExpression re = repository.getExpressionCombiner().or(req1, req2);
        Collection<Resource> resources = repository.findProviders(re);
        Assert.assertEquals(2, resources.size());

        for (Resource res : resources) {
            XResource xres = (XResource) res;

            XIdentityCapability icap = xres.getIdentityCapability();
            assertEquals("org.apache.felix.configadmin", icap.getName());
            assertTrue(Version.parseVersion("1.2.8").equals(icap.getVersion()) ||
                       Version.parseVersion("1.4.0").equals(icap.getVersion()));
        }
    }

    @Test
    public void testFindAndRequirementExpression() throws Exception {
        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        XResourceBuilder<XResource> rbf1 = getXResourceBuilder();
        Map<String, Object> atts1 = new HashMap<String, Object>();
        atts1.put("A", "1");
        atts1.put("B", "2");
        rbf1.addCapability("foo", atts1, null);
        rbf1.addIdentityCapability("foo", Version.parseVersion("1"));
        XResource res1 = rbf1.getResource();
        storage.addResource(res1);

        XResourceBuilder<XResource> rbf2 = getXResourceBuilder();
        Map<String, Object> atts2 = new HashMap<String, Object>();
        atts2.put("A", "1");
        atts2.put("B", "3");
        rbf2.addCapability("foo", atts2, null);
        rbf2.addIdentityCapability("foo", Version.parseVersion("1.1"));
        XResource res2 = rbf2.getResource();
        storage.addResource(res2);

        ExpressionCombiner ec = repository.getExpressionCombiner();
        Requirement req1 = repository.newRequirementBuilder("foo").addDirective("filter", "(A=1)").build();
        Requirement req2 = repository.newRequirementBuilder("foo").addDirective("filter", "(B=3)").build();

        Collection<Resource> providers = repository.findProviders(ec.and(req1, req2));
        assertEquals(1, providers.size());

        Resource res = providers.iterator().next();
        XResource xres = (XResource) res;
        XIdentityCapability icap = xres.getIdentityCapability();
        assertEquals("foo", icap.getName());
        assertEquals(Version.parseVersion("1.1"), icap.getVersion());
    }

    @Test
    public void testFindAndNotRequirementExpression() throws Exception {
        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        XResourceBuilder<XResource> rbf1 = getXResourceBuilder();
        Map<String, Object> atts1 = new HashMap<String, Object>();
        atts1.put("A", "1");
        atts1.put("B", "2");
        rbf1.addCapability("foo", atts1, null);
        rbf1.addIdentityCapability("foo", Version.parseVersion("1"));
        XResource res1 = rbf1.getResource();
        storage.addResource(res1);

        XResourceBuilder<XResource> rbf2 = getXResourceBuilder();
        Map<String, Object> atts2 = new HashMap<String, Object>();
        atts2.put("A", "1");
        atts2.put("B", "3");
        rbf2.addCapability("foo", atts2, null);
        rbf2.addIdentityCapability("foo", Version.parseVersion("1.1"));
        XResource res2 = rbf2.getResource();
        storage.addResource(res2);

        ExpressionCombiner ec = repository.getExpressionCombiner();
        Requirement req1 = repository.newRequirementBuilder("foo").addDirective("filter", "(A=1)").build();
        Requirement req2 = repository.newRequirementBuilder("foo").addDirective("filter", "(B=3)").build();

        Collection<Resource> providers = repository.findProviders(ec.and(ec.expression(req1), ec.not(req2)));
        assertEquals(1, providers.size());

        Resource res = providers.iterator().next();
        XResource xres = (XResource) res;
        XIdentityCapability icap = xres.getIdentityCapability();
        assertEquals("foo", icap.getName());
        assertEquals(Version.parseVersion("1.0"), icap.getVersion());
    }

    @Test
    public void testComplexRequirementExpression() throws Exception {
        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        XResourceBuilder<XResource> rbf1 = getXResourceBuilder();
        Map<String, Object> atts1 = new HashMap<String, Object>();
        atts1.put("test", "yes");
        rbf1.addCapability("x", atts1, null);
        rbf1.addIdentityCapability("bar", Version.parseVersion("1.2.3.beta4"));
        XResource res1 = rbf1.getResource();
        storage.addResource(res1);

        XResourceBuilder<XResource> rbf2 = getXResourceBuilder();
        Map<String, Object> atts2 = new HashMap<String, Object>();
        atts2.put("toast", "lightly");
        atts2.put("jam", "lots");
        rbf2.addCapability("y", atts2, null);
        rbf2.addIdentityCapability("bar", Version.parseVersion("2.0"));
        XResource res2 = rbf2.getResource();
        storage.addResource(res2);

        XResourceBuilder<XResource> rbf3 = getXResourceBuilder();
        Map<String, Object> atts3a = new HashMap<String, Object>();
        atts3a.put("toast", "lightly");
        atts3a.put("jam", "lots");
        rbf3.addCapability("y", atts3a, null);
        Map<String, Object> atts3b = new HashMap<String, Object>();
        atts3b.put("test", "yes");
        rbf3.addCapability("x", atts3b, null);
        rbf3.addIdentityCapability("bar", Version.parseVersion("2.3"));
        XResource res3 = rbf3.getResource();
        storage.addResource(res3);

        XResourceBuilder<XResource> rbf4 = getXResourceBuilder();
        Map<String, Object> atts4 = new HashMap<String, Object>();
        atts4.put("toast", "lightly");
        atts4.put("jam", "no");
        rbf4.addCapability("y", atts4, null);
        rbf4.addIdentityCapability("bar", Version.parseVersion("2.3.1"));
        XResource res4 = rbf4.getResource();
        storage.addResource(res4);

        ExpressionCombiner ec = repository.getExpressionCombiner();
        RequirementExpression reqa = ec.expression(repository.newRequirementBuilder("x").addDirective("filter", "(test=yes)").build());
        Collection<Resource> prova = repository.findProviders(reqa);
        assertEquals(2, prova.size());
        Set<Version> expectedVersions = new HashSet<Version>(
                Arrays.asList(Version.parseVersion("1.2.3.beta4"), Version.parseVersion("2.3")));
        Set<Version> actualVersions = getVersions(prova);
        assertEquals(expectedVersions, actualVersions);

        Requirement reqb = repository.newRequirementBuilder("y").addDirective("filter", "(toast=lightly)").build();
        Requirement reqc = repository.newRequirementBuilder("y").addDirective("filter", "(jam=no)").build();
        RequirementExpression reqd = ec.and(ec.expression(reqb), ec.not(reqc));
        Collection<Resource> provb = repository.findProviders(reqd);
        assertEquals(2, provb.size());
        Set<Version> expectedVersions2 = new HashSet<Version>(
                Arrays.asList(Version.parseVersion("2.0"), Version.parseVersion("2.3.0")));
        Set<Version> actualVersions2 = getVersions(provb);
        assertEquals(expectedVersions2, actualVersions2);

        RequirementExpression reqe = ec.or(reqa, reqd);
        Collection<Resource> provc = repository.findProviders(reqe);
        assertEquals(3, provc.size());
        Set<Version> expectedVersions3 = new HashSet<Version>(
                Arrays.asList(Version.parseVersion("1.2.3.beta4"), Version.parseVersion("2.0"), Version.parseVersion("2.3.0")));
        Set<Version> actualVersions3 = getVersions(provc);
        assertEquals(expectedVersions3, actualVersions3);
    }

    @Test
    public void testComplexRequirementExpression2() throws Exception {
        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        XResourceBuilder<XResource> rbf1 = getXResourceBuilder();
        Map<String, Object> atts1 = new HashMap<String, Object>();
        atts1.put("test", "yes");
        rbf1.addCapability("x", atts1, null);
        rbf1.addIdentityCapability("bar", Version.parseVersion("1.2.3.beta4"));
        XResource res1 = rbf1.getResource();
        storage.addResource(res1);

        XResourceBuilder<XResource> rbf2 = getXResourceBuilder();
        Map<String, Object> atts2 = new HashMap<String, Object>();
        atts2.put("toast", "lightly");
        atts2.put("jam", "lots");
        rbf2.addCapability("y", atts2, null);
        rbf2.addIdentityCapability("bar", Version.parseVersion("2.0"));
        XResource res2 = rbf2.getResource();
        storage.addResource(res2);

        XResourceBuilder<XResource> rbf3 = getXResourceBuilder();
        Map<String, Object> atts3a = new HashMap<String, Object>();
        atts3a.put("toast", "lightly");
        atts3a.put("jam", "lots");
        rbf3.addCapability("y", atts3a, null);
        Map<String, Object> atts3b = new HashMap<String, Object>();
        atts3b.put("test", "yes");
        rbf3.addCapability("x", atts3b, null);
        rbf3.addIdentityCapability("bar", Version.parseVersion("2.3"));
        XResource res3 = rbf3.getResource();
        storage.addResource(res3);

        XResourceBuilder<XResource> rbf4 = getXResourceBuilder();
        Map<String, Object> atts4 = new HashMap<String, Object>();
        atts4.put("toast", "lightly");
        atts4.put("jam", "no");
        rbf4.addCapability("y", atts4, null);
        rbf4.addIdentityCapability("bar", Version.parseVersion("2.3.1"));
        XResource res4 = rbf4.getResource();
        storage.addResource(res4);

        ExpressionCombiner ec = repository.getExpressionCombiner();
        Requirement req0 = repository.newRequirementBuilder("x").addDirective("filter", "(test=*)").build();
        Requirement req1 = repository.newRequirementBuilder("y").addDirective("filter", "(jam=*)").build();
        Requirement req2 = repository.newRequirementBuilder("y").addDirective("filter", "(toast=*)").build();
        RequirementExpression rea = ec.and(req0, req1, req2);
        Collection<Resource> prova = repository.findProviders(rea);
        assertEquals(1, prova.size());
        assertEquals(Collections.singleton(Version.parseVersion("2.3")), getVersions(prova));

        RequirementExpression reb = ec.or(req0, req1, req2);
        Collection<Resource> provb = repository.findProviders(reb);
        assertEquals(4, provb.size());
        Set<Version> expectedVersions = new HashSet<Version>(
                Arrays.asList(
                        Version.parseVersion("1.2.3.beta4"),
                        Version.parseVersion("2.0"),
                        Version.parseVersion("2.3.0"),
                        Version.parseVersion("2.3.1")));
        assertEquals(expectedVersions, getVersions(provb));

        Requirement req3 = repository.newRequirementBuilder("y").addDirective("filter", "(jam=lots)").build();
        RequirementExpression rec = ec.not(req3);
        Collection<Resource> provc = repository.findProviders(rec);
        assertEquals(1, provc.size());
        assertEquals(Collections.singleton(Version.parseVersion("2.3.1")), getVersions(provc));

        Requirement req4 = repository.newRequirementBuilder("osgi.identity").addDirective("filter", "(osgi.identity=foo)").build();
        RequirementExpression red = ec.not(req4);
        Collection<Resource> provd = repository.findProviders(red);
        assertEquals(4, provd.size());
        Set<Version> expectedVersions2 = new HashSet<Version>(
                Arrays.asList(
                        Version.parseVersion("1.2.3.beta4"),
                        Version.parseVersion("2.0"),
                        Version.parseVersion("2.3.0"),
                        Version.parseVersion("2.3.1")));
        assertEquals(expectedVersions2, getVersions(provd));

        Requirement req5 = repository.newRequirementBuilder("y").addDirective("filter", "(jam=no)").build();
        Collection<Resource> prove = repository.findProviders(ec.expression(req5));
        assertEquals(1, prove.size());
        assertEquals(Collections.singleton(Version.parseVersion("2.3.1")), getVersions(prove));

        RequirementExpression ref = ec.not(ec.not(req5));
        Collection<Resource> provf = repository.findProviders(ref);
        assertEquals(1, provf.size());
        assertEquals(Collections.singleton(Version.parseVersion("2.3.1")), getVersions(prove));

        Requirement req6 = repository.newRequirementBuilder("x").addDirective("filter", "(test=yes)").build();
        Requirement req7 = repository.newRequirementBuilder("y").addDirective("filter", "(jam=lots)").build();
        RequirementExpression reg = ec.not(ec.and(req6, req7));
        Collection<Resource> provg = repository.findProviders(reg);
        assertEquals(3, provg.size());
        Set<Version> expectedVersions3 = new HashSet<Version>(
                Arrays.asList(
                        Version.parseVersion("1.2.3.beta4"),
                        Version.parseVersion("2.0"),
                        Version.parseVersion("2.3.1")));
        assertEquals(expectedVersions3, getVersions(provg));

        Requirement req8 = repository.newRequirementBuilder("x").addDirective("filter", "(test=yes)").build();
        Requirement req9 = repository.newRequirementBuilder("y").addDirective("filter", "(jam=lots)").build();
        RequirementExpression reh = ec.not(ec.or(req8, req9));
        Collection<Resource> provh = repository.findProviders(reh);
        assertEquals(1, provh.size());
        assertEquals(Collections.singleton(Version.parseVersion("2.3.1")), getVersions(provh));
    }

    private Set<Version> getVersions(Collection<Resource> resources) {
        Set<Version> versions = new HashSet<Version>();
        for (Resource r : resources) {
            XResource xres = (XResource) r;
            XIdentityCapability icap = xres.getIdentityCapability();
            versions.add(icap.getVersion());
        }
        return versions;
    }

    private XResourceBuilder<XResource> getXResourceBuilder() {
        XResourceBuilder<XResource> rbf = XResourceBuilderFactory.create();

        Map<String, Object> catts = new HashMap<String, Object>();
        catts.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, getURL("/content/sample1.txt"));
        rbf.addCapability(ContentNamespace.CONTENT_NAMESPACE, catts, null);

        return rbf;
    }

    private String getURL(String fname) {
        URL url = getClass().getResource(fname);
        if (url == null)
            return "";

        return url.toExternalForm();
    }
}