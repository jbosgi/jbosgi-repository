/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.osgi.repository.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XCapability;
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
    private final XRepository delegate;

    public AbstractPersistentRepository(RepositoryStorage storage, XRepository delegate) {
        this.storage = storage;
        this.delegate = delegate;
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        Collection<Capability> providers = storage.findProviders(req);
        if (providers.isEmpty()) {
            providers = delegate.findProviders(req);
            if (providers.isEmpty() == false) {
                List<Capability> caplist = new ArrayList<Capability>(providers);
                for (int i =0; i < caplist.size(); i++) {
                    XCapability orgcap = (XCapability) caplist.get(i);
                    XResource orgres = (XResource) orgcap.getResource();
                    XResource newres = storage.addResource(orgres);
                    if (newres != orgres) {
                        String namespace = orgcap.getNamespace();
                        Object orgval = orgcap.getAttributes().get(namespace);
                        for (Capability newcap : newres.getCapabilities(namespace)) {
                            Object newval = newcap.getAttributes().get(namespace);
                            if (orgval.equals(newval)) {
                                caplist.set(i, newcap);
                            }
                        }
                    }
                }
                providers = caplist;
            }
        }
        return providers;
    }

    @Override
    public RepositoryStorage getRepositoryStorage() {
        return storage;
    }
}