/*
 * #%L
 * JBossOSGi Repository
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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.osgi.repository.Namespace100.REPOSITORY_NAMESPACE;
import static org.jboss.osgi.repository.Namespace100.Element.REPOSITORY;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.osgi.repository.AttributeValueHandler.AttributeValue;
import org.jboss.osgi.repository.Namespace100.Attribute;
import org.jboss.osgi.repository.Namespace100.Element;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;

/**
 * Read repository contnet from XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-May-2012
 */
public class RepositoryXMLReader implements RepositoryReader {

    private final Map<String, String> attributes = new HashMap<String, String>();
    private final XMLStreamReader reader;

    public static RepositoryReader create(InputStream input) {
        return new RepositoryXMLReader(input);
    }

    private RepositoryXMLReader(InputStream input) {
        try {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
        } catch (Exception ex) {
            throw MESSAGES.cannotInitializeRepositoryReader(ex);
        }
        try {
            reader.require(START_DOCUMENT, null, null);
            reader.nextTag();
            reader.require(START_ELEMENT, REPOSITORY_NAMESPACE, REPOSITORY.getLocalName());
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        } catch (Exception ex) {
            throw MESSAGES.cannotReadResourceElement(ex, reader.getLocation());
        }
    }

    @Override
    public Map<String, String> getRepositoryAttributes() {
        return attributes;
    }

    @Override
    public XResource nextResource() {
        try {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case RESOURCE: {
                        return readResourceElement(reader);
                    }
                }
            }
        } catch (XMLStreamException ex) {
            throw MESSAGES.cannotReadResourceElement(ex, reader.getLocation());
        }
        return null;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (XMLStreamException ex) {
            // ignore
        }
    }

    private XResource readResourceElement(XMLStreamReader reader) throws XMLStreamException {
        XResourceBuilder<XResource> builder = XResourceBuilderFactory.create();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            // [TODO] add support for namespaced attributes
            String key = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            builder.addAttribute(key, value);
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CAPABILITY: {
                    readCapabilityElement(reader, builder);
                    break;
                }
                case REQUIREMENT: {
                    readRequirementElement(reader, builder);
                    break;
                }
            }
        }
        XResource resource = builder.getResource();

        // Transform the resource into an URLResource
        List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (caps.size() > 0) {
            XCapability ccap = (XCapability) caps.get(0);
            String urlspec = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
            if (urlspec != null) {
                URL contentURL;
                try {
                    contentURL = new URL(urlspec);
                } catch (MalformedURLException ex) {
                    throw MESSAGES.invalidContentURL(urlspec);
                }
                builder = URLResourceBuilderFactory.create(contentURL, ccap.getAttributes());
                for (Capability cap : resource.getCapabilities(null)) {
                    if (cap != ccap) {
                        builder.addCapability(cap.getNamespace(), cap.getAttributes(), cap.getDirectives());
                    }
                }
                for (Requirement req : resource.getRequirements(null)) {
                    String namespace = req.getNamespace();
                    builder.addRequirement(namespace, req.getAttributes(), req.getDirectives());
                }
                resource = builder.getResource();
            }
        }
        return resource;
    }

    private void readCapabilityElement(XMLStreamReader reader, XResourceBuilder<XResource> builder) throws XMLStreamException {
        String namespace = reader.getAttributeValue(null, Attribute.NAMESPACE.toString());
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        readAttributesAndDirectives(reader, atts, dirs);
        try {
            builder.addCapability(namespace, atts, dirs);
        } catch (RuntimeException ex) {
            throw MESSAGES.cannotReadResourceElement(ex, reader.getLocation());
        }
    }

    private void readRequirementElement(XMLStreamReader reader, XResourceBuilder<XResource> builder) throws XMLStreamException {
        String namespace = reader.getAttributeValue(null, Attribute.NAMESPACE.toString());
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        readAttributesAndDirectives(reader, atts, dirs);
        try {
            builder.addRequirement(namespace, atts, dirs);
        } catch (RuntimeException ex) {
            throw MESSAGES.cannotReadResourceElement(ex, reader.getLocation());
        }
    }

    private void readAttributesAndDirectives(XMLStreamReader reader, Map<String, Object> atts, Map<String, String> dirs) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ATTRIBUTE: {
                    readAttributeElement(reader, atts);
                    break;
                }
                case DIRECTIVE: {
                    readDirectiveElement(reader, dirs);
                    break;
                }
            }
        }
    }

    private void readAttributeElement(XMLStreamReader reader, Map<String, Object> attributes) throws XMLStreamException {
        String name = reader.getAttributeValue(null, Attribute.NAME.toString());
        String valstr = reader.getAttributeValue(null, Attribute.VALUE.toString());
        String typespec = reader.getAttributeValue(null, Attribute.TYPE.toString());
        AttributeValue value = AttributeValueHandler.readAttributeValue(typespec, valstr);
        attributes.put(name, value.getValue());
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT);
    }

    private void readDirectiveElement(XMLStreamReader reader, Map<String, String> directives) throws XMLStreamException {
        String name = reader.getAttributeValue(null, Attribute.NAME.toString());
        String value = reader.getAttributeValue(null, Attribute.VALUE.toString());
        directives.put(name, value);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
        }
    }
}
