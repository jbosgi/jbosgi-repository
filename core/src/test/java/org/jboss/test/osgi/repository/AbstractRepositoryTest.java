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
package org.jboss.test.osgi.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.osgi.framework.spi.VirtualFileResourceLoader;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public abstract class AbstractRepositoryTest {

    protected RepositoryReader getRepositoryReader(String xmlres) throws XMLStreamException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(xmlres);
        return RepositoryXMLReader.create(input);
    }

    protected List<XResource> getResources(RepositoryReader reader) {
        List<XResource> resources = new ArrayList<XResource>();
        XResource resource = reader.nextResource();
        while (resource != null) {
            resources.add(resource);
            resource = reader.nextResource();
        }
        return resources;
    }

    protected void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File aux : file.listFiles())
                deleteRecursive(aux);
        }
        file.delete();
    }

    protected ModuleIdentifier addModuleSpec(ModuleLoaderSupport moduleLoader, JavaArchive archive) throws Exception {
        ModuleIdentifier moduleId = ModuleIdentifier.create(archive.getName());
        ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleId);
        VirtualFileResourceLoader resourceLoader = new VirtualFileResourceLoader(toVirtualFile(archive));
        specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader));
        specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        ModuleSpec moduleSpec = specBuilder.create();
        moduleLoader.addModuleSpec(moduleSpec);
        return moduleId;
    }

    protected VirtualFile toVirtualFile(JavaArchive archive) throws IOException {
        ZipExporter exporter = archive.as(ZipExporter.class);
        return AbstractVFS.toVirtualFile(archive.getName(), exporter.exportAsInputStream());
    }
    
    static class ModuleLoaderSupport extends ModuleLoader {

        private Map<ModuleIdentifier, ModuleSpec> modules = new HashMap<ModuleIdentifier, ModuleSpec>();

        void addModuleSpec(ModuleSpec moduleSpec) {
            modules.put(moduleSpec.getModuleIdentifier(), moduleSpec);
        }

        @Override
        protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
            return modules.get(identifier);
        }

        @Override
        protected void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
            super.setAndRelinkDependencies(module, dependencies);
        }
    }
}