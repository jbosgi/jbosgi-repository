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

import org.jboss.osgi.repository.internal.AbstractResourceBuilder;
import org.jboss.osgi.repository.ArtifactCoordinates;
import org.jboss.osgi.repository.ArtifactProviderPlugin;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.resolver.v2.XResource;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import static org.jboss.osgi.repository.RepositoryConstants.MAVEN_IDENTITY_NAMESPACE;

/**
 * An {@link ArtifactProviderPlugin} that delegates to shrinkwrap
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class ShrinkwrapProviderPlugin implements ArtifactProviderPlugin {

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace)) {
            String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
            ArtifactCoordinates coordinates = ArtifactCoordinates.parse(mavenId);
            MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class);
            resolver = resolver.artifact(coordinates.toExternalForm());
            try {
                File[] files = resolver.resolveAsFiles();
                URL[] urls = new URL[files.length];
                for (int i = 0; i < files.length; i++) {
                    try {
                        urls[i] = files[i].toURI().toURL();
                    } catch (MalformedURLException e) {
                        //ignore
                    }
                }
                AbstractResourceBuilder builder = AbstractResourceBuilder.INSTANCE;
                Collection<Capability> result = new ArrayList<Capability>();
                for (URL url : urls) {
                    XResource xres = builder.createResource(url);
                    result.add(xres.getIdentityCapability());
                }
                return result;
            } catch (Exception ex) {
                throw new RepositoryResolutionException(ex);
            }
        } else {
            throw new RepositoryResolutionException("Unsupported namespace: " + namespace);
        }
    }
}