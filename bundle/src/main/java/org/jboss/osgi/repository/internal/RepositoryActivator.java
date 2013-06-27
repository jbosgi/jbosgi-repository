/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
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
package org.jboss.osgi.repository.internal;

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.ResourceInstaller;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractPersistentRepository;
import org.jboss.osgi.repository.spi.AbstractResourceInstaller;
import org.jboss.osgi.repository.spi.FileBasedRepositoryStorage;
import org.jboss.osgi.repository.spi.MavenIdentityRepository;
import org.jboss.osgi.repository.spi.MavenIdentityRepository.ConfigurationPropertyProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.repository.Repository;

/**
 * An activator for {@link Repository} services.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryActivator implements BundleActivator {

    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    @Override
    public void start(final BundleContext context) throws Exception {

        // Create the {@link ConfigurationPropertyProvider}
        final ConfigurationPropertyProvider propProvider = new ConfigurationPropertyProvider() {
            @Override
            public String getProperty(String key, String defaultValue) {
                String value = context.getProperty(key);
                return value != null ? value : defaultValue;
            }
        };

        // Create the {@link RepositoryStorageFactory}
        final RepositoryStorageFactory factory = new RepositoryStorageFactory() {
            @Override
            public RepositoryStorage create(XRepository repository) {
                File storageDir = getRepositoryStorageDir(propProvider, context);
                return new FileBasedRepositoryStorage(repository, storageDir, propProvider);
            }
        };

        // Setup the repositories
        XPersistentRepository repository = new AbstractPersistentRepository(factory);
        repository.addRepositoryDelegate(new MavenIdentityRepository(propProvider));

        // Register the {@link XRepository} service
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, repository.getName());
        String[] serviceNames = new String[] { XRepository.class.getName(), Repository.class.getName() };
        registrations.add(context.registerService(serviceNames, repository, props));
        
        // Register the {@link ResourceInstaller} service
        ResourceInstaller installer = new AbstractResourceInstaller();
        registrations.add(context.registerService(ResourceInstaller.class, installer, null));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
    }

    private File getRepositoryStorageDir(ConfigurationPropertyProvider propProvider, BundleContext context) {
        String dirName = propProvider.getProperty(XRepository.PROPERTY_REPOSITORY_STORAGE_DIR, null);
        if (dirName == null) {
            dirName = propProvider.getProperty(Constants.FRAMEWORK_STORAGE, null);
            if (dirName == null) {
                try {
                    File storageDir = context.getDataFile("osgi-store");
                    dirName = storageDir.getCanonicalPath();
                } catch (IOException ex) {
                    throw MESSAGES.cannotCreateRepositoryStorageArea(ex);
                }
            }
            dirName += "/repository";
        }
        return new File(dirName).getAbsoluteFile();
    }
}