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
package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * A {@link XRepository} that delegates to {@link RepositoryStorage}.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public class AbstractPersistentRepository extends AbstractRepository implements XPersistentRepository {

    private final RepositoryStorage storage;
    private final List<XRepository> delegates = new ArrayList<XRepository>();

    public AbstractPersistentRepository(RepositoryStorageFactory factory) {
        if (factory == null)
            throw MESSAGES.illegalArgumentNull("factory");

        this.storage = factory.create(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T adapt(Class<T> type) {
        T result = super.adapt(type);
        if (result == null) {
            if (RepositoryStorage.class.isAssignableFrom(type)) {
                result = (T) storage;
            } else {
                for (XRepository delegate : delegates) {
                    result = delegate.adapt(type);
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public void addRepositoryDelegate(XRepository delegate) {
        synchronized (delegates) {
            if (delegate != this) {
                LOGGER.debugf("Add repository: %s", delegate);
                delegates.add(delegate);
            }
        }
    }

    public void removeRepositoryDelegate(XRepository delegate) {
        synchronized (delegates) {
            if (delegate != this) {
                LOGGER.debugf("Remove repository: %s", delegate);
                delegates.remove(delegate);
            }
        }
    }

    public List<XRepository> getRepositoryDelegates() {
        synchronized (delegates) {
            return Collections.unmodifiableList(delegates);
        }
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        if (req == null)
            throw MESSAGES.illegalArgumentNull("req");

        // Try to find the providers in the storage
        List<Capability> providers = new ArrayList<Capability>();
        providers.addAll(storage.findProviders(req));

        // Try to find the providers in the delegates
        if (providers.isEmpty()) {
            for (XRepository delegate : delegates) {
                Collection<Capability> caps = delegate.findProviders(req);
                if (!caps.isEmpty()) {
                    // Add the delegate resources to the storage
                    for (Capability cap : caps) {
                        XResource res = (XResource) cap.getResource();
                        XIdentityCapability icap = res.getIdentityCapability();
                        XResource storageResource = storage.getResource(icap);
                        if (storageResource == null) {
                            storageResource = storage.addResource(res);
                        }
                        providers.add(storageResource.getIdentityCapability());
                    }
                    break;
                }
            }
        }

        return Collections.unmodifiableList(providers);
    }

}