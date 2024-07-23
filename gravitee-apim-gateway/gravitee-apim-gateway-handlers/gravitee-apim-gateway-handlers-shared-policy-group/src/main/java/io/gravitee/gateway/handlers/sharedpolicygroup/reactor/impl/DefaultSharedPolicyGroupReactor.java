/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor.impl;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.SharedPolicyGroupPolicyManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.SharedPolicyGroupPolicyChainFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.policy.PolicyChain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultSharedPolicyGroupReactor
    extends AbstractLifecycleComponent<SharedPolicyGroupReactor>
    implements SharedPolicyGroupReactor {

    private final ReactableSharedPolicyGroup reactableSharedPolicyGroup;
    private final SharedPolicyGroupPolicyChainFactory policyChainFactory;
    private final SharedPolicyGroupPolicyManager sharedPolicyGroupPolicyManager;
    private PolicyChain policyChain;

    public DefaultSharedPolicyGroupReactor(
        ReactableSharedPolicyGroup reactableSharedPolicyGroup,
        SharedPolicyGroupPolicyChainFactory policyChainFactory,
        SharedPolicyGroupPolicyManager sharedPolicyGroupPolicyManager
    ) {
        this.reactableSharedPolicyGroup = reactableSharedPolicyGroup;
        this.policyChainFactory = policyChainFactory;
        this.sharedPolicyGroupPolicyManager = sharedPolicyGroupPolicyManager;
    }

    @Override
    public String id() {
        return reactableSharedPolicyGroup.getId();
    }

    @Override
    public ReactableSharedPolicyGroup reactableSharedPolicyGroup() {
        return reactableSharedPolicyGroup;
    }

    @Override
    public PolicyChain policyChain() {
        return policyChain;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("Organization '{}' reactor  is now starting...", id());
        sharedPolicyGroupPolicyManager.start();
        policyChain =
            policyChainFactory.create(
                reactableSharedPolicyGroup.getId(),
                reactableSharedPolicyGroup.getEnvironmentId(),
                reactableSharedPolicyGroup.getDefinition().getPolicies(),
                ExecutionPhase.valueOf(reactableSharedPolicyGroup.getDefinition().getPhase().name())
            );
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("Organization '{}' reactor  is now stopping...", id());
        sharedPolicyGroupPolicyManager.stop();
    }
}
