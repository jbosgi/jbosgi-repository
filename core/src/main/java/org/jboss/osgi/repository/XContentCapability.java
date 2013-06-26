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

import org.jboss.osgi.resolver.XCapability;

/**
 * A content capability
 * 
 * @author thomas.diesler@jboss.com
 * @since 05-Jul-2012
 */
public interface XContentCapability extends XCapability {

    String DEFAULT_DIGEST = "default-digest";
    String DEFAULT_MIME_TYPE = "application/octet-stream";
    Long DEFAULT_SIZE = new Long(-1);

    /**
     * An IANA defined MIME type for the format of this content
     */
    String getMimeType();

    /**
     * The SHA-256 hex encoded digest for this resource
     */
    String getDigest();
    
    /**
     * The URL to the bytes. This must be an absolute URL
     */
    String getContentURL();

    /**
     * The size of the resource in bytes as it will be read from the URL
     */
    Long getSize();
}
