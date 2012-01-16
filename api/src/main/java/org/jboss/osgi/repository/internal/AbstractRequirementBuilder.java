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

import org.jboss.osgi.repository.RequirementBuilder;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResourceBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.osgi.repository.RepositoryConstants.MAVEN_IDENTITY_NAMESPACE;

/**
 * A builder for resource requirements
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class AbstractRequirementBuilder implements RequirementBuilder {

    public static AbstractRequirementBuilder INSTANCE = new AbstractRequirementBuilder();

    // Hide ctor
    private AbstractRequirementBuilder() {

    }

    public XRequirement createArtifactRequirement(String mavenId) {
        XResourceBuilder builder = XResourceBuilder.INSTANCE.createResource();
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        atts.put(MAVEN_IDENTITY_NAMESPACE, mavenId);
        return builder.addGenericRequirement(MAVEN_IDENTITY_NAMESPACE, atts, dirs);
    }

    public XRequirement createArtifactRequirement(String groupId, String artifactId, String version) {
        String mavenId = getMavenIdentifier(groupId, artifactId, version, null, null);
        return createArtifactRequirement(mavenId);
    }

    public XRequirement createArtifactRequirement(String groupId, String artifactId, String version, String type, String classifier) {
        String mavenId = getMavenIdentifier(groupId, artifactId, version, type, classifier);
        return createArtifactRequirement(mavenId);
    }

    private String getMavenIdentifier(String groupId, String artifactId, String version, String type, String classifier) {
        if (groupId == null)
            throw new IllegalArgumentException("Null groupId");
        if (artifactId == null)
            throw new IllegalArgumentException("Null artifactId");
        if (version == null)
            throw new IllegalArgumentException("Null version");
        String mavenId = groupId + ":" + artifactId + ":" + (type != null ? type : "jar") + ":" + version;
        if (classifier != null)
            mavenId += ":" + classifier;
        return mavenId;
    }

}