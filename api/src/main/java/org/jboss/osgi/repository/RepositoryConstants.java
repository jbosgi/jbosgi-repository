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

/**
 * Defines names for the attributes, directives and name spaces for
 * resources, capabilities and requirements in the context of the
 * JBoss Repository.
 * <p/>
 * <p/>
 * The values associated with these keys are of type {@code String}, unless
 * otherwise indicated.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public final class RepositoryConstants {

    // Hide ctor
    private RepositoryConstants() {
    }

    /**
     * Artifact coordinates may be given in simple groupId:artifactId:version form,
     * or they may be fully qualified in the form groupId:artifactId:type:version[:classifier]
     */
    public static final String MAVEN_IDENTITY_NAMESPACE = "maven.identity";

    /**
     * Artifact coordinates may be given by {@link org.jboss.modules.ModuleIdentifier}
     */
    public static final String MODULE_IDENTITY_NAMESPACE = "module.identity";

    /**
     * An attribute on the identity capability that represents the location of the resource.
     */
    public static final String CONTENT_URL = "content.url";

    /**
     * An attribute on the identity capability that represents the location of the resource
     * relative to the base url of the repository.
     */
    public static final String CONTENT_PATH = "content.path";
}
