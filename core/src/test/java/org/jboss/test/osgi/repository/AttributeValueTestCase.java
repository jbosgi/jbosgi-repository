package org.jboss.test.osgi.repository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.jboss.osgi.repository.AttributeValueHandler;
import org.jboss.osgi.repository.AttributeValueHandler.AttributeValue;
import org.jboss.osgi.repository.Namespace100.Type;
import org.junit.Test;

/**
 * Test the {@link AttributeValueHandler}.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class AttributeValueTestCase extends AbstractRepositoryTest {

    @Test
    public void testSimpleParsing() throws Exception {
        // String 
        AttributeValue value = AttributeValueHandler.readAttributeValue(null, "a");
        Assert.assertEquals("a", value.getValue());
        Assert.assertEquals("a", value.getValueString());
        Assert.assertEquals(Type.String, value.getType());
        Assert.assertFalse(value.isListType());
        Assert.assertEquals("[type=String, value=a]", value.toExternalForm());
        Assert.assertEquals(AttributeValue.parse("[type=String, value=a]"), value);
        Assert.assertEquals(AttributeValue.create("a"), value);

        // List<String> [a, b]
        value = AttributeValueHandler.readAttributeValue("List<String>", "a,b");
        Assert.assertEquals(Arrays.asList("a","b"), value.getValue());
        Assert.assertEquals("a, b", value.getValueString());
        Assert.assertEquals(Type.String, value.getType());
        Assert.assertTrue(value.isListType());
        Assert.assertEquals("[type=List<String>, value=a, b]", value.toExternalForm());
        Assert.assertEquals(AttributeValue.parse("[type=List<String>, value=a, b]"), value);
        Assert.assertEquals(AttributeValue.create(Arrays.asList("a","b")), value);
    }

    @Test
    public void testCommaValueParsing() throws Exception {
        // List<String> [a,b, c,d]
        AttributeValue value = AttributeValueHandler.readAttributeValue("List<String>", "a\\,b,c\\,d");
        Assert.assertEquals(Arrays.asList("a,b","c,d"), value.getValue());
        Assert.assertEquals(value, AttributeValueHandler.readAttributeValue("List<String>", value.getValueString()));
        Assert.assertEquals(value, AttributeValue.create(Arrays.asList("a,b","c,d")));

        // List<String> [a\b, c\d]
        value = AttributeValueHandler.readAttributeValue("List<String>", "a\\b,c\\d");
        Assert.assertEquals(Arrays.asList("a\\b","c\\d"), value.getValue());
        Assert.assertEquals(value, AttributeValueHandler.readAttributeValue("List<String>", value.getValueString()));
        Assert.assertEquals(value, AttributeValue.create(Arrays.asList("a\\b","c\\d")));
    }

    @Test
    public void testLongParsing() throws Exception {
        // Long 100
        AttributeValue value = AttributeValueHandler.readAttributeValue("Long", "100");
        Assert.assertEquals(new Long(100),  value.getValue());
        Assert.assertEquals("100",  value.getValueString());

        // List<Long> [100, 200]
        value = AttributeValueHandler.readAttributeValue("List<Long>", "100, 200");
        Assert.assertEquals(Arrays.asList(new Long(100), new Long(200)), value.getValue());
        Assert.assertEquals(value, AttributeValueHandler.readAttributeValue("List<Long>", value.getValueString()));
        Assert.assertEquals(value, AttributeValue.create(Arrays.asList(new Long(100), new Long(200))));
        
        // List<Object> [100, 200]
        List<Object> listA = Arrays.asList((Object)new Long(100), (Object)new Long(200));
        Assert.assertEquals(value, AttributeValue.create(listA));
        
        // List<Long> (empty)
        value = AttributeValueHandler.readAttributeValue("List<Long>", "");
        Assert.assertEquals(Arrays.asList(), value.getValue());
        Assert.assertEquals(value, AttributeValueHandler.readAttributeValue("List<Long>", value.getValueString()));
        
        // Note, the component type of an empty list cannot by determined
        value = AttributeValue.create(new ArrayList<Long>());
        Assert.assertEquals(Collections.emptyList(),  value.getValue());
        Assert.assertEquals("",  value.getValueString());
        Assert.assertEquals(Type.String,  value.getType());
        Assert.assertTrue(value.isListType());
        Assert.assertEquals("[type=List<String>, value=]",  value.toExternalForm());
    }
}