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
package org.jboss.osgi.repository.internal;

/**
 * @author David Bosschaert
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.osgi.resource.Requirement;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementExpression;

public class ExpressionCombinerImpl implements ExpressionCombiner {
    @Override
    public AndExpression and(RequirementExpression expr1, RequirementExpression expr2) {
        return and(expr1, expr2, new RequirementExpression[] {});
    }

    @Override
    public AndExpression and(final RequirementExpression expr1, final RequirementExpression expr2, final RequirementExpression... moreExprs) {
        return new AndExpression() {
            @Override
            public List<RequirementExpression> getRequirementExpressions() {
                List<RequirementExpression> l = new ArrayList<RequirementExpression>();
                l.add(expr1);
                l.add(expr2);
                l.addAll(Arrays.asList(moreExprs));
                return l;
            }

            @Override
            public String toString() {
                return formatRequirements("and", getRequirementExpressions());
            }
        };
    }

    @Override
    public IdentityExpression identity(final Requirement req) {
        return new IdentityExpression() {
            @Override
            public Requirement getRequirement() {
                return req;
            }

            @Override
            public String toString() {
                return getRequirement().toString();
            }
        };
    }

    @Override
    public NotExpression not(final RequirementExpression req) {
        return new NotExpression() {
            @Override
            public RequirementExpression getRequirementExpression() {
                return req;
            }

            @Override
            public String toString() {
                return "not(" + getRequirementExpression() + ")";
            }
        };
    }

    @Override
    public OrExpression or(RequirementExpression expr1, RequirementExpression expr2) {
        return or(expr1, expr2, new RequirementExpression[] {});
    }

    @Override
    public OrExpression or(final RequirementExpression expr1, final RequirementExpression expr2, final RequirementExpression... moreExprs) {
        return new OrExpression() {
            @Override
            public List<RequirementExpression> getRequirementExpressions() {
                List<RequirementExpression> l = new ArrayList<RequirementExpression>();
                l.add(expr1);
                l.add(expr2);
                l.addAll(Arrays.asList(moreExprs));
                return l;
            }

            @Override
            public String toString() {
                return formatRequirements("or", getRequirementExpressions());
            }
        };
    }

    private String formatRequirements(String operator, Collection<RequirementExpression> reqs) {
        StringBuilder sb = new StringBuilder("(");
        for (RequirementExpression re : reqs) {
            if (sb.length() > 1)
                sb.append(" " + operator + " ");

            sb.append(re.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
