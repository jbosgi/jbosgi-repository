/*
 * Copyright (c) OSGi Alliance (2013). All Rights Reserved.
 *
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
 */
package org.osgi.service.repository;

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A builder for {@code Requirement} objects.
 *
 * @noimplement
 */
public interface RequirementBuilder {
	/**
	 * Add an attribute to the requirement.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return a builder object that can be used to further define the
	 *         requirement.
	 */
	RequirementBuilder addAttribute(String name, Object value);

	/**
	 * Add a directive to the requirement.
	 *
	 * @param name The directive name.
	 * @param value The directive value.
	 * @return a builder object that can be used to further define the
	 *         requirement.
	 */
	RequirementBuilder addDirective(String name, String value);

	/**
	 * Set all the attributes to the values in the provided map. This will
	 * replace any previous attribute set on the builder.
	 *
	 * @param attrs The map of attributes to use.
	 * @return a builder object that can be used to further define the
	 *         requirement.
	 */
	RequirementBuilder setAttributes(Map<String, Object> attrs);

	/**
	 * Set all the directives to the values in the provided map. This will
	 * replace any previous directives set on the builder.
	 *
	 * @param dirs The map of directives to use.
	 * @return a builder object that can be used to further define the
	 *         requirement.
	 */
	RequirementBuilder setDirectives(Map<String, String> dirs);

	/**
	 * Specifies the {@code Resource} object for the requirement. Note that
	 * providing a resource is optional.
	 *
	 * @param resource The resource for the requirement. Will overwrite any
	 *        previous resource if provided.
	 * @return a builder object that can be used to further define the
	 *         requirement.
	 */
	RequirementBuilder setResource(Resource resource);

	/**
	 * Build the requirement according to the specification provided to the
	 * builder.
	 *
	 * @return the requirement.
	 */
	Requirement build();
}
