/*
 * #%L
 * JBossOSGi Repository: TCK Integration
 * %%
 * Copyright (C) 2012 JBoss by Red Hat
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
package org.jboss.test.osgi.repository.tck;

import java.io.ByteArrayInputStream;
import java.util.Hashtable;

import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Integration with the OSGi TCK. It implements an integration protocol as specified by the Repository CT:<p/>
 *
 *   As the Repository spec doesn't specify how the Repository is primed with information, a Repository implementation
 *   must supply an integration bundle which primes the repository with information as expected by this test case.
 *   This process works as follows:<ul>
 *   <li>The test registers a String service which holds the Repository XML that contains the expected content.</li>
 *   <li>The service has the property REPOSITORY_XML_KEY set to the name of this class.</li>
 *   <li>The integration bundle must listen to this service and register one or more Repository service implementations
 *   that serve the information as specified in the XML.</li>
 *   <li>When the integration bundle is finished with its setup it must register a service with the property
 *   REPOSITORY_POPULATED_KEY set to the name of this test class.
 *   The service object or registration class are ignored by the test.</li>
 *   <li>The test waits for this service to appear and runs the tests when it does.</li>
 *   </ul>
 *
 * @author David Bosschaert
 */
public class Activator implements BundleActivator {
    private static final String REPOSITORY_POPULATED_KEY = "repository-populated";
    private static final String TEST_CLASS_NAME = "org.osgi.test.cases.repository.junit.RepositoryTest";

    private ServiceTracker serviceTracker;
    private ServiceRegistration servicePrimedRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        Filter filter = context.createFilter(
                "(&(objectClass=java.lang.String)(repository-xml=" + TEST_CLASS_NAME + "))");
        serviceTracker = new ServiceTracker(context, filter, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object svc = super.addingService(reference);
                if (svc instanceof String) {
                    primeRepository(context, (String) svc);
                }
                return svc;
            }
        };
        serviceTracker.open();
    }

    public void primeRepository(BundleContext context, String xml) {
        synchronized (this) {
            if (servicePrimedRegistration != null)
                // not priming again, already primed
                return;
        }

        ServiceTracker st = new ServiceTracker(context, Repository.class.getName(), null);
        st.open();

        try {
            Repository rep = (Repository) st.waitForService(10000);
            if (rep == null)
                throw new IllegalStateException("Unable to find service: " + RepositoryStorage.class);
            XPersistentRepository xpr = (XPersistentRepository) rep;
            RepositoryStorage rs = xpr.getRepositoryStorage();

            RepositoryReader reader = RepositoryXMLReader.create(new ByteArrayInputStream(xml.getBytes()));
            XResource resource = reader.nextResource();
            while (resource != null) {
                rs.addResource(resource);
                resource = reader.nextResource();
            }

            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put(REPOSITORY_POPULATED_KEY, TEST_CLASS_NAME);
            synchronized (this) {
                servicePrimedRegistration = context.registerService(Object.class.getName(), new Object(), props);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            throw new IllegalStateException(ex);
        } finally {
            st.close();
        }
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        serviceTracker.close();

        if (servicePrimedRegistration != null)
            servicePrimedRegistration.unregister();
    }
}
