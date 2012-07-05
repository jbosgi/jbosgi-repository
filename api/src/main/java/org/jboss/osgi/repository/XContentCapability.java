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