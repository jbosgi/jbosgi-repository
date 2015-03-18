/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
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

package org.osgi.util.function;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A predicate that accepts a single argument and produces a boolean result.
 *
 * <p>
 * This is a functional interface and can be used as the assignment target for a
 * lambda expression or method reference.
 * 
 * @param <T> The type of the predicate input.
 * 
 * @ThreadSafe
 * @author $Id: 0c2c61f78bede4e2afc4278af4f5e4b873769347 $
 */
@ConsumerType
public interface Predicate<T> {
	/**
	 * Evaluates this predicate on the specified argument.
	 * 
	 * @param t The input to this predicate.
	 * @return {@code true} if the specified argument is accepted by this
	 *         predicate; {@code false} otherwise.
	 */
	boolean test(T t);
}
