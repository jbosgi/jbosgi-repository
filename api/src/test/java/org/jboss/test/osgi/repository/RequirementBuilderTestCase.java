/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RequirementBuilder;

/**
 * @author David Bosschaert
 */
public class RequirementBuilderTestCase {
    @Test
    public void testEmptyRequirement() {
        RequirementBuilder rb = new RequirementBuilderImpl("org.foo.bar");
        Requirement req = rb.build();

        Assert.assertEquals("org.foo.bar", req.getNamespace());
        Assert.assertEquals(null, req.getResource());
        Assert.assertEquals(0, req.getAttributes().size());
        Assert.assertEquals(0, req.getDirectives().size());
    }

    @Test
    public void testFullRequirement() {
        Resource res = Mockito.mock(Resource.class);
        RequirementBuilder rb = new RequirementBuilderImpl("test").
                setResource(res).
                addAttribute("a1", "v1").
                addAttribute("a2", 42l).
                addDirective("d", "dir");

        Requirement req = rb.build();
        Assert.assertEquals("test", req.getNamespace());
        Assert.assertSame(res, req.getResource());
        Assert.assertEquals(1, req.getDirectives().size());
        Assert.assertEquals("dir", req.getDirectives().get("d"));
        Assert.assertEquals(2, req.getAttributes().size());
        Assert.assertEquals("v1", req.getAttributes().get("a1"));
        Assert.assertEquals(42l, req.getAttributes().get("a2"));
    }

    @Test
    public void testCombination() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("a", "A");
        Map<String, String> dirs = new HashMap<String, String>();
        dirs.put("d", "D");

        RequirementBuilder rb = new RequirementBuilderImpl("test").
                setAttributes(Collections.unmodifiableMap(attrs)).
                setDirectives(Collections.unmodifiableMap(dirs)).
                addAttribute("b", "B").
                addDirective("d", "DDD");

        Requirement req = rb.build();
        Assert.assertEquals(2, req.getAttributes().size());
        Assert.assertEquals("A", req.getAttributes().get("a"));
        Assert.assertEquals("B", req.getAttributes().get("b"));
        Assert.assertEquals(1, req.getDirectives().size());
        Assert.assertEquals("DDD", req.getDirectives().get("d"));


    }
}
