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
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

/**
 * Test simple OSGi bundle deployment
 *
 * @author thomas.diesler@jboss.com
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
                builder.addImportPackages(Repository.class, XCapability.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testRepositoryService() throws Exception {

        // Get the service reference
        ServiceReference sref = context.getServiceReference(Repository.class.getName());
        Repository repo = (Repository) context.getService(sref);
        assertNotNull("Repository not null", repo);

        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.felix:org.apache.felix.configadmin:1.2.8");
        Requirement req = XRequirementBuilder.createArtifactRequirement(mavenid);
        assertNotNull("Requirement not null", req);

        Collection<Capability> caps = repo.findProviders(Collections.singleton(req)).get(req);
        assertEquals("Capability not null", 1, caps.size());

        XIdentityCapability xcap = (XIdentityCapability) caps.iterator().next();
        assertEquals("org.apache.felix.configadmin", xcap.getSymbolicName());
        RepositoryContent content = (RepositoryContent) xcap.getResource();
        InputStream input = content.getContent();
        try {
            Bundle bundle = context.installBundle(xcap.getSymbolicName(), input);
            try {
                bundle.start();
                assertEquals(Bundle.ACTIVE, bundle.getState());
            } finally {
                bundle.uninstall();
            }
        } finally {
            input.close();
        }
    }
}
