/*
 * Copyright (c) OSGi Alliance (2006, 2013). All Rights Reserved.
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

import java.util.Collection;
import java.util.Map;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A repository service that contains {@link Resource resources}.
 * 
 * <p>
 * Repositories may be registered as services and may be used as by a resolve
 * context during resolver operations.
 * 
 * <p>
 * Repositories registered as services may be filtered using standard service
 * properties.
 * 
 * @ThreadSafe
 * @author $Id: a19f4e42f0fc6fbfe8c2ed5eccc6203f737474f6 $
 */
@ProviderType
public interface Repository {
	/**
	 * Service property to provide URLs related to this repository.
	 * 
	 * <p>
	 * The value of this property must be of type {@code String},
	 * {@code String[]}, or {@code Collection<String>}.
	 */
	String	URL	= "repository.url";

	/**
	 * Find the capabilities that match the specified requirements.
	 * 
	 * @param requirements The requirements for which matching capabilities
	 *        should be returned. Must not be {@code null}.
	 * @return A map of matching capabilities for the specified requirements.
	 *         Each specified requirement must appear as a key in the map. If
	 *         there are no matching capabilities for a specified requirement,
	 *         then the value in the map for the specified requirement must be
	 *         an empty collection. The returned map is the property of the
	 *         caller and can be modified by the caller.
	 */
	Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements);

	/**
	 * Find the resources that match the specified requirement expression.
	 * 
	 * @param expression The {@code RequirementExpression} for which matching
	 *        capabilities should be returned. Must not be {@code null}.
	 * @return A collection of matching {@code Resource}s. If there are no
	 *         matching resources, an empty collection is returned. The returned
	 *         collection is the property of the caller and can be modified by
	 *         the caller.
	 * @since 1.1
	 */
	Collection<Resource> findProviders(RequirementExpression expression);

	/**
	 * Return an expression combiner. An expression combiner can be used to
	 * combine multiple requirement expressions into more complex requirement
	 * expressions using {@link AndExpression and}, {@link OrExpression or} and
	 * {@link NotExpression not} operators.
	 * 
	 * @return An {@code ExpressionCombiner}.
	 * @since 1.1
	 */
	ExpressionCombiner getExpressionCombiner();

	/**
	 * Return a new {@code RequirementBuilder} which provides a convenient way
	 * to create a requirement.
	 * 
	 * <p>
	 * For example:
	 * 
	 * <pre> 
     * Requirement myReq = repository.newRequirementBuilder("org.foo.ns1").
     *   addDirective("filter", "(org.foo.ns1=val1)").
     *   addDirective("cardinality", "multiple").build();
	 * </pre>
	 * 
	 * @param namespace The namespace for the requirement to be created.
	 * @return A new requirement builder for a requirement in the specified
	 *         namespace.
	 * @since 1.1
	 */
	RequirementBuilder newRequirementBuilder(String namespace);
}
