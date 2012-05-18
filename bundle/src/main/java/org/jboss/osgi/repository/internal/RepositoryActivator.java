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

import java.util.Dictionary;
import java.util.Hashtable;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.core.MavenArtifactRepository;
import org.jboss.osgi.repository.spi.DefaultStorageRepository;
import org.jboss.osgi.repository.spi.AggregatingRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An activator for the {@link Repository} service.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryActivator implements BundleActivator {

    private final String[] SERVICE_NAMES = new String[] { XRepository.class.getName(), Repository.class.getName() };

    private XRepository repository;

    @Override
    public void start(final BundleContext context) throws Exception {

        // Register the repository storage service
        RepositoryStorage storage = new FileBasedRepositoryStorage(context.getDataFile("repository"));
        context.registerService(RepositoryStorage.class.getName(), storage, null);

        // Register the maven artifact repository
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "JBossOSGi Maven Artifact Repository");
        context.registerService(SERVICE_NAMES, new MavenArtifactRepository(), props);

        ServiceTracker storageTracker = new ServiceTracker(context, RepositoryStorage.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                RepositoryStorage storage = (RepositoryStorage) super.addingService(reference);
                if (repository == null) {
                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
                    props.put(Constants.SERVICE_DESCRIPTION, "JBossOSGi Aggregating Repository");
                    repository = new DefaultStorageRepository(storage, new RepositoryDelegate(context));
                    context.registerService(SERVICE_NAMES, repository, props);
                }
                return storage;
            }
        };
        storageTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    class RepositoryDelegate extends AggregatingRepository {

        public RepositoryDelegate(final BundleContext context) {
            ServiceTracker repoTracker = new ServiceTracker(context, XRepository.class.getName(), null) {
                @Override
                public Object addingService(ServiceReference reference) {
                    XRepository repo = (XRepository) super.addingService(reference);
                    if (repo != repository) {
                        addRepository(repo);
                    }
                    return repo;
                }

                @Override
                public void removedService(ServiceReference reference, Object service) {
                    XRepository repo = (XRepository) service;
                    if (repo != repository) {
                        removeRepository(repo);
                    }
                    super.removedService(reference, service);
                }
            };
            repoTracker.open();
        }
    }
}