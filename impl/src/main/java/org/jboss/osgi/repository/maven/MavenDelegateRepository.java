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
package org.jboss.osgi.repository.maven;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RequirementBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.internal.AbstractRequirementBuilder;
import org.jboss.osgi.repository.internal.NotImplementedException;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.spi.AbstractBundleRevision;
import org.jboss.osgi.resolver.spi.ResourceIndexComparator;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.jboss.osgi.repository.RepositoryConstants.MAVEN_IDENTITY_NAMESPACE;

/**
 * An implementation of a Repository that delegates to Mavan.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class MavenDelegateRepository implements XRepository {

    @Override
    public RequirementBuilder getRequirementBuilder() {
        return AbstractRequirementBuilder.INSTANCE;
    }

    @Override
    public SortedSet<Capability> findProviders(Requirement req) {
        if (req == null)
            throw new IllegalArgumentException("Null req");

        SortedSet<Capability> result;
        String namespace = req.getNamespace();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace)) {
            result = processMavenIdentity(req);
        } else {
            throw new NotImplementedException("Unsupported requirement namespace: " + namespace);
        }
        return result;
    }

    @Override
    public Map<Requirement, SortedSet<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        throw new NotImplementedException();
    }

    private SortedSet<Capability> processMavenIdentity(Requirement req) {
        String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).artifact(mavenId);
        File[] files = resolver.resolveAsFiles();
        return processResolutionResult(files);
    }

    private SortedSet<Capability> processResolutionResult(File[] files) {
        final List<Resource> resources = new ArrayList<Resource>();
        SortedSet<Capability> result = new TreeSet<Capability>(new ResourceIndexComparator() {
            @Override
            protected long getResourceIndex(Resource res) {
                return resources.indexOf(res);
            }
        });
        for (File file : files) {
            XResource resource = new FileBasedResource(file);
            resources.add(resource);
            XResourceBuilder builder = XResourceBuilder.INSTANCE;
            InputStream content = resource.getContent();
            try {
                Manifest manifest = new JarInputStream(content).getManifest();
                OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
                builder.associateResource(resource).load(metaData);
                result.add(resource.getIdentityCapability());
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create capabilities", ex);
            } finally {
                safeClose(content);
            }
        }
        return result;
    }

    private void safeClose(InputStream content) {
        if (content != null)                    {
            try {
                content.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static class FileBasedResource extends AbstractBundleRevision {

        private final File file;
        
        FileBasedResource(File file) {
            this.file = file;
        }
        
        @Override
        public InputStream getContent() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("File not found: " + file);
            }
        }
    }
}