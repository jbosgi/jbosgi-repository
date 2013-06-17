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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.jboss.osgi.repository.impl.ExpressionCombinerImpl;
import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.junit.Test;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.service.repository.SimpleRequirementExpression;

/**
 * @author David Bosschaert
 */
public class ExpressionCombinerTestCase {
    @Test
    public void testExpressionCombinerAnd() {
        Requirement req1 = new RequirementBuilderImpl("ns1").build();
        Requirement req2 = new RequirementBuilderImpl("ns2").build();
        ExpressionCombiner ec = new ExpressionCombinerImpl();

        RequirementExpression a = ec.and(req1, req2);
        Assert.assertTrue(a instanceof AndExpression);
        AndExpression ae = (AndExpression) a;
        Assert.assertEquals(2, ae.getRequirements().size());
        List<Requirement> reqs = getSimpleRequirements(ae.getRequirements());
        Assert.assertTrue(reqs.contains(req1));
        Assert.assertTrue(reqs.contains(req2));

        Requirement req3 = new RequirementBuilderImpl("ns3").build();
        RequirementExpression aa = ec.and(a, ec.expression(req3));
        AndExpression ae2 = (AndExpression) aa;
        Assert.assertEquals(2, ae2.getRequirements().size());

        boolean foundSimple = false;
        boolean foundComplex = false;
        for (RequirementExpression re : ae2.getRequirements()) {
            if (re == a) {
                foundComplex = true;
                continue;
            }
            if (re instanceof SimpleRequirementExpression) {
                if (((SimpleRequirementExpression) re).getRequirement() == req3) {
                    foundSimple = true;
                    continue;
                }
            }
            Assert.fail("Not as expected " + re);
        }
        Assert.assertTrue(foundSimple);
        Assert.assertTrue(foundComplex);
    }

    @Test
    public void testExpressionCombinerNot() {
        Requirement req = new RequirementBuilderImpl("ns").build();
        ExpressionCombiner ec = new ExpressionCombinerImpl();

        RequirementExpression e = ec.not(req);
        NotExpression ne = (NotExpression) e;
        SimpleRequirementExpression sr = (SimpleRequirementExpression) ne.getRequirement();
        Assert.assertSame(req, sr.getRequirement());

        RequirementExpression e2 = ec.not(e);
        NotExpression ne2 = (NotExpression) e2;
        Assert.assertSame(e, ne2.getRequirement());
    }

    @Test
    public void testExpressionCombinerOr() {
        Requirement req1 = new RequirementBuilderImpl("ns1").build();
        Requirement req2 = new RequirementBuilderImpl("ns2").build();
        ExpressionCombiner ec = new ExpressionCombinerImpl();

        RequirementExpression a = ec.or(req1, req2);
        OrExpression ae = (OrExpression) a;
        Assert.assertEquals(2, ae.getRequirements().size());
        List<Requirement> reqs = getSimpleRequirements(ae.getRequirements());
        Assert.assertTrue(reqs.contains(req1));
        Assert.assertTrue(reqs.contains(req2));

        Requirement req3 = new RequirementBuilderImpl("ns3").build();
        RequirementExpression aa = ec.or(a, ec.expression(req3));
        OrExpression ae2 = (OrExpression) aa;
        Assert.assertEquals(2, ae2.getRequirements().size());

        boolean foundSimple = false;
        boolean foundComplex = false;
        for (RequirementExpression re : ae2.getRequirements()) {
            if (re == a) {
                foundComplex = true;
                continue;
            }
            if (re instanceof SimpleRequirementExpression) {
                if (((SimpleRequirementExpression) re).getRequirement() == req3) {
                    foundSimple = true;
                    continue;
                }
            }
            Assert.fail("Not as expected " + re);
        }
        Assert.assertTrue(foundSimple);
        Assert.assertTrue(foundComplex);
    }

    private List<Requirement> getSimpleRequirements(List<RequirementExpression> expressions) {
        List<Requirement> l = new ArrayList<Requirement>();
        for (RequirementExpression re : expressions) {
            if (re instanceof SimpleRequirementExpression) {
                l.add(((SimpleRequirementExpression) re).getRequirement());
            } else {
                throw new IllegalArgumentException("Not a simple requirement: " + re);
            }
        }
        return l;
    }
}
