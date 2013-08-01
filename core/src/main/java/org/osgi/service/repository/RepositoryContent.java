/*
 * Copyright (c) OSGi Alliance (2012, 2013). All Rights Reserved.
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

import java.io.InputStream;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Resource;

/**
 * An accessor for the default content of a resource.
 * 
 * All {@link Resource} objects which represent resources in a
 * {@link Repository} must implement this interface. A user of the resource can
 * then cast the {@link Resource} object to this type and then obtain an
 * {@code InputStream} to the default content of the resource.
 * 
 * @ThreadSafe
 * @author $Id: 3af096bb605a4896abe99e5f546ea04b99f3b3e2 $
 */
@ProviderType
public interface RepositoryContent {

	/**
	 * Returns a new input stream to the default format of this resource.
	 * 
	 * @return A new input stream for associated resource.
	 */
	InputStream getContent();
}
