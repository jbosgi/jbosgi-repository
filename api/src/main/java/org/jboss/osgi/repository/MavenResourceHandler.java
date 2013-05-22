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
package org.jboss.osgi.repository;

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.framework.Version;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * Create an URL based resource
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public final class MavenResourceHandler {

    public XResource toBundleResource(XResource resource) {
        return toBundleResource(resource, null, null);
    }

    public XResource toBundleResource(XResource resource, String symbolicName, Version version) {
        if (resource == null)
            throw MESSAGES.illegalArgumentNull("resource");

        XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next();
        String urlspec = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        InputStream content = ((RepositoryContent) resource).getContent();

        XResourceBuilder<?> builder;
        Manifest manifest;
        try {
            builder = URLResourceBuilderFactory.create(new URL(urlspec), ccap.getAttributes());
            manifest = new JarInputStream(content).getManifest();
        } catch (IOException ex) {
            throw MESSAGES.invalidMavenResource(ex, resource);
        } finally {
            safeClose(content);
        }

        // First check if we can get the metadata from the manifest
        if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
            OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
            builder.loadFrom(metaData);
            return builder.getResource();
        }

        // For an invalid manifest, we must have an explicit bsname/version
        if (symbolicName == null)
            throw MESSAGES.illegalArgumentNull("symbolicName");
        if (version == null)
            throw MESSAGES.illegalArgumentNull("version");

        // Scan the jar for directories that contain classes
        Set<String> packageNames = new LinkedHashSet<String>();
        content = ((RepositoryContent) resource).getContent();
        try {
            JarInputStream jar = new JarInputStream(content);
            JarEntry jarEntry = jar.getNextJarEntry();
            while (jarEntry != null) {
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    String packageName = entryName.substring(0, entryName.lastIndexOf('/'));
                    if (packageName.indexOf("internal") < 0) {
                        packageNames.add(packageName);
                    }
                }
                jarEntry = jar.getNextJarEntry();
            }
        } catch (IOException ex) {
            throw MESSAGES.invalidMavenResource(ex, resource);
        } finally {
            safeClose(content);
        }

        OSGiMetaDataBuilder mdbuilder = OSGiMetaDataBuilder.createBuilder(symbolicName, version);
        for (String packageName : packageNames) {
            mdbuilder.addExportPackages(packageName + ";version='" + version + "'");
        }
        OSGiMetaData metaData = mdbuilder.getOSGiMetaData();
        builder.loadFrom(metaData);
        return builder.getResource();
    }

    public XResource toModuleResource(XResource resource, ModuleIdentifier moduleId) throws RepositoryStorageException {
        if (resource == null)
            throw MESSAGES.illegalArgumentNull("resource");
        if (moduleId == null)
            throw MESSAGES.illegalArgumentNull("moduleId");

        XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next();
        String urlspec = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        InputStream content = ((RepositoryContent) resource).getContent();

        XResourceBuilder<?> builder;
        try {
            builder = URLResourceBuilderFactory.create(new URL(urlspec), ccap.getAttributes());
        } catch (IOException ex) {
            throw MESSAGES.invalidMavenResource(ex, resource);
        } finally {
            safeClose(content);
        }
        builder.addIdentityCapability(moduleId);
        return builder.getResource();
    }

    private void safeClose(InputStream content) {
        try {
            if (content != null) {
                content.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
