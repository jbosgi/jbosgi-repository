package org.jboss.osgi.repository.spi;
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

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * An abstract  {@link XRepository} that does nothing.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public abstract class AbstractRepository implements XRepository {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> reqs) {
        if (reqs == null)
            throw MESSAGES.illegalArgumentNull("reqs");
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement req : reqs) {
            Collection<Capability> providers = findProviders(req);
            result.put(req, providers);
        }
        return result;
    }

    @Override
    public abstract Collection<Capability> findProviders(Requirement req);

    /**
     * Convert the given resource into the target resource ttpe associated with the requirement.
     * @return The target resource or null
     */
    public XResource getTargetResource(Requirement req, XResource resource) {
        XResource result = null;

        // Compute the target type
        OSGiMetaData metadata = getOSGiMetaData(req, resource);
        String targetType = (String) req.getAttributes().get(XRepository.TARGET_TYPE);
        if (targetType == null) {
            targetType = metadata != null ? XResource.TYPE_BUNDLE : XResource.TYPE_MODULE;
        }

        if (XResource.TYPE_BUNDLE.equals(targetType)) {
            XResourceBuilder<XResource> builder = getResourceBuilder(resource);
            builder.loadFrom(metadata);
            result = builder.getResource();
            result.putAttachment(XRepository.OSGI_METADATA, metadata);
        } else if (XResource.TYPE_MODULE.equals(targetType)) {
            Object idspec = req.getAttributes().get(XResource.MODULE_IDENTITY_NAMESPACE);
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(idspec.toString());
            XResourceBuilder<XResource> builder = getResourceBuilder(resource);
            builder.addIdentityCapability(moduleId);
            result = builder.getResource();
        } else if (!XResource.TYPE_BUNDLE.equals(targetType) && !XResource.TYPE_MODULE.equals(targetType)) {
            result = resource;
        }
        if (result != null) {
            result.putAttachment(XRepository.SOURCE_REQUIREMENT, req);
        }
        return result;
    }

    private XResourceBuilder<XResource> getResourceBuilder(XResource resource) {
        XResourceBuilder<XResource> builder;
        List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (!caps.isEmpty()) {
            XCapability ccap = (XCapability) caps.iterator().next();
            URL contentURL = toContentURL((String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
            builder = URLResourceBuilderFactory.create(contentURL, ccap.getAttributes());
        } else {
            builder = XResourceBuilderFactory.create();
        }
        return builder;
    }

    public OSGiMetaData getOSGiMetaData(Requirement req, XResource resource) {
        OSGiMetaData result = null;
        Manifest manifest = getResourceManifest(resource);
        if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
            result = OSGiMetaDataBuilder.load(manifest);
        }
        return result;
    }

    public Manifest getResourceManifest(XResource resource) {
        Manifest manifest = null;
        if (resource instanceof RepositoryContent) {
            InputStream content = ((RepositoryContent) resource).getContent();;
            try {
                manifest = new JarInputStream(content).getManifest();
            } catch (IOException ex) {
                LOGGER.debugf("Cannot access manifest from: %s", resource);
            } finally {
                safeClose(content);
            }
        }
        return manifest;
    }

    private static URL toContentURL(String urlspec) {
        try {
            return new URL(urlspec);
        } catch (MalformedURLException e) {
            throw MESSAGES.invalidContentURL(urlspec);
        }
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