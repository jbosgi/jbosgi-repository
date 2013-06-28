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
package org.jboss.osgi.repository.impl;

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
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.service.repository.SimpleRequirementExpression;

public class ExpressionCombinerImpl implements ExpressionCombiner {
    @Override
    public RequirementExpression and(final Requirement... reqs) {
        return new AndExpression() {
            @Override
            public List<RequirementExpression> getRequirements() {
                List<RequirementExpression> l = new ArrayList<RequirementExpression>();
                for (Requirement req : reqs) {
                    l.add(expression(req));
                }
                return l;
            }

            @Override
            public String toString() {
                return formatRequirements("and", getRequirements());
            }
        };
    }

    @Override
    public RequirementExpression and(final RequirementExpression... reqs) {
        return new AndExpression() {
            @Override
            public List<RequirementExpression> getRequirements() {
                return Arrays.asList(reqs);
            }

            @Override
            public String toString() {
                return formatRequirements("and", getRequirements());
            }
        };
    }

    @Override
    public RequirementExpression expression(final Requirement req) {
        return new SimpleRequirementExpression() {
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
    public RequirementExpression not(final Requirement req) {
        return new NotExpression() {
            @Override
            public RequirementExpression getRequirement() {
                return expression(req);
            }

            @Override
            public String toString() {
                return "not(" + getRequirement() + ")";
            }
        };
    }

    @Override
    public RequirementExpression not(final RequirementExpression req) {
        return new NotExpression() {
            @Override
            public RequirementExpression getRequirement() {
                return req;
            }

            @Override
            public String toString() {
                return "not(" + getRequirement() + ")";
            }
        };
    }

    @Override
    public RequirementExpression or(final Requirement... reqs) {
        return new OrExpression() {
            @Override
            public List<RequirementExpression> getRequirements() {
                List<RequirementExpression> l = new ArrayList<RequirementExpression>();
                for (Requirement req : reqs) {
                    l.add(expression(req));
                }
                return l;
            }

            @Override
            public String toString() {
                return formatRequirements("or", getRequirements());
            }
        };
    }

    @Override
    public RequirementExpression or(final RequirementExpression... reqs) {
        return new OrExpression() {
            @Override
            public List<RequirementExpression> getRequirements() {
                return Arrays.asList(reqs);
            }

            @Override
            public String toString() {
                return formatRequirements("or", getRequirements());
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
