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
package org.jboss.test.osgi.repository;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.AbstractRepository;
import org.jboss.osgi.repository.internal.ExpressionCombinerImpl;
import org.jboss.osgi.repository.internal.RequirementBuilderImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;

/**
 * Test the {@link Repository} API
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 28-Jun-2013
 */
public class RepositoryTestCase extends AbstractRepositoryTest {

    private XRepository repository;

    @Before
    public void setUp() throws IOException {
        repository = new AbstractRepository() {
            public Collection<Capability> findProviders(Requirement req) {
                return Collections.emptyList();
            }
        };
    }

    @Test
    public void testGetRequirementBuilder() {
        RequirementBuilder builder = repository.newRequirementBuilder("toastie");
        Assert.assertTrue(builder instanceof RequirementBuilderImpl);
        Requirement req = builder.build();
        Assert.assertEquals("toastie", req.getNamespace());
    }

    @Test
    public void testGetExpressionCombiner() {
        Assert.assertTrue(repository.getExpressionCombiner() instanceof ExpressionCombiner);
        Assert.assertTrue(repository.getExpressionCombiner() instanceof ExpressionCombinerImpl);
    }
}