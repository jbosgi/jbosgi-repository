
package org.jboss.osgi.repository;
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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.spi.AbstractRepositoryCapability;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.resolver.spi.AbstractResource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

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

    public static XResourceBuilder create(URL contentURL, Map<String, Object> contentAtts, boolean loadMetadata) {
        URLResource urlres = new URLResource(contentURL);
        URLResourceBuilderFactory factory = new URLResourceBuilderFactory(urlres);

        XResourceBuilder builder = XResourceBuilderFactory.create(factory);
        XCapability ccap = builder.addCapability(ContentNamespace.CONTENT_NAMESPACE, contentAtts, null);
        
        Map<String, Object> atts = ccap.getAttributes();
        atts.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, contentURL.toExternalForm());
        if (atts.get(ContentNamespace.CONTENT_NAMESPACE) == null)
            atts.put(ContentNamespace.CONTENT_NAMESPACE, XContentCapability.DEFAULT_DIGEST);
        if (atts.get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE) == null)
            atts.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, XContentCapability.DEFAULT_MIME_TYPE);
        if (atts.get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE) == null)
            atts.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, XContentCapability.DEFAULT_SIZE);
        
        if (loadMetadata) {
            InputStream content = urlres.getContent();
            try {
                Manifest manifest = new JarInputStream(content).getManifest();
                OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
                builder.loadFrom(metaData);
            } catch (Exception ex) {
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
    public XCapability createCapability(XResource resource, String namespace, Map<String, Object> atts, Map<String, String> dirs) {
        return new AbstractRepositoryCapability(resource, namespace, atts, dirs);
    }

    @Override
    public XResource createResource() {
        return urlres;
    }

    static class URLResource extends AbstractResource implements RepositoryContent {

        private final URL contentURL;

        URLResource(URL contentURL) {
            if (contentURL == null)
                throw MESSAGES.illegalArgumentNull("contentURL");
            this.contentURL = contentURL;
        }

        URL getContentURL() {
            return contentURL;
        }

        @Override
        public InputStream getContent() {
            try {
                if (contentURL.getProtocol().equals("file")) {
                    return new FileInputStream(new File(contentURL.getPath()));
                } else {
                    return contentURL.openStream();
                }
            } catch (IOException ex) {
                throw MESSAGES.storageCannotObtainInputStream(ex, this);
            }
        }
    }
}
