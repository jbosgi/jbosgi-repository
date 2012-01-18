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

import org.jboss.osgi.repository.ArtifactCoordinates;
import org.jboss.osgi.repository.ArtifactHandler;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.ResolutionException;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An {@link ArtifactHandler} that delegates to shrinkwrap
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class ShrinkwrapArtifactHandler implements ArtifactHandler {

    @Override
    public URL[] resolveArtifacts(ArtifactCoordinates coordinates) {
        String mavenId = coordinates.toExternalForm();
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).artifact(mavenId);
        try {
            File[] files = resolver.resolveAsFiles();
            URL[] result = new URL[files.length];
            for(int i=0; i < files.length; i++) {
                try {
                    result[i] = files[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    //ignore
                }
            }
            return result;
        } catch (ResolutionException e) {
            throw new RepositoryResolutionException(e);
        }
    }

    @Override
    public void storeArtifacts(ArtifactCoordinates coordinates, URL[] urls) {
        // do nothing
    }
}