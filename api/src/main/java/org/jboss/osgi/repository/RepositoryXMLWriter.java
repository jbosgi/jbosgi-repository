/*
 * #%L
 * JBossOSGi Repository: API
 * %%
 * Copyright (C) 2011 - 2012 JBoss by Red Hat
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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;
import static org.jboss.osgi.repository.Namespace100.REPOSITORY_NAMESPACE;
import static org.jboss.osgi.repository.Namespace100.Attribute.NAME;
import static org.jboss.osgi.repository.Namespace100.Attribute.NAMESPACE;
import static org.jboss.osgi.repository.Namespace100.Attribute.TYPE;
import static org.jboss.osgi.repository.Namespace100.Attribute.VALUE;
import static org.jboss.osgi.repository.Namespace100.Element.ATTRIBUTE;
import static org.jboss.osgi.repository.Namespace100.Element.CAPABILITY;
import static org.jboss.osgi.repository.Namespace100.Element.DIRECTIVE;
import static org.jboss.osgi.repository.Namespace100.Element.REPOSITORY;
import static org.jboss.osgi.repository.Namespace100.Element.REQUIREMENT;
import static org.jboss.osgi.repository.Namespace100.Element.RESOURCE;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.osgi.repository.Namespace100.Type;
import org.jboss.osgi.resolver.XResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Write repository contnet to XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-May-2012
 */
public class RepositoryXMLWriter implements RepositoryWriter {

    private final XMLStreamWriter writer;

    public static RepositoryWriter create(OutputStream output) {
        return new RepositoryXMLWriter(output);
    }

    private RepositoryXMLWriter(OutputStream output) {
        try {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
        } catch (Exception ex) {
            throw MESSAGES.illegalStateCannotInitializeRepositoryWriter(ex);
        }
    }

    @Override
    public void writeRepositoryAttributes(Map<String, String> attributes) {
        try {
            writer.writeStartDocument();
            writer.setDefaultNamespace(REPOSITORY_NAMESPACE);
            writer.writeStartElement(REPOSITORY.getLocalName());
            writer.writeDefaultNamespace(REPOSITORY_NAMESPACE);
            for (Entry<String, String> entry : attributes.entrySet()) {
                writer.writeAttribute(entry.getKey(), entry.getValue());
            }
        } catch (XMLStreamException ex) {
            throw MESSAGES.illegalStateCannotWriteRepositoryElement(ex);
        }
    }

    @Override
    public void writeResource(XResource resource) {
        try {
            writer.writeStartElement(RESOURCE.getLocalName());
            for (Capability cap : resource.getCapabilities(null)) {
                writer.writeStartElement(CAPABILITY.getLocalName());
                writer.writeAttribute(NAMESPACE.getLocalName(), cap.getNamespace());
                writeAttributes(cap.getAttributes());
                writeDirectives(cap.getDirectives());
                writer.writeEndElement();
            }
            for (Requirement req : resource.getRequirements(null)) {
                writer.writeStartElement(REQUIREMENT.getLocalName());
                writer.writeAttribute(NAMESPACE.getLocalName(), req.getNamespace());
                writeAttributes(req.getAttributes());
                writeDirectives(req.getDirectives());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } catch (XMLStreamException ex) {
            throw MESSAGES.illegalStateCannotWriteRepositoryElement(ex);
        }
    }

    @Override
    public void close() {
        try {
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException ex) {
            throw MESSAGES.illegalStateCannotWriteRepositoryElement(ex);
        }
    }

    private void writeAttributes(Map<String, Object> attributes) throws XMLStreamException {
        for (Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            Class<?> clazz = value.getClass();
            boolean listType = List.class.isAssignableFrom(clazz);
            writer.writeStartElement(ATTRIBUTE.getLocalName());
            writer.writeAttribute(NAME.getLocalName(), entry.getKey());
            if (listType) {
                String valstr = value.toString();
                valstr = valstr.substring(1, valstr.length() -1);
                writer.writeAttribute(VALUE.getLocalName(), valstr);
                Class<?> type = ((List<?>)value).toArray().getClass().getComponentType();
                if (type != String.class) {
                    writer.writeAttribute(TYPE.getLocalName(), "List<" + type.getSimpleName() + ">");
                }
            } else {
                writer.writeAttribute(VALUE.getLocalName(), value.toString());
                if (clazz != String.class) {
                    Type type = Type.valueOf(clazz.getSimpleName());
                    writer.writeAttribute(TYPE.getLocalName(), type.toString());
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeDirectives(Map<String, String> directives) throws XMLStreamException {
        for (Entry<String, String> entry : directives.entrySet()) {
            writer.writeStartElement(DIRECTIVE.getLocalName());
            writer.writeAttribute(NAME.getLocalName(), entry.getKey());
            writer.writeAttribute(VALUE.getLocalName(), entry.getValue());
            writer.writeEndElement();
        }
    }
}
