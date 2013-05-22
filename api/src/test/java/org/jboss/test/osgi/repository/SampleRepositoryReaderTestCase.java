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

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.osgi.repository.Namespace100;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * Test the the {@link RepositoryXMLReader}.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class SampleRepositoryReaderTestCase extends AbstractRepositoryTest {

    @Test
    public void testSampleRepositoryXML() throws Exception {
        RepositoryReader reader = getRepositoryReader("xml/sample-repository.xml");
        Map<String, String> attributes = reader.getRepositoryAttributes();
        List<XResource> resources = getResources(reader);
        verifyContent(attributes, resources);
    }
    
    static void verifyContent(Map<String, String> attributes, List<XResource> resources) {
        Assert.assertEquals("Two attributes", 2, attributes.size());
        Assert.assertEquals("OSGi Repository", attributes.get(Namespace100.Attribute.NAME.getLocalName()));
        Assert.assertEquals("13582741", attributes.get(Namespace100.Attribute.INCREMENT.getLocalName()));

        Assert.assertEquals("One resource", 1, resources.size());
        XResource resource = resources.get(0);
        Assert.assertNotNull("Resource not null", resource);
        Assert.assertTrue("RepositoryContent", resource instanceof RepositoryContent);

        // osgi.identity
        XIdentityCapability icap = resource.getIdentityCapability();
        Assert.assertEquals("org.acme.pool", icap.getName());
        Assert.assertEquals(Version.parseVersion("1.5.6"), icap.getVersion());
        Assert.assertEquals(IdentityNamespace.TYPE_BUNDLE, icap.getType());

        // osgi.content
        List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        Assert.assertEquals("One capability", 1, caps.size());
        Capability cap = caps.get(0);
        Map<String, Object> atts = cap.getAttributes();
        Map<String, String> dirs = cap.getDirectives();
        Assert.assertEquals("Four attributes", 4, atts.size());
        Assert.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", atts.get(ContentNamespace.CONTENT_NAMESPACE));
        Assert.assertEquals("http://www.acme.com/repository/org/acme/pool/org.acme.pool-1.5.6.jar", atts.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
        Assert.assertEquals(new Long(4405), atts.get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
        Assert.assertEquals("application/vnd.osgi.bundle", atts.get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE));
        Assert.assertEquals("No directives", 0, dirs.size());

        // osgi.wiring.bundle
        caps = resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
        Assert.assertEquals("One capability", 1, caps.size());
        cap = caps.get(0);
        atts = cap.getAttributes();
        dirs = cap.getDirectives();
        Assert.assertEquals("Two attributes", 2, atts.size());
        Assert.assertEquals("org.acme.pool", atts.get(BundleNamespace.BUNDLE_NAMESPACE));
        Assert.assertEquals(Version.parseVersion("1.5.6"), atts.get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
        Assert.assertEquals("No directives", 0, dirs.size());

        // osgi.wiring.package
        caps = resource.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
        Assert.assertEquals("One capability", 1, caps.size());
        cap = caps.get(0);
        atts = cap.getAttributes();
        dirs = cap.getDirectives();
        Assert.assertEquals("Four attributes", 4, atts.size());
        Assert.assertEquals("org.acme.pool", atts.get(PackageNamespace.PACKAGE_NAMESPACE));
        Assert.assertEquals(Version.parseVersion("1.1.2"), atts.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        Assert.assertEquals("org.acme.pool", atts.get(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        Assert.assertEquals(Version.parseVersion("1.5.6"), atts.get(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
        Assert.assertEquals("One directive", 1, dirs.size());
        Assert.assertEquals("org.acme.pool,org.acme.util", dirs.get(PackageNamespace.CAPABILITY_USES_DIRECTIVE));

        // osgi.wiring.package
        List<Requirement> reqs = resource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
        Assert.assertEquals("One requirement", 1, reqs.size());
        Requirement req = reqs.get(0);
        atts = req.getAttributes();
        dirs = req.getDirectives();
        Assert.assertEquals("No attributes", 0, atts.size());
        Assert.assertEquals("One directive", 1, dirs.size());
        Assert.assertEquals("(&(osgi.wiring.package=org.apache.commons.pool)(version>=1.5.6))", dirs.get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));

        // osgi.identity
        reqs = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        Assert.assertEquals("One requirement", 1, reqs.size());
        req = reqs.get(0);
        atts = req.getAttributes();
        dirs = req.getDirectives();
        Assert.assertEquals("No attributes", 0, atts.size());
        Assert.assertEquals("Four directives", 4, dirs.size());
        Assert.assertEquals("meta", dirs.get(IdentityNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE));
        Assert.assertEquals("optional", dirs.get(IdentityNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
        Assert.assertEquals("(&(version=1.5.6)(osgi.identity=org.acme.pool-src))", dirs.get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        Assert.assertEquals("sources", dirs.get(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE));
    }
}