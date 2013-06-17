package org.jboss.osgi.repository.spi;
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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.impl.ExpressionCombinerImpl;
import org.jboss.osgi.repository.impl.RequirementBuilderImpl;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.service.repository.SimpleRequirementExpression;

/**
 * An abstract  {@link XRepository} that does nothing.
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 11-May-2012
 */
public abstract class AbstractRepository implements XRepository {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> reqs) {
        if (reqs == null)
            throw MESSAGES.illegalArgumentNull("reqs");
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement req : reqs) {
            Collection<Capability> providers = findProviders(req);
            result.put(req, providers);
        }
        return result;
    }

    @Override
    public abstract Collection<Capability> findProviders(Requirement req);

    @Override
    public Collection<Resource> findProviders(RequirementExpression re) {
        if (re == null)
            throw MESSAGES.illegalArgumentNull("re");

        if (re instanceof SimpleRequirementExpression) {
            return findSimpleRequirementExpression((SimpleRequirementExpression) re);
        } else if (re instanceof AndExpression) {
            return findAndExpression((AndExpression) re);
        } else if (re instanceof OrExpression) {
            return findOrExpression((OrExpression) re);
        } else if (re instanceof NotExpression) {
            return findNotExpression((NotExpression) re);
        }

        throw MESSAGES.malformedRequirementExpression(re);
    }

    private Collection<Resource> findSimpleRequirementExpression(SimpleRequirementExpression re) {
        Requirement req = re.getRequirement();
        return findSimpleRequirement(req);
    }

    private Collection<Resource> findSimpleRequirement(Requirement req) {
        Collection<Capability> capabilities = findProviders(req);

        List<Resource> resources = new ArrayList<Resource>();
        for (Capability cap : capabilities) {
            Resource res = cap.getResource();
            if (res != null) {
                resources.add(res);
            }
        }
        return resources;
    }

    private Collection<Resource> findAndExpression(AndExpression re) {
        List<RequirementExpression> reqs = re.getRequirements();
        if (reqs.size() == 0)
            return Collections.emptyList();

        List<Resource> l = null;
        List<NotExpression> notExpressions = new ArrayList<NotExpression>();
        for (RequirementExpression req : reqs) {
            if (req instanceof NotExpression) {
                notExpressions.add((NotExpression) req);
                continue;
            }
            if (l == null) {
                // first condition
                l = new ArrayList<Resource>(findProviders(req));
            } else {
                l.retainAll(findProviders(req));
            }
        }

        // Handle the not expressions
        for (NotExpression req : notExpressions) {
            NotExpression ne = req;
            l.removeAll(findProviders(ne.getRequirement()));
        }

        return l;
    }

    private Collection<Resource> findOrExpression(OrExpression re) {
        Set<Resource> l = new HashSet<Resource>();
        for (RequirementExpression req : re.getRequirements()) {
            l.addAll(findProviders(req));
        }
        return l;
    }

    private Collection<Resource> findNotExpression(NotExpression ne) {
        RequirementExpression re = ne.getRequirement();
        if (re instanceof SimpleRequirementExpression) {
            Requirement req = ((SimpleRequirementExpression) re).getRequirement();
            Requirement nreq = negateRequirement(req);
            return findSimpleRequirement(nreq);
        } else if (re instanceof NotExpression) {
            return findProviders(((NotExpression) re).getRequirement());
        } else if (re instanceof AndExpression) {
            return findInverse(re);
        } else if (re instanceof OrExpression) {
            return findInverse(re);
        }
        throw new UnsupportedOperationException();
    }

    private Collection<Resource> findInverse(RequirementExpression re) {
        Collection<Resource> andProviders = findProviders(re);

        Requirement matchAll = newRequirementBuilder("osgi.identity").build();
        Collection<Resource> allResources = findSimpleRequirement(matchAll);

        // TODO would be better if this filtering was done lazily
        Collection<Resource> result = new ArrayList<Resource>();
        for (Resource res : allResources) {
            if (andProviders.contains(res))
                continue;
            result.add(res);
        }
        return result;
    }

    private Requirement negateRequirement(Requirement req) {
        String filter = req.getDirectives().get("filter");
        if (filter == null) {
            throw new IllegalStateException("No filter directive: " + req);
        }
        String invFilter = "(!" + filter + ")";
        return newRequirementBuilder(req.getNamespace()).
            setAttributes(req.getAttributes()).
            setDirectives(req.getDirectives()).
            addDirective("filter", invFilter).
            build();
    }

    @Override
    public ExpressionCombiner getExpressionCombiner() {
        return new ExpressionCombinerImpl();
    }

    @Override
    public RequirementBuilder newRequirementBuilder(String namespace) {
        return new RequirementBuilderImpl(namespace);
    }
}