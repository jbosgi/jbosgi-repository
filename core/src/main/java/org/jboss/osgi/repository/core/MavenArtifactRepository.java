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

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.resolver.XResourceConstants.MAVEN_IDENTITY_NAMESPACE;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;


/**
 * A simple {@link XRepository} that delegates to a maven repositories.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class MavenArtifactRepository extends AbstractRepository implements XRepository {

    private static String JBOSS_NEXUS_BASE = "http://repository.jboss.org/nexus/content/groups/public";
    private static String MAVEN_CENTRAL_BASE = "http://repo1.maven.org/maven2";

    private final URL[] baserepos;

    public MavenArtifactRepository() {
        List<URL> repos = new ArrayList<URL>();
        String userhome = System.getProperty("user.home");
        File localrepo = new File(userhome + File.separator + ".m2" + File.separator + "repository");
        if (localrepo.isDirectory()) {
            repos.add(getBaseURL(localrepo.toURI().toString()));
        }
        repos.add(getBaseURL(JBOSS_NEXUS_BASE));
        repos.add(getBaseURL(MAVEN_CENTRAL_BASE));
        baserepos = repos.toArray(new URL[repos.size()]);
    }

    @Override
    public String getName() {
        return "Maven Artifact Repository";
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        List<Capability> result = new ArrayList<Capability>();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace)) {
            String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
            MavenCoordinates coordinates = MavenCoordinates.parse(mavenId);
            LOGGER.infoFindMavenProviders(coordinates);
            for (URL baseURL : baserepos) {
                URL url = coordinates.getArtifactURL(baseURL);
                try {
                    url.openStream().close();
                } catch (IOException e) {
                    LOGGER.errorCannotOpenInputStream(url);
                    continue;
                }
                try {
                    String contentPath = url.toExternalForm();
                    contentPath = contentPath.substring(baseURL.toExternalForm().length());
                    XResourceBuilder builder = URLResourceBuilderFactory.create(baseURL, contentPath, null, true);
                    XResource resource = builder.getResource();
                    result.add(resource.getIdentityCapability());
                    LOGGER.infoFoundMavenResource(resource);
                    break;
                } catch (Exception ex) {
                    LOGGER.resolutionCannotCreateResource(ex, coordinates);
                }
            }
        }
        return result;
    }

    private URL getBaseURL(String basestr) {
        URL baseURL = null;
        try {
            baseURL = new URL(basestr);
        } catch (MalformedURLException e) {
            // ignore
        }
        return baseURL;
    }
}