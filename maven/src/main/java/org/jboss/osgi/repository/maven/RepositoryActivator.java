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
import org.jboss.osgi.repository.XRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.net.URL;

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
        ArtifactHandler artifactHandler = new TrackingArtifactHandler(context);
        MavenDelegateRepository service = new MavenDelegateRepository(artifactHandler);
        registration = context.registerService(Repository.class.getName(), service, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null)
            registration.unregister();
    }

    class TrackingArtifactHandler implements ArtifactHandler {

        private ArtifactHandler delegate;

        TrackingArtifactHandler(BundleContext context) {
            delegate = new URLStreamArtifactHandler(context);
            ServiceTracker tracker = new ServiceTracker(context, ArtifactHandler.class.getName(), null) {

                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    delegate = (ArtifactHandler) service;
                }

                @Override
                public void removedService(ServiceReference reference, Object service) {
                    super.removedService(reference, service);
                    delegate = new URLStreamArtifactHandler(context);                }
            };
            tracker.open();
        }

        @Override
        public URL[] resolveArtifacts(ArtifactCoordinates coordinates) {
            return delegate.resolveArtifacts(coordinates);
        }

        @Override
        public void storeArtifacts(ArtifactCoordinates coordinates, URL[] urls) {
            delegate.storeArtifacts(coordinates, urls);
        }
    }

}