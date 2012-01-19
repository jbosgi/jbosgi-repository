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

import org.jboss.logging.Logger;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.ArtifactCoordinates;
import org.jboss.osgi.repository.ArtifactHandler;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.RequirementBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.v2.XResource;
import org.jboss.osgi.resolver.v2.XResourceBuilder;
import org.jboss.osgi.resolver.v2.spi.AbstractBundleRevision;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.jboss.osgi.repository.RepositoryConstants.MAVEN_IDENTITY_NAMESPACE;

/**
 * An implementation of a Repository that delegates to Mavan.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryImpl implements XRepository {

    private static Logger log = Logger.getLogger(RepositoryImpl.class);

    private final ArtifactHandler artifactHandler;

    public RepositoryImpl(ArtifactHandler artifactHandler) {
        this.artifactHandler =artifactHandler;
    }

    @Override
    public RequirementBuilder getRequirementBuilder() {
        return AbstractRequirementBuilder.INSTANCE;
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        if (req == null)
            throw new IllegalArgumentException("Null req");
        Collection<Capability> result;
        String namespace = req.getNamespace();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace)) {
            result = processMavenIdentity(req);
        } else {
            throw new NotImplementedException("Unsupported requirement namespace: " + namespace);
        }
        return result;
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        throw new NotImplementedException();
    }

    private List<Capability> processMavenIdentity(Requirement req) {
        String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
        ArtifactCoordinates coordinates = ArtifactCoordinates.parse(mavenId);
        URL[] urls = artifactHandler.resolveArtifacts(coordinates);
        List<Capability> result = processResolutionResult(urls);
        artifactHandler.storeArtifacts(coordinates, urls);
        return result;
    }

    private List<Capability> processResolutionResult(URL[] urls) {
        List<Capability> result = new ArrayList<Capability>();
        for (URL url : urls) {
            XResource resource = new URLBasedResource(url);
            XResourceBuilder builder = XResourceBuilder.INSTANCE;
            InputStream content = resource.getContent();
            try {
                Manifest manifest = new JarInputStream(content).getManifest();
                OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
                builder.associateResource(resource).load(metaData);
                result.add(resource.getIdentityCapability());
            } catch (Exception ex) {
                throw new RepositoryResolutionException("Cannot create capability from: " + url, ex);
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