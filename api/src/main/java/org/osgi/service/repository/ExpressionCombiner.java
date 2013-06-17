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

import org.osgi.resource.Requirement;

/**
 * An {@code ExpressionCombiner} can be used to combine multiple requirements
 * into a single complex requirement using the {@code and}, {@code or} and
 * {@code not} operators.
 *
 */
public interface ExpressionCombiner {
	/**
	 * Combine multiple {@link Requirement} objects into a single expression
	 * using the {@code and} operator.
	 *
	 * @param reqs The requirements to combine.
	 * @return A {@link RequirementExpression} representing the combined
	 *         requirements.
	 */
	RequirementExpression and(Requirement... reqs);

	/**
	 * Combine multiple {@link RequirementExpression} objects into a single
	 * expression using the {@code and} operator.
	 *
	 * @param reqs The requirements to combine.
	 * @return A {@link RequirementExpression} representing the combined
	 *         requirements.
	 */
	RequirementExpression and(RequirementExpression... reqs);

	/**
	 * Convert a {@link Requirement} into a {@link RequirementExpression}. This
	 * can be useful when working with a combination of {@code Requirement} and
	 * {@code RequirementExpresion} objects.
	 *
	 * @param req The requirement to convert.
	 * @return A {@link RequirementExpression} representing the requirement.
	 */
	RequirementExpression expression(Requirement req);

	/**
	 * Provide the negative of a {@link Requirement}.
	 *
	 * @param req The requirement to provide the negative of.
	 * @return A {@link RequirementExpression} representing the negative of the
	 *         requirement.
	 */
	RequirementExpression not(Requirement req);

	/**
	 * Provide the negative of a {@link RequirementExpression}.
	 *
	 * @param req The requirement to provide the negative of.
	 * @return A {@link RequirementExpression} representing the negative of the
	 *         requirement.
	 */
	RequirementExpression not(RequirementExpression req);

	/**
	 * Combine multiple {@link Requirement} objects into a single expression
	 * using the {@code or} operator.
	 *
	 * @param reqs The requirements to combine.
	 * @return A {@link RequirementExpression} representing the combined
	 *         requirements.
	 */
	RequirementExpression or(Requirement... reqs);

	/**
	 * Combine multiple {@link RequirementExpression} objects into a single
	 * expression using the {@code or} operator.
	 *
	 * @param reqs The requirements to combine.
	 * @return A {@link RequirementExpression} representing the combined
	 *         requirements.
	 */
	RequirementExpression or(RequirementExpression... reqs);
}
