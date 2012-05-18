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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.osgi.repository.XRepository;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * An abstract  {@link XRepository} that does nothing.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public abstract class AbstractRepository implements XRepository {

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        if (requirements == null)
            throw MESSAGES.illegalArgumentNull("requirements");

        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement req : requirements) {
            Collection<Capability> providers = findProviders(req);
            result.put(req, providers);
        }
        return result;
    }

    @Override
    public abstract Collection<Capability> findProviders(Requirement req);
}