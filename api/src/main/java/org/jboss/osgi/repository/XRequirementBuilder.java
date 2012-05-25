/*
 * #%L
 * JBossOSGi Resolver API
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.jboss.osgi.repository;


import static org.jboss.osgi.repository.XRepository.MAVEN_IDENTITY_NAMESPACE;
import static org.jboss.osgi.repository.XRepository.MODULE_IDENTITY_NAMESPACE;

import java.util.Map;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;

/**
 * A builder for resource requirements
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public final class XRequirementBuilder {

    private final XResourceBuilder builder;
    private final XRequirement requirement;

    public static XRequirementBuilder create(ModuleIdentifier moduleId) {
        return create(MODULE_IDENTITY_NAMESPACE, moduleId.toString());
    }

    public static XRequirementBuilder create(MavenCoordinates coordinates) {
        return create(MAVEN_IDENTITY_NAMESPACE, coordinates.toExternalForm());
    }

    public static XRequirementBuilder create(String namespace) {
        return create(namespace, null);
    }

    public static XRequirementBuilder create(String namespace, String nsvalue) {
        XResourceBuilder builder = XResourceBuilderFactory.create();
        XRequirement req = builder.addGenericRequirement(namespace);
        if (nsvalue != null) {
            req.getAttributes().put(namespace, nsvalue);
        }
        return new XRequirementBuilder(builder, req);
    }

    private XRequirementBuilder(XResourceBuilder builder, XRequirement requirement) {
        this.builder = builder;
        this.requirement = requirement;
    }

    public Map<String, Object> getAttributes() {
        return requirement.getAttributes();
    }

    public Map<String, String> getDirectives() {
        return requirement.getDirectives();
    }

    public XRequirement getRequirement() {
        XResource resource = builder.getResource();
        String namespace = requirement.getNamespace();
        return (XRequirement) resource.getRequirements(namespace).get(0);
    }

}