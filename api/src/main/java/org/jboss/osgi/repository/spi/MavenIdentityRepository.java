/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.repository.RepositoryLogger.LOGGER;
import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.spi.AbstractRequirement;
import org.osgi.framework.Filter;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * A simple {@link XRepository} that delegates to a maven repositories.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class MavenIdentityRepository extends AbstractRepository implements XRepository {

    private final URL[] baserepos;

    /** The configuration for the {@link MavenIdentityRepository} */
    public interface Configuration {

        /** The default JBoss Nexus repository: http://repository.jboss.org/nexus/content/groups/public */
        String JBOSS_NEXUS_BASE = "http://repository.jboss.org/nexus/content/groups/public";

        /** The default Maven Central repository: http://repo1.maven.org/maven2 */
        String MAVEN_CENTRAL_BASE = "http://repo1.maven.org/maven2";

        /** Get the list of configured base URLs */
        List<URL> getBaseURLs();
    }

    /** A configuration property provider */
    public interface ConfigurationPropertyProvider {

        /** Get the list of configured base URLs */
        String getProperty(String key, String defaultValue);
    }

    public MavenIdentityRepository() {
        this(new ConfigurationPropertyProvider() {
            @Override
            public String getProperty(String key, String defaultValue) {
                return SecurityActions.getSystemProperty(key, defaultValue);
            }
        });
    }

    public MavenIdentityRepository(ConfigurationPropertyProvider provider) {
        this(getDefaultConfiguration(provider));
    }

    public MavenIdentityRepository(Configuration configuration) {
        List<URL> repos = new ArrayList<URL>();
        for (URL baseURL : configuration.getBaseURLs()) {
            repos.add(baseURL);
        }
        baserepos = repos.toArray(new URL[repos.size()]);
    }

    /**
     * Get the default configuration which delegates to
     *
     * #1 The local maven repository at ~/.m2/repository
     * #2 The default JBoss Nexus repository
     * #3 The default Maven Central repository
     */
    public static Configuration getDefaultConfiguration(final ConfigurationPropertyProvider provider) {
        return new Configuration() {
            @Override
            public List<URL> getBaseURLs() {
                List<URL> result = new ArrayList<URL>();
                String property = provider.getProperty(PROPERTY_MAVEN_REPOSITORY_BASE_URLS, null);
                if (property == null) {
                    property = "";
                    String userhome = SecurityActions.getSystemProperty("user.home", "");
                    File localrepo = new File(userhome + File.separator + ".m2" + File.separator + "repository");
                    if (localrepo.isDirectory()) {
                        property += localrepo.toURI().toString() + ",";
                    }
                    property += JBOSS_NEXUS_BASE + ",";
                    property += MAVEN_CENTRAL_BASE;
                }
                for (String urlspec : property.split(",")) {
                    result.add(getBaseURL(urlspec));
                }
                return Collections.unmodifiableList(result);
            }
        };
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {

        String namespace = req.getNamespace();
        if (!XResource.MAVEN_IDENTITY_NAMESPACE.equals(namespace))
            return Collections.emptyList();

        MavenCoordinates mavenId;
        if (req.getDirectives().get("filter") != null) {
            Filter filter = ((XRequirement)req).getFilter();
            String gpart = AbstractRequirement.getValueFromFilter(filter, "groupId", null);
            String apart = AbstractRequirement.getValueFromFilter(filter, "artifactId", null);
            String tpart = AbstractRequirement.getValueFromFilter(filter, "type", null);
            String vpart = AbstractRequirement.getValueFromFilter(filter, "version", null);
            String cpart = AbstractRequirement.getValueFromFilter(filter, "classifier", null);
            mavenId = MavenCoordinates.create(gpart, apart, vpart, tpart, cpart);
        } else {
            String nsvalue = (String) req.getAttributes().get(XResource.MAVEN_IDENTITY_NAMESPACE);
            mavenId = MavenCoordinates.parse(nsvalue);
        }

        LOGGER.infoFindMavenProviders(mavenId);

        URL contentURL = null;
        for (URL baseURL : baserepos) {
            URL url = mavenId.getArtifactURL(baseURL);
            try {
                url.openStream().close();
                contentURL = url;
                break;
            } catch (IOException e) {
                LOGGER.debugf("Cannot access input stream for: %s", url);
            }
        }

        List<Capability> result = new ArrayList<Capability>();
        if (contentURL != null) {
            try {
                XResourceBuilder<XResource> builder = URLResourceBuilderFactory.create(contentURL, null);
                builder.addIdentityCapability(mavenId);
                XResource resource = builder.getResource();
                LOGGER.infoFoundMavenResource(resource);

                // Convert the resource to the given target type
                resource = getTargetResource((XRequirement) req, resource);
                if (resource != null) {
                    result.add(resource.getIdentityCapability());
                }

            } catch (Exception ex) {
                LOGGER.errorCannotCreateResource(ex, mavenId);
            }
        }

        return Collections.unmodifiableList(result);
    }

    private static URL getBaseURL(String urlspec) {
        try {
            return new URL(urlspec);
        } catch (MalformedURLException e) {
            throw MESSAGES.invalidRepositoryBase(urlspec);
        }
    }
}
