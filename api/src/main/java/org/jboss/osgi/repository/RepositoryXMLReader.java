/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.osgi.repository.Namespace100.Attribute;
import org.jboss.osgi.repository.Namespace100.Element;
import org.jboss.osgi.repository.Namespace100.Type;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.osgi.framework.Version;
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
            throw MESSAGES.illegalStateCannotInitializeRepositoryReader(ex);
        }
        try {
            reader.require(START_DOCUMENT, null, null);
            reader.nextTag();
            reader.require(START_ELEMENT, REPOSITORY_NAMESPACE, REPOSITORY.getLocalName());
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        } catch (Exception ex) {
            throw MESSAGES.storageCannotReadResourceElement(ex, reader.getLocation());
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
            throw MESSAGES.storageCannotReadResourceElement(ex, reader.getLocation());
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
        XResourceBuilder builder = XResourceBuilderFactory.create();
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
                    throw MESSAGES.storageInvalidContentURL(urlspec);
                }
                builder = URLResourceBuilderFactory.create(contentURL, ccap.getAttributes(), false);
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

    private void readCapabilityElement(XMLStreamReader reader, XResourceBuilder builder) throws XMLStreamException {
        String namespace = reader.getAttributeValue(null, Attribute.NAMESPACE.toString());
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        readAttributesAndDirectives(reader, atts, dirs);
        try {
            builder.addCapability(namespace, atts, dirs);
        } catch (RuntimeException ex) {
            throw MESSAGES.storageCannotReadResourceElement(ex, reader.getLocation());
        }
    }

    private void readRequirementElement(XMLStreamReader reader, XResourceBuilder builder) throws XMLStreamException {
        String namespace = reader.getAttributeValue(null, Attribute.NAMESPACE.toString());
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        readAttributesAndDirectives(reader, atts, dirs);
        try {
            builder.addRequirement(namespace, atts, dirs);
        } catch (RuntimeException ex) {
            throw MESSAGES.storageCannotReadResourceElement(ex, reader.getLocation());
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
        boolean listType = false;
        String name = reader.getAttributeValue(null, Attribute.NAME.toString());
        String valstr = reader.getAttributeValue(null, Attribute.VALUE.toString());
        String typespec = reader.getAttributeValue(null, Attribute.TYPE.toString());
        if (typespec != null && typespec.startsWith("List<") && typespec.endsWith(">")) {
            typespec = typespec.substring(5, typespec.length() - 1);
            listType = true;
        }
        Type type = typespec != null ? Type.valueOf(typespec) : Type.String;
        
        // Whitespace around the list and around commas must be trimmed
        valstr = valstr.trim();
        
        Object value;
        switch (type) {
            case String:
                if (listType) {
                    List<String> list = new ArrayList<String>();
                    String[] split = valstr.split(",");
                    for (String val : split) {
                        list.add(val.trim());
                    }
                    value = list;
                } else {
                    value = valstr;
                }
                break;
            case Version:
                if (listType) {
                    List<Version> list = new ArrayList<Version>();
                    String[] split = valstr.split(",");
                    for (String val : split) {
                        list.add(Version.parseVersion(val.trim()));
                    }
                    value = list;
                } else {
                    value = Version.parseVersion(valstr);
                }
                break;
            case Long:
                if (listType) {
                    List<Long> list = new ArrayList<Long>();
                    String[] split = valstr.split(",");
                    for (String val : split) {
                        list.add(Long.parseLong(val.trim()));
                    }
                    value = list;
                } else {
                    value = Long.parseLong(valstr);
                }
                break;
            case Double:
                if (listType) {
                    List<Double> list = new ArrayList<Double>();
                    String[] split = valstr.split(",");
                    for (String val : split) {
                        list.add(Double.parseDouble(val.trim()));
                    }
                    value = list;
                } else {
                    value = Double.parseDouble(valstr);
                }
                break;
            default:
                value = valstr;
                break;
        }
        attributes.put(name, value);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
        }
    }

    private void readDirectiveElement(XMLStreamReader reader, Map<String, String> directives) throws XMLStreamException {
        String name = reader.getAttributeValue(null, Attribute.NAME.toString());
        String value = reader.getAttributeValue(null, Attribute.VALUE.toString());
        directives.put(name, value);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
        }
    }
}
