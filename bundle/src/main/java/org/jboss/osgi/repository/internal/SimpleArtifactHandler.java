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
import org.jboss.osgi.repository.ArtifactHandler;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link ArtifactHandler} that uses .
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
class SimpleArtifactHandler implements ArtifactHandler {

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
    public URL[] resolveArtifacts(ArtifactCoordinates coordinates) {
        URL[] result = new URL[0];
        for (URL baseURL : baserepos) {
            URL url = coordinates.toArtifactURL(baseURL);
            try {
                url.openStream().close();
                result = new URL[]{url};
                break;
            } catch (IOException e) {
                // ignore
            }
        }
        log.infof("resolved %s -> %s", coordinates, result);
        return result;
    }

    @Override
    public URL[] storeArtifacts(ArtifactCoordinates coordinates, URL[] urls) {
        return urls;
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
