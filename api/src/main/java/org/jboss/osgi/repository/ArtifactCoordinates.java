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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The artifact coordinates.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class ArtifactCoordinates {

    private final String groupId;
    private final String artifactId;
    private final String type;
    private final String version;
    private final String classifier;

    public static ArtifactCoordinates parse(String coordinates) {
        ArtifactCoordinates result;
        String[] parts = coordinates.split(":");
        if (parts.length == 3) {
            result = new ArtifactCoordinates(parts[0], parts[1], null, parts[2], null);
        } else if (parts.length == 4) {
            result = new ArtifactCoordinates(parts[0], parts[1], parts[2], parts[3], null);
        } else if (parts.length == 5) {
            result = new ArtifactCoordinates(parts[0], parts[1], parts[2], parts[3], parts[4]);
        } else {
            throw new IllegalArgumentException("Invalid coordinates: " + coordinates);
        }
        return result;
    }

    public ArtifactCoordinates create(String groupId, String artifactId, String version, String type, String classifier) {
        return new ArtifactCoordinates(groupId, artifactId, type, version, classifier);
    }

    private ArtifactCoordinates(String groupId, String artifactId, String type, String version, String classifier) {
        if (groupId == null)
            throw new IllegalArgumentException("Null groupId");
        if (artifactId == null)
            throw new IllegalArgumentException("Null artifactId");
        if (version == null)
            throw new IllegalArgumentException("Null version");

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = (type != null ? type : "jar");
        this.version = version;
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String toExternalForm() {
        String clstr = classifier != null ? ":" + classifier : "";
        return groupId + ":" + artifactId + ":" + type + ":" + version + clstr;
    }

    public URL toArtifactURL(URL baseURL) {
        String base = baseURL.toExternalForm();
        if (base.endsWith("/") == false)
            base += "/";
        String dirstr = base + groupId.replace('.', '/') + "/" + artifactId + "/" + version;
        String clstr = classifier != null ? "-" + classifier : "";
        String urlstr = dirstr + "/" + artifactId + "-" + version + clstr + "." + type;
        try {
            return new URL(urlstr);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid artifact URL: " + urlstr);
        }
    }

    @Override
    public String toString() {
        return "ArtifactCoordinates[" + toExternalForm() + "]";
    }
}