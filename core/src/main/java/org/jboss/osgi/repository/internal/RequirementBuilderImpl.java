/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.RequirementBuilder;

/**
 * @author David Bosschaert
 */
public class RequirementBuilderImpl implements RequirementBuilder {
    private final String namespace;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private Map<String, String> directives = new HashMap<String, String>();
    private Resource resource = null;

    public RequirementBuilderImpl(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public synchronized RequirementBuilder addAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public synchronized RequirementBuilder addDirective(String name, String value) {
        directives.put(name, value);
        return this;
    }

    @Override
    public synchronized RequirementBuilder setAttributes(Map<String, Object> attrs) {
        attributes = new HashMap<String, Object>(attrs);
        return this;
    }

    @Override
    public synchronized RequirementBuilder setDirectives(Map<String, String> dirs) {
        directives = new HashMap<String, String>(dirs);
        return this;
    }

    @Override
    public synchronized RequirementBuilder setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public synchronized Requirement build() {
        return new RequirementImpl(namespace, attributes, directives, resource);
    }

    @Override
    public IdentityExpression buildExpression() {
        final Requirement req = build();
        return new IdentityExpression() {
            @Override
            public Requirement getRequirement() {
                return req;
            }
        };
    }

    private static final class RequirementImpl implements Requirement {
        private final String namespace;
        private final Map<String, Object> attributes;
        private final Map<String, String> directives;
        private final Resource resource;

        public RequirementImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs, Resource res) {
            namespace = ns;
            attributes = Collections.unmodifiableMap(attrs);
            directives = Collections.unmodifiableMap(dirs);
            resource = res;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Map<String, String> getDirectives() {
            return directives;
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((directives == null) ? 0 : directives.hashCode());
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + ((resource == null) ? 0 : resource.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RequirementImpl other = (RequirementImpl) obj;
            if (attributes == null) {
                if (other.attributes != null)
                    return false;
            } else if (!attributes.equals(other.attributes))
                return false;
            if (directives == null) {
                if (other.directives != null)
                    return false;
            } else if (!directives.equals(other.directives))
                return false;
            if (namespace == null) {
                if (other.namespace != null)
                    return false;
            } else if (!namespace.equals(other.namespace))
                return false;
            if (resource == null) {
                if (other.resource != null)
                    return false;
            } else if (!resource.equals(other.resource))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "RequirementImpl [namespace=" + namespace + ", attributes=" + attributes + ", directives=" + directives
                    + ", resource=" + resource + "]";
        }
    }
}
