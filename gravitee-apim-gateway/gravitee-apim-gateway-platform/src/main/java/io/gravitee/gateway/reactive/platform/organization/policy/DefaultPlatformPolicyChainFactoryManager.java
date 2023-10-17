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

import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultPlatformPolicyChainFactoryManager implements OrganizationPolicyChainFactoryManager {

    private final OrganizationReactorRegistry organizationReactorRegistry;

    @Override
    public PolicyChainFactory get(final String organizationId) {
        return new OrganizationPolicyChainFactory(organizationId, organizationReactorRegistry);
    }
}
