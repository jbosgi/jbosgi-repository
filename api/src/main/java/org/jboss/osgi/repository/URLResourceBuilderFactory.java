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

package org.jboss.osgi.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.resolver.XResourceConstants;
import org.osgi.service.repository.ContentNamespace;


/**
 * Create an URL based resource
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public final class URLResourceBuilderFactory extends XResourceBuilderFactory {

    private final URLResource urlres;

    private URLResourceBuilderFactory(URLResource urlres) {
        this.urlres = urlres;
    }

    public static XResourceBuilder create(URL baseURL, String contentPath, Map<String, Object> atts, boolean loadMetadata) throws MalformedURLException {
        final URLResource urlres = new URLResource(baseURL, contentPath);
        URLResourceBuilderFactory factory = new URLResourceBuilderFactory(urlres);

        XResourceBuilder builder = XResourceBuilderFactory.create(factory);
        XCapability ccap = builder.addGenericCapability(ContentNamespace.CONTENT_NAMESPACE, atts, null);
        ccap.getAttributes().put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, urlres.getContentURL().toExternalForm());
        ccap.getAttributes().put(XResourceConstants.CAPABILITY_PATH_ATTRIBUTE, urlres.getContentPath());

        if (loadMetadata) {
            InputStream content = urlres.getContent();
            try {
                Manifest manifest = new JarInputStream(content).getManifest();
                OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
                builder.loadFrom(metaData);
            } catch (Exception ex) {
                URL contentURL = urlres.getContentURL();
                throw new IllegalStateException("Cannot create capability from: " + contentURL, ex);
            } finally {
                if (content != null) {
                    try {
                        content.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        return builder;
    }

    @Override
    public XResource createResource() {
        return urlres;
    }
}
