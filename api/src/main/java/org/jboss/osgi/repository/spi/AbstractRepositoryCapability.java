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

package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.resolver.internal.ResolverMessages.MESSAGES;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_MIME_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.osgi.repository.XContentCapability;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.spi.AbstractCapability;

/**
 * The abstract implementation of a {@link XContentCapability}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 05-Jul-2012
 */
public class AbstractRepositoryCapability extends AbstractCapability implements XContentCapability {

    private String mimeType;
    private String digest;
    private String contentURL;
    private Long size;

    public AbstractRepositoryCapability(XResource resource, String namespace, Map<String, Object> atts, Map<String, String> dirs) {
        super(resource, namespace, replaceAttributeTypes(atts), dirs);
    }

    private static Map<String, Object> replaceAttributeTypes(Map<String, Object> atts) {
        Map<String, Object> result = new HashMap<String, Object>(atts);
        Object val = result.get(CAPABILITY_SIZE_ATTRIBUTE);
        if (val instanceof String) {
            result.put(CAPABILITY_SIZE_ATTRIBUTE, Long.parseLong((String)val));
        }
        return result;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getDigest() {
        return digest;
    }

    @Override
    public String getContentURL() {
        return contentURL;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public void validate() {
        super.validate();
        if (CONTENT_NAMESPACE.equals(getNamespace())) {
            digest = (String) getAttribute(CONTENT_NAMESPACE);
            if (digest == null)
                throw MESSAGES.illegalStateCannotObtainAttribute(CONTENT_NAMESPACE);
            mimeType = (String) getAttribute(CAPABILITY_MIME_ATTRIBUTE);
            if (mimeType == null)
                throw MESSAGES.illegalStateCannotObtainAttribute(CAPABILITY_MIME_ATTRIBUTE);
            mimeType = (String) getAttribute(CAPABILITY_MIME_ATTRIBUTE);
            if (mimeType == null)
                throw MESSAGES.illegalStateCannotObtainAttribute(CAPABILITY_MIME_ATTRIBUTE);
            contentURL = (String) getAttribute(CAPABILITY_URL_ATTRIBUTE);
            if (contentURL == null)
                throw MESSAGES.illegalStateCannotObtainAttribute(CAPABILITY_URL_ATTRIBUTE);
            size = (Long) getAttribute(CAPABILITY_SIZE_ATTRIBUTE);
            if (size == null)
                throw MESSAGES.illegalStateCannotObtainAttribute(CAPABILITY_SIZE_ATTRIBUTE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends XCapability> T adapt(Class<T> clazz) {
        T result = super.adapt(clazz);
        if (result == null) {
            if (XContentCapability.class == clazz && CONTENT_NAMESPACE.equals(getNamespace())) {
                result = (T) this;
            }
        }
        return result;
    }

}