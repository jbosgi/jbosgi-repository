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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryWriter;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.RepositoryXMLWriter;
import org.jboss.osgi.resolver.XResource;
import org.junit.Test;

/**
 * Test the the {@link RepositoryXMLWriter}.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-May-2012
 */
public class RepositoryWriterTestCase extends AbstractRepositoryTest {

    @Test
    public void testSampleRepositoryXML() throws Exception {

        RepositoryReader reader = getRepositoryReader("xml/sample-repository.xml");
        Map<String, String> attributes = reader.getRepositoryAttributes();
        List<XResource> resources = getResources(reader);

        File file = new File("target/repository.xml");
        RepositoryWriter writer = RepositoryXMLWriter.create(new FileOutputStream(file));
        writer.writeRepositoryAttributes(attributes);
        for (XResource res : resources) {
            writer.writeResource(res);
        }
        writer.close();

        reader = RepositoryXMLReader.create(new FileInputStream(file));
        RepositoryReaderTestCase.verifyContent(reader.getRepositoryAttributes(), getResources(reader));
    }

}