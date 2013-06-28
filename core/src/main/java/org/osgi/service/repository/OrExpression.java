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

import java.util.List;

/**
 * A {@link RequirementExpression} representing multiple requirements combined
 * together using the {@code or} operator.
 *
 * @ThreadSafe
 * @noimplement
 */
public interface OrExpression extends RequirementExpression {
	/**
	 * Obtain the requirements that are combined using the {@code or} operator.
	 *
	 * @return The requirements, represented as {@link RequirementExpression}
	 *         objects.
	 */
	List<RequirementExpression> getRequirements();
}
