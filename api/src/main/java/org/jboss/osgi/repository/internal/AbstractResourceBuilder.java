/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.repository.internal;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.ResourceBuilder;
import org.jboss.osgi.resolver.v2.XResource;
import org.jboss.osgi.resolver.v2.XResourceBuilder;
import org.jboss.osgi.resolver.v2.spi.AbstractBundleRevision;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * An {@link ResourceBuilder} that delegates to shrinkwrap
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class AbstractResourceBuilder implements ResourceBuilder {

    public static AbstractResourceBuilder INSTANCE = new AbstractResourceBuilder();

    // Hide ctor
    private AbstractResourceBuilder() {
    }

    @Override
    public XResource createResource(URL url) {
        XResource resource = new URLBasedResource(url);
        XResourceBuilder builder = XResourceBuilder.INSTANCE;
        InputStream content = resource.getContent();
        try {
            Manifest manifest = new JarInputStream(content).getManifest();
            OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
            builder.associateResource(resource).load(metaData);
        } catch (Exception ex) {
            throw new RepositoryResolutionException("Cannot create capability from: " + url, ex);
        } finally {
            safeClose(content);
        }
        return resource;
    }

    private void safeClose(InputStream content) {
        if (content != null) {
            try {
                content.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static class URLBasedResource extends AbstractBundleRevision {

        private final URL url;

        URLBasedResource(URL url) {
            this.url = url;
        }

        @Override
        public InputStream getContent() {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new RepositoryResolutionException(e);
            }
        }
    }
}