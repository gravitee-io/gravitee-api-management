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
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChain implements PolicyChain {

    private Set<Policy> policies;
    private Iterator<Policy> policyIterator;

    protected AbstractPolicyChain(final Set<Policy> policies) {
        this.policies = policies;
        this.policyIterator = policies.iterator();
    }

    public Set<Policy> getPolicies() {
        return this.policies;
    }

    protected Iterator<Policy> iterator() {
        return policyIterator;
    }
 }
