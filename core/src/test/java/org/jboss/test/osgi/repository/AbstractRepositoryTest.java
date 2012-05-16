/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.test.osgi.repository;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.osgi.repository.RepositoryProcessor;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.core.RepositoryXMLReaderImpl;
import org.jboss.osgi.repository.spi.AbstractRepositoryProcessor;
import org.jboss.osgi.resolver.XResource;

/**
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public abstract class AbstractRepositoryTest {

    protected List<XResource> getResources(String xmlres) throws XMLStreamException {
        final List<XResource> resources = new ArrayList<XResource>();
        RepositoryProcessor processor = new AbstractRepositoryProcessor() {
            @Override
            public boolean addResource(XResource res) {
                resources.add(res);
                return true;
            }
        };
        RepositoryXMLReader reader = new RepositoryXMLReaderImpl();
        InputStream input = getClass().getClassLoader().getResourceAsStream(xmlres);
        reader.parse(input, processor);
        return resources;
    }

    protected void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File aux : file.listFiles())
                deleteRecursive(aux);
        }
        file.delete();
    }

}