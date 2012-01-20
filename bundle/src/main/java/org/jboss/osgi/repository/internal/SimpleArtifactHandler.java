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
package org.jboss.osgi.repository.internal;

import org.jboss.logging.Logger;
import org.jboss.osgi.repository.ArtifactCoordinates;
import org.jboss.osgi.repository.ArtifactProviderPlugin;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.resolver.v2.XResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jboss.osgi.repository.RepositoryConstants.MAVEN_IDENTITY_NAMESPACE;

/**
 * A simple {@link org.jboss.osgi.repository.ArtifactProviderPlugin} that uses .
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
class SimpleArtifactHandler implements ArtifactProviderPlugin {

    private static Logger log = Logger.getLogger(SimpleArtifactHandler.class);

    private static String JBOSS_NEXUS_BASE = "http://repository.jboss.org/nexus/content/groups/public";
    private static String MAVEN_CENTRAL_BASE = "http://repo1.maven.org/maven2";

    private final BundleContext context;
    private final URL[] baserepos;

    SimpleArtifactHandler(BundleContext context) {
        this.context = context;

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
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        if (MAVEN_IDENTITY_NAMESPACE.equals(namespace)) {
            String mavenId = (String) req.getAttributes().get(MAVEN_IDENTITY_NAMESPACE);
            ArtifactCoordinates coordinates = ArtifactCoordinates.parse(mavenId);
            try {
                List<URL> urls = new ArrayList<URL>();
                for (URL baseURL : baserepos) {
                    URL url = coordinates.toArtifactURL(baseURL);
                    try {
                        url.openStream().close();
                        urls.add(url);
                        break;
                    } catch (IOException e) {
                        // ignore
                    }
                }
                AbstractResourceBuilder builder = AbstractResourceBuilder.INSTANCE;
                Collection<Capability> result = new ArrayList<Capability>();
                for (URL url : urls) {
                    XResource xres = builder.createResource(url);
                    result.add(xres.getIdentityCapability());
                }
                return result;
            } catch (Exception ex) {
                throw new RepositoryResolutionException(ex);
            }
        } else {
            throw new RepositoryResolutionException("Unsupported namespace: " + namespace);
        }
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
