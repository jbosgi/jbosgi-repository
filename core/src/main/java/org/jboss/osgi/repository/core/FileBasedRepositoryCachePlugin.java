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
package org.jboss.osgi.repository.core;

import org.jboss.logging.Logger;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.spi.AbstractRepositoryCachePlugin;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jboss.osgi.resolver.XResourceConstants.CONTENT_PATH;
import static org.jboss.osgi.resolver.XResourceConstants.MAVEN_IDENTITY_NAMESPACE;


/**
 * A simple {@link org.jboss.osgi.repository.RepositoryCachePlugin} that uses
 * the local file system.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class FileBasedRepositoryCachePlugin extends AbstractRepositoryCachePlugin {

    private static Logger log = Logger.getLogger(FileBasedRepositoryCachePlugin.class);

    private final File repository;

    public FileBasedRepositoryCachePlugin(File repository) {
        this.repository = repository;
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {

        String namespace = req.getNamespace();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace) == false)
            return Collections.emptySet();

        String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
        MavenCoordinates coordinates = MavenCoordinates.parse(mavenId);
        try {
            List<Capability> result = new ArrayList<Capability>();
            URL baseURL = repository.toURI().toURL();
            URL url = coordinates.toArtifactURL(baseURL);
            if (new File(url.getPath()).exists()) {
                String contentPath = url.toExternalForm();
                contentPath = contentPath.substring(baseURL.toExternalForm().length());
                XResource resource = XResourceBuilder.create(baseURL, contentPath).getResource();
                result.add(resource.getIdentityCapability());
            }
            return Collections.unmodifiableList(result);
        } catch (Exception ex) {
            throw new RepositoryResolutionException(ex);
        }
    }

    @Override
    public Collection<Capability> storeCapabilities(Collection<Capability> caps) throws RepositoryStorageException {
        List<Capability> result = new ArrayList<Capability>(caps.size());
        for (Capability cap : caps) {
            XIdentityCapability icap = (XIdentityCapability) cap;
            String contentPath = (String) icap.getAttribute(CONTENT_PATH);
            if (contentPath != null) {
                File contentFile = new File(repository.getAbsolutePath() + File.separator + contentPath);
                try {
                    copyResourceContent(cap.getResource(), contentFile);
                    XIdentityCapability newid = recreateIdentity(contentFile);
                    result.add(newid);
                } catch (IOException ex) {
                    new RepositoryStorageException(ex);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private XIdentityCapability recreateIdentity(File contentFile) throws IOException {
        URL baseURL = repository.toURI().toURL();
        String contentPath = contentFile.getPath().substring(repository.getAbsolutePath().length() + 1);
        XResourceBuilder builder = XResourceBuilder.create(baseURL, contentPath);
        return builder.getResource().getIdentityCapability();
    }

    private void copyResourceContent(Resource resource, File contentFile) throws IOException {
        int len = 0;
        byte[] buf = new byte[4096];
        contentFile.getParentFile().mkdirs();
        InputStream in = ((XResource)resource).getContent();
        OutputStream out = new FileOutputStream(contentFile);
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
