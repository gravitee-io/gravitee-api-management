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
package io.gravitee.gateway.reactive.platform.organization.reactor;

import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.reactive.platform.organization.policy.OrganizationPolicyManager;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultOrganizationReactor extends AbstractOrganizationReactor {

    private final PolicyChainFactory<HttpPolicyChain> policyChainFactory;
    private final OrganizationPolicyManager organizationPolicyManager;

    public DefaultOrganizationReactor(
        final ReactableOrganization reactableOrganization,
        final PolicyChainFactory<HttpPolicyChain> policyChainFactory,
        final OrganizationPolicyManager organizationPolicyManager
    ) {
        super(reactableOrganization);
        this.policyChainFactory = policyChainFactory;
        this.organizationPolicyManager = organizationPolicyManager;
    }

    public PolicyChainFactory<HttpPolicyChain> policyChainFactory() {
        return policyChainFactory;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("Organization '{}' reactor  is now starting...", id());
        organizationPolicyManager.start();
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("Organization '{}' reactor  is now stopping...", id());
        organizationPolicyManager.stop();
    }
}
