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
package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;
import static org.jboss.osgi.resolver.XResource.MODULE_IDENTITY_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * A simple {@link XRepository} that gets resources from the a {@link ModuleLoader}.
 *
 * @author thomas.diesler@jboss.com
 * @since 25-Jun-2013
 */
public class ModuleIdentityRepository extends AbstractRepository {

    private final ModuleLoader moduleLoader;

    public ModuleIdentityRepository(ModuleLoader moduleLoader) {
        if (moduleLoader == null)
            throw MESSAGES.illegalArgumentNull("moduleLoader");
        this.moduleLoader = moduleLoader;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public Module loadModule(ModuleIdentifier identifier) {
        Module module;
        try {
            module = moduleLoader.loadModule(identifier);
        } catch (ModuleLoadException ex) {
            throw MESSAGES.cannotObtainModuleResource(ex, identifier);
        }
        return module;
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        if (!MODULE_IDENTITY_NAMESPACE.equals(namespace)) {
            return Collections.emptyList();
        }

        String idspec = (String) req.getAttributes().get(MODULE_IDENTITY_NAMESPACE);
        if (idspec == null)
            throw MESSAGES.cannotObtainRequiredAttribute(MODULE_IDENTITY_NAMESPACE);

        // Load the module
        ModuleIdentifier moduleId = ModuleIdentifier.fromString(idspec);
        Module module = loadModule(moduleId);

        // Build the module resource
        XResourceBuilder<XResource> factory = XResourceBuilderFactory.create();
        factory.addIdentityCapability(moduleId);
        XResource resource = factory.getResource();

        // Convert the resource to the given target type
        List<Capability> result = new ArrayList<Capability>();
        try {
            resource = getTargetResource(resource, module);
            result.add(resource.getIdentityCapability());
        } catch (Exception ex) {
            LOGGER.errorCannotCreateResource(ex, idspec);
        }

        return Collections.unmodifiableList(result);
    }

    private XResource getTargetResource(XResource resource, Module module) throws Exception {
        // Add the module identity attribute and remove all requirements
        ModuleIdentifier moduleId = module.getIdentifier();
        XResource auxres = super.getTargetResource(resource);
        XIdentityCapability icap = auxres.getIdentityCapability();
        XResourceBuilder<XResource> builder = XResourceBuilderFactory.create();
        for (Capability cap : auxres.getCapabilities(null)) {
            String namespace = cap.getNamespace();
            Map<String, Object> atts = new LinkedHashMap<String, Object>(cap.getAttributes());
            Map<String, String> dirs = new LinkedHashMap<String, String>(cap.getDirectives());
            if (cap == icap) {
                atts.put(MODULE_IDENTITY_NAMESPACE, moduleId.toString());
            }
            builder.addCapability(namespace, atts, dirs);
        }
        List<Requirement> reqs = auxres.getRequirements(null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Ignoring %d requirements", reqs.size());
            for (Requirement req : reqs) {
                LOGGER.debugf(" %s", req);
            }
        }
        return builder.getResource();
    }

    @Override
    public Manifest getResourceManifest(XResource resource) throws IOException {
        Manifest manifest = null;
        Module module = loadModule(getModuleIdentifier(resource));
        ModuleClassLoader classLoader = module.getClassLoader();
        Enumeration<URL> urls = classLoader.findResources(JarFile.MANIFEST_NAME, false);
        while (urls.hasMoreElements()) {
            URL manifestURL = urls.nextElement();
            if (manifest == null) {
                InputStream input = manifestURL.openStream();
                try {
                    manifest = new Manifest(input);
                } finally {
                    input.close();
                }
            } else {
                // Cannot process multiple manifests
                manifest = null;
                break;
            }
        }
        return manifest;
    }

    @Override
    public OSGiMetaData getOSGiMetaData(XResource resource) throws IOException {
        OSGiMetaData result = getOSGiMetaDataFromManifest(resource);
        if (result == null) {
            ModuleIdentifier moduleId = getModuleIdentifier(resource);
            Module module = loadModule(moduleId);
            result = getOSGiMetaDataFromModule(module);
        }
        return result;
    }

    public OSGiMetaData getOSGiMetaDataFromModule(Module module) {

        // Get symbolic name & version
        ModuleIdentifier moduleId = module.getIdentifier();
        String symbolicName = moduleId.getName();
        Version version;
        try {
            version = Version.parseVersion(moduleId.getSlot());
        } catch (IllegalArgumentException ex) {
            version = Version.emptyVersion;
        }
        OSGiMetaDataBuilder builder = OSGiMetaDataBuilder.createBuilder(symbolicName, version);

        /****
         * Must be in sync with AbstractResourceBuilder#loadFrom
         */
        // Add a package capability for every exported path
        for(String path : module.getExportedPaths()) {
            if (path.length() > 0) {
                String packageName = path.replace('/', '.');
                builder.addExportPackages(packageName);
            }
        }

        return builder.getOSGiMetaData();
    }

    public ModuleIdentifier getModuleIdentifier(XResource resource) {
        XIdentityCapability icap = resource.getIdentityCapability();
        String idspec = (String) icap.getAttribute(XResource.MODULE_IDENTITY_NAMESPACE);
        return ModuleIdentifier.fromString(idspec);
    }
}