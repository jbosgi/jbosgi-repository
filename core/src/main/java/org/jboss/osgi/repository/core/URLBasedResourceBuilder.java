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
package org.jboss.osgi.repository.core;

import static org.jboss.osgi.resolver.XResourceConstants.CONTENT_PATH;
import static org.jboss.osgi.resolver.XResourceConstants.CONTENT_URL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.URLBasedResource;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.resolver.spi.AbstractResource;
import org.jboss.osgi.resolver.spi.AbstractResourceBuilder;
import org.osgi.framework.Version;


/**
 * Create an URL based resource
 * 
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
final class URLBasedResourceBuilder {

    static XResource createResource(URL baseURL, String contentPath) {
        final URLBasedResource urlres = new URLBasedResource(baseURL, contentPath);
        XResourceBuilderFactory factory = new XResourceBuilderFactory() {
            
            @Override
            public AbstractResourceBuilder createResourceBuilder() {
                AbstractResourceBuilder builder = new AbstractResourceBuilder(this) {
                    @Override
                    public XIdentityCapability addIdentityCapability(String symbolicName, Version version, String type, Map<String, Object> atts, Map<String, String> dirs) {
                        atts.put(CONTENT_URL, urlres.getContentURL());
                        atts.put(CONTENT_PATH, urlres.getContentPath());
                        return super.addIdentityCapability(symbolicName, version, type, atts, dirs);
                    }
                };
                return builder;
            }
            
            @Override
            public AbstractResource createResource() {
                return urlres;
            }
        };
        
        XResourceBuilder builder = XResourceBuilderFactory.create(factory);
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
        XResource resource = builder.getResource();
        return resource;
    }
}
