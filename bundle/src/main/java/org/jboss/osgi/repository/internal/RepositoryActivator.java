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
import org.jboss.osgi.repository.XRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;

/**
 * An activator for the {@link XRepository} service.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryActivator implements BundleActivator {

    private ServiceRegistration registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        ArtifactProviderPlugin provider = new TrackingArtifactHandler(context);
        RepositoryCachePlugin cache = new AbstractRepositoryCache();
        RepositoryImpl service = new RepositoryImpl(provider,  cache);
        registration = context.registerService(Repository.class.getName(), service, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null)
            registration.unregister();
    }

    class TrackingArtifactHandler implements ArtifactProviderPlugin {

        private ArtifactProviderPlugin delegate;

        TrackingArtifactHandler(BundleContext context) {
            delegate = new SimpleArtifactHandler(context);
            ServiceTracker tracker = new ServiceTracker(context, ArtifactProviderPlugin.class.getName(), null) {

                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    delegate = (ArtifactProviderPlugin) service;
                }

                @Override
                public void removedService(ServiceReference reference, Object service) {
                    super.removedService(reference, service);
                    delegate = new SimpleArtifactHandler(context);
                }
            };
            tracker.open();
        }

        @Override
        public Collection<Capability> findProviders(Requirement req) {
            return delegate.findProviders(req);
        }
    }

}