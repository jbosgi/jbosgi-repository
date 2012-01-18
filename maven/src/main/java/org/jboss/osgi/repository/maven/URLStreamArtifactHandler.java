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
package org.jboss.osgi.repository.maven;

import org.jboss.osgi.repository.ArtifactCoordinates;
import org.jboss.osgi.repository.ArtifactHandler;
import org.osgi.framework.BundleContext;

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
class URLStreamArtifactHandler implements ArtifactHandler {

    private static String JBOSS_NEXUS_BASE = "http://repository.jboss.org/nexus/content/groups/public";
    private static String MAVEN_CENTRAL_BASE = "http://repo1.maven.org/maven2";

    private final BundleContext context;
    private final URL[] baserepos;

    URLStreamArtifactHandler(BundleContext context) {
        this.context = context;
        baserepos = new URL[]{getBaseURL(JBOSS_NEXUS_BASE), getBaseURL(MAVEN_CENTRAL_BASE)};
    }

    @Override
    public URL[] resolveArtifacts(ArtifactCoordinates coordinates) {
        List<URL> result = new ArrayList<URL>();
        for (URL baseURL : baserepos) {
            URL url = coordinates.toArtifactURL(baseURL);
            try {
                url.openStream().close();
                result.add(url);
            } catch (IOException e) {
                // ignore
            }
        }
        return result.toArray(new URL[result.size()]);
    }

    @Override
    public void storeArtifacts(ArtifactCoordinates coordinates, URL[] urls) {
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
