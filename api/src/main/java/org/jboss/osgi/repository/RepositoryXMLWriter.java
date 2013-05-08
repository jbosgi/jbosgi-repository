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
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.osgi.repository.AttributeValueHandler.AttributeValue;
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
            throw MESSAGES.cannotInitializeRepositoryWriter(ex);
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
            throw MESSAGES.cannotWriteRepositoryElement(ex);
        }
    }

    @Override
    public void writeResource(XResource resource) {
        try {
            writer.writeStartElement(RESOURCE.getLocalName());
            for (Entry<String, Object> entry : resource.getAttributes().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                writer.writeAttribute(key, "" + value);
            }
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
            throw MESSAGES.cannotWriteRepositoryElement(ex);
        }
    }

    @Override
    public void close() {
        try {
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException ex) {
            throw MESSAGES.cannotWriteRepositoryElement(ex);
        }
    }

    private void writeAttributes(Map<String, Object> attributes) throws XMLStreamException {
        for (Entry<String, Object> entry : attributes.entrySet()) {
            AttributeValue attval = AttributeValue.create(entry.getValue());
            writer.writeStartElement(ATTRIBUTE.getLocalName());
            writer.writeAttribute(NAME.getLocalName(), entry.getKey());
            if (attval.isListType()) {
                writer.writeAttribute(VALUE.getLocalName(), attval.getValueString());
                writer.writeAttribute(TYPE.getLocalName(), "List<" + attval.getType() + ">");
            } else {
                writer.writeAttribute(VALUE.getLocalName(), attval.getValueString());
                if (attval.getType() != Type.String) {
                    writer.writeAttribute(TYPE.getLocalName(), attval.getType().toString());
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
