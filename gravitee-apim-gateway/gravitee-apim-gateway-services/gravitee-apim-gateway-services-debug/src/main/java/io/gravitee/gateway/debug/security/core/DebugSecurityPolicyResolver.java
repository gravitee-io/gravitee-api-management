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
package io.gravitee.gateway.debug.security.core;

import io.gravitee.gateway.debug.policy.impl.PolicyDebugDecorator;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.security.core.AuthenticationHandlerSelector;
import io.gravitee.gateway.security.core.HookAuthenticationPolicy;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugSecurityPolicyResolver extends SecurityPolicyResolver {

    public DebugSecurityPolicyResolver(PolicyManager policyManager, AuthenticationHandlerSelector authenticationHandlerSelector) {
        super(policyManager, authenticationHandlerSelector);
    }

    @Override
    protected Policy createHookAuthenticationPolicy(HookAuthenticationPolicy securityPolicy)
        throws InstantiationException, IllegalAccessException {
        final Policy hookAuthenticationPolicy = super.createHookAuthenticationPolicy(securityPolicy);
        final PolicyMetadata policyMetadata = new PolicyMetadata(hookAuthenticationPolicy.id(), null);
        policyMetadata.metadata().put(PolicyMetadata.MetadataKeys.STAGE, SECURITY_POLICY_STAGE);
        return new PolicyDebugDecorator(StreamType.ON_REQUEST, hookAuthenticationPolicy, policyMetadata);
    }
}
