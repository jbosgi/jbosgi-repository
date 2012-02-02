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

import org.jboss.osgi.repository.ArtifactProviderPlugin;
import org.jboss.osgi.repository.RepositoryCachePlugin;
import org.jboss.osgi.repository.core.FileBasedRepositoryCachePlugin;
import org.jboss.osgi.repository.core.MavenArtifactProvider;
import org.jboss.osgi.repository.core.RepositoryImpl;
import org.jboss.osgi.repository.core.TrackingArtifactProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.repository.Repository;

/**
 * An activator for the {@link Repository} service.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryActivator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        // Register the MavenArtifactProvider
        MavenArtifactProvider simpleProvider = new MavenArtifactProvider();
        context.registerService(ArtifactProviderPlugin.class.getName(), simpleProvider, null);
        // Register the Repository
        ArtifactProviderPlugin provider = new TrackingArtifactProvider(context);
        RepositoryCachePlugin cache = new FileBasedRepositoryCachePlugin(context.getDataFile("repository"));
        RepositoryImpl service = new RepositoryImpl(provider, cache);
        context.registerService(Repository.class.getName(), service, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}