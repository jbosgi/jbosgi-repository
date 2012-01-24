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
package org.jboss.osgi.repository.spi;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.RepositoryResourceBuilder;
import org.jboss.osgi.resolver.v2.XCapability;
import org.jboss.osgi.resolver.v2.XRequirement;
import org.jboss.osgi.resolver.v2.spi.AbstractBundleRevision;
import org.osgi.framework.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.jboss.osgi.repository.RepositoryConstants.CONTENT_PATH;
import static org.jboss.osgi.repository.RepositoryConstants.CONTENT_URL;

/**
 * An {@link org.jboss.osgi.repository.RepositoryResourceBuilder} that delegates to shrinkwrap
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class AbstractRepositoryResourceBuilder extends RepositoryResourceBuilder {

    public AbstractRepositoryResourceBuilder(URL baseURL, String contentPath) {
        resource = new URLBasedResource(baseURL, contentPath);
        InputStream content = getResource().getContent();
        try {
            Manifest manifest = new JarInputStream(content).getManifest();
            OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
            load(metaData);
        } catch (Exception ex) {
            URL contentURL = getResource().getContentURL();
            throw new RepositoryResolutionException("Cannot create capability from: " + contentURL, ex);
        } finally {
            safeClose(content);
        }
    }

    @Override
    public XCapability addIdentityCapability(String symbolicName, Version version, String type, Map<String, Object> atts, Map<String, String> dirs) {
        URLBasedResource urlres = (URLBasedResource) resource;
        atts.put(CONTENT_URL, urlres.getContentURL());
        atts.put(CONTENT_PATH, urlres.getContentPath());
        return super.addIdentityCapability(symbolicName, version, type, atts, dirs);
    }

    @Override
    public URLBasedResource getResource() {
        return (URLBasedResource) super.getResource();
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

        private final String contentPath;
        private final URL contentURL;

        URLBasedResource(URL baseURL, String contentPath) {
            this.contentPath = contentPath;
            try {
                String base = baseURL.toExternalForm();
                if (!(base.endsWith("/") || contentPath.startsWith("/")))
                    base += "/";
                this.contentURL = new URL(base + contentPath);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        URL getContentURL() {
            return contentURL;
        }

        String getContentPath() {
            return contentPath;
        }

        @Override
        public InputStream getContent() {
            try {
                if (contentURL.getProtocol().equals("file")) {
                    return new FileInputStream(new File(contentURL.getPath()));
                } else {
                    return contentURL.openStream();
                }
            } catch (IOException e) {
                throw new RepositoryResolutionException(e);
            }
        }
    }
}