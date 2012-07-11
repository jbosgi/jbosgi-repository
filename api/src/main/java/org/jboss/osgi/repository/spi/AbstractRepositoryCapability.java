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
