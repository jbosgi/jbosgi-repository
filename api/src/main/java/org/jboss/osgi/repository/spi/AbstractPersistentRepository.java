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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.util.Collection;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
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
    private XRepository delegate;

    public AbstractPersistentRepository(RepositoryStorageFactory factory) {
        this(factory, null);
    }

    public AbstractPersistentRepository(RepositoryStorageFactory factory, XRepository delegate) {
        if (factory == null)
            throw MESSAGES.illegalArgumentNull("factory");

        this.storage = factory.create(this);
        this.delegate = delegate;
    }

    @Override
    public RepositoryStorage getRepositoryStorage() {
        return storage;
    }

    @Override
    public XRepository getRepository() {
        return delegate;
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        if (req == null)
            throw MESSAGES.illegalArgumentNull("req");

        Collection<Capability> providers = storage.findProviders(req);
        if (providers.isEmpty() && delegate != null) {
            providers = delegate.findProviders(req);
        }
        return providers;
    }

}