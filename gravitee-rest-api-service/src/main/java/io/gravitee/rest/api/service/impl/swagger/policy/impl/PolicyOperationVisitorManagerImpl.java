/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.swagger.policy.impl;

import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.visitor.v2.SwaggerOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.policy.api.swagger.Policy;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyOperationVisitorManagerImpl implements PolicyOperationVisitorManager {

    private final List<PolicyOperationVisitor> policyVisitors = new ArrayList<>();

    private final Map<String, SwaggerOperationVisitor> swaggerOperationVisitors = new HashMap<>();
    private final Map<String, OAIOperationVisitor> oaiOperationVisitors = new HashMap<>();

    @Override
    public void add(PolicyOperationVisitor visitor) {
        policyVisitors.add(visitor);
        if (visitor.getOaiOperationVisitor() != null) {
            oaiOperationVisitors.put(visitor.getId(), (OAIOperationVisitor<Optional<Policy>>) (descriptor, operation) ->
                    visitor.getOaiOperationVisitor().visit(descriptor, operation));
        }

        if (visitor.getSwaggerOperationVisitor() != null) {
            swaggerOperationVisitors.put(visitor.getId(), (SwaggerOperationVisitor<Optional<Policy>>) (descriptor, operation) ->
                    visitor.getSwaggerOperationVisitor().visit(descriptor, operation));
        }
    }

    @Override
    public List<PolicyOperationVisitor> getPolicyVisitors() {
        return policyVisitors;
    }

    @Override
    public SwaggerOperationVisitor getSwaggerOperationVisitor(String policy) {
        return swaggerOperationVisitors.get(policy);
    }

    @Override
    public OAIOperationVisitor getOAIOperationVisitor(String policy) {
        return oaiOperationVisitors.get(policy);
    }
}
