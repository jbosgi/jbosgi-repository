package org.jboss.osgi.repository.internal;
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

import org.jboss.osgi.repository.core.XRepositoryBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.repository.Repository;

/**
 * An activator for {@link Repository} services.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class RepositoryActivator implements BundleActivator {

    private XRepositoryBuilder builder;

    @Override
    public void start(final BundleContext context) throws Exception {
        builder = XRepositoryBuilder.create(context);
        builder.addDefaultRepositoryStorage(context.getDataFile("repository"));
        builder.addDefaultRepositories();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        builder.unregisterServices();
    }
}