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
package org.jboss.osgi.repository;

import java.io.IOException;
import java.net.URL;

/**
 * Handles resolution and storage of repository artifacts
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public interface ArtifactHandler {

    /**
     * Resolve the the given artifact coordinates.
     *
     * @param coordinates the artifact coordinates
     * @return An array of URLs that match the given coordinates or an empty array.
     * @throws RepositoryResolutionException on resolution error
     */
    URL[] resolveArtifacts(ArtifactCoordinates coordinates) throws RepositoryResolutionException;

    /**
     * Store the artifacts to the internal cache.
     *
     * @param coordinates the artifact coordinates
     * @return An array of URLs that now point to the internal cache..
     * @throws RepositoryStorageException on artifact storage error
     */
    URL[] storeArtifacts(ArtifactCoordinates coordinates, URL[] urls) throws RepositoryStorageException;
}
