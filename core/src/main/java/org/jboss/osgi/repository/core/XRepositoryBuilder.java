/*
 * #%L
 * JBossOSGi Repository: Bundle
 * %%
 * Copyright (C) 2011 - 2012 JBoss by Red Hat
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

package org.jboss.osgi.repository.core;

import static org.jboss.osgi.repository.XRepository.SERVICE_NAMES;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractPersistentRepository;
import org.jboss.osgi.repository.spi.AggregatingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A builder for {@link XRepository} services.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-May-2012
 */
public class XRepositoryBuilder {

    public static final String ROOT_REPOSITORY = "root-repository";

    private final Set<ServiceRegistration> registrations = new CopyOnWriteArraySet<ServiceRegistration>();
    private final BundleContext context;
    private ServiceTracker repositoryStorageFactoryTracker;

    public static XRepositoryBuilder create(BundleContext context) {
        return new XRepositoryBuilder(context);
    }

    private XRepositoryBuilder(BundleContext context) {
        this.context = context;
    }

    public void addDefaultRepositoryStorage(final File storageDir) {
        RepositoryStorageFactory factory = new RepositoryStorageFactory() {
            @Override
            public RepositoryStorage create(XRepository repository) {
                return new FileBasedRepositoryStorage(repository, storageDir);
            }
        };
        addRepositoryStorage(factory);
    }

    public void addRepositoryStorage(RepositoryStorageFactory factory) {
        registrations.add(context.registerService(RepositoryStorageFactory.class.getName(), factory, null));
    }

    public void addDefaultRepositories() {

        // Register the maven artifact repository
        addRepository(new MavenArtifactRepository());

        repositoryStorageFactoryTracker = new ServiceTracker(context, RepositoryStorageFactory.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object svc = super.addingService(reference);
                if (svc instanceof RepositoryStorageFactory) {
                    RepositoryStorageFactory factory = (RepositoryStorageFactory) svc;

                    // Register the root repository
                    XRepository repository = new AbstractPersistentRepository(factory, getRepositoryServiceTracker());
                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(Constants.SERVICE_RANKING, new Integer(1000));
                    props.put(Constants.SERVICE_DESCRIPTION, repository.getName());
                    props.put(ROOT_REPOSITORY, Boolean.TRUE);
                    ServiceRegistration reg = context.registerService(SERVICE_NAMES, repository, props);
                    registrations.add(reg);
                    return reg;
                }
                return null;
            }

            @Override
            public void removedService(ServiceReference reference, Object registration) {
                if (registration instanceof ServiceRegistration) {
                    ServiceRegistration reg = (ServiceRegistration) registration;
                    reg.unregister();
                    registrations.remove(reg);
                }

                super.removedService(reference, registration);
            }
        };
        repositoryStorageFactoryTracker.open();
    }

    public void addRepository(XRepository repository) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, repository.getName());
        registrations.add(context.registerService(SERVICE_NAMES, repository, props));
    }

    public XRepository getRepositoryServiceTracker() {
        return new RepositoryServiceTracker(context);
    }

    public Set<ServiceRegistration> getRegistrations() {
        return Collections.unmodifiableSet(registrations);
    }

    public void unregisterServices() {
        repositoryStorageFactoryTracker.close();
        for (ServiceRegistration reg : getRegistrations()) {
            reg.unregister();
        }
    }

    static class RepositoryServiceTracker extends AggregatingRepository {

        public RepositoryServiceTracker(final BundleContext context) {
            ServiceTracker tracker = new ServiceTracker(context, XRepository.class.getName(), null) {
                @Override
                public Object addingService(ServiceReference sref) {
                    XRepository repo = (XRepository) super.addingService(sref);
                    if (sref.getProperty(ROOT_REPOSITORY) == null) {
                        addRepository(repo);
                    }
                    return repo;
                }

                @Override
                public void removedService(ServiceReference sref, Object service) {
                    XRepository repo = (XRepository) service;
                    if (sref.getProperty(ROOT_REPOSITORY) == null) {
                        removeRepository(repo);
                    }
                    super.removedService(sref, service);
                }
            };
            tracker.open();
        }
    }
}