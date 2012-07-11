/*
 * #%L
 * JBossOSGi Resolver API
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
        XRequirement req = builder.addRequirement(namespace, nsvalue);
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
