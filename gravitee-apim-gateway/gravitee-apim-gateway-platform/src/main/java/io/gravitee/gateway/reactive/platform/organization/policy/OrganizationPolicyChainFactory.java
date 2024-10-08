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
package io.gravitee.gateway.reactive.platform.organization.policy;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.platform.organization.reactor.DefaultOrganizationReactor;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactor;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class OrganizationPolicyChainFactory implements PolicyChainFactory<HttpPolicyChain> {

    private final String organizationId;
    private final OrganizationReactorRegistry organizationReactorRegistry;

    @Override
    public HttpPolicyChain create(final String flowChainId, final Flow flow, final ExecutionPhase phase) {
        OrganizationReactor organizationReactor = organizationReactorRegistry.get(organizationId);
        if (organizationReactor instanceof DefaultOrganizationReactor defaultOrganizationReactor) {
            return defaultOrganizationReactor.policyChainFactory().create(flowChainId, flow, phase);
        }
        throw new IllegalStateException(
            String.format(
                "PolicyChainFactory.create() shouldn't be call if no OrganizationReactor has been registered for organization '%s'",
                organizationId
            )
        );
    }
}
