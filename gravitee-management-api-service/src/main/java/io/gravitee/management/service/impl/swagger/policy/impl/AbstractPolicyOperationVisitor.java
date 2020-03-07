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
package io.gravitee.management.service.impl.swagger.policy.impl;

import io.gravitee.management.service.impl.swagger.visitor.OperationVisitor;
import io.gravitee.policy.api.swagger.Policy;

import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPolicyOperationVisitor<D, O> implements OperationVisitor<Optional<Policy>, D, O> {

    private final io.gravitee.policy.api.swagger.OperationVisitor<D, O> policyVisitor;

    public AbstractPolicyOperationVisitor(io.gravitee.policy.api.swagger.OperationVisitor<D, O> policyVisitor) {
        this.policyVisitor = policyVisitor;
    }

    @Override
    public Optional<Policy> visit(D descriptor, O operation) {
        return policyVisitor.visit(descriptor, operation);
    }
}
