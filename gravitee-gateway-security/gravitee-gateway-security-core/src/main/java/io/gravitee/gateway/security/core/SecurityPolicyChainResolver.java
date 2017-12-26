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
package io.gravitee.gateway.security.core;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.policy.api.PolicyResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyChainResolver extends AbstractPolicyChainResolver {

    @Autowired
    private SecurityProviderManager securityManager;

    @Override
    public PolicyChain resolve(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        if (streamType == StreamType.ON_REQUEST) {
            final AuthenticationHandler securityProvider = securityManager.resolve(request);

            if (securityProvider != null) {
                logger.debug("Security provider [{}] has been selected to secure incoming request {}",
                        securityProvider.name(), request.id());

                List<AuthenticationPolicy> policies = securityProvider.handle(executionContext);

                return RequestPolicyChain.create(createAuthenticationChain(policies), executionContext);
            }

            // No authentication method selected, must send a 401
            logger.debug("No security provider has been selected to process request {}. Returning an unauthorized status (401)", request.id());
            return new DirectPolicyChain(
                    PolicyResult.failure(HttpStatusCode.UNAUTHORIZED_401, "Unauthorized"), executionContext);
        } else {
            // In the case of response flow, there is no need for authentication.
            return new NoOpPolicyChain(executionContext);
        }
    }

    private List<Policy> createAuthenticationChain(List<AuthenticationPolicy> securityPolicies) {
        return securityPolicies.stream().map(new Function<AuthenticationPolicy, Policy>() {
            @Override
            public Policy apply(AuthenticationPolicy securityPolicy) {
                if (securityPolicy instanceof HookAuthenticationPolicy) {
                    try {
                        return (Policy) ((HookAuthenticationPolicy) securityPolicy).clazz().newInstance();
                    } catch (Exception ex) {
                        logger.error("Unexpected error while loading authentication policy", ex);
                    }
                } else if (securityPolicy instanceof PluginAuthenticationPolicy) {
                    return create(
                            StreamType.ON_REQUEST,
                            ((PluginAuthenticationPolicy) securityPolicy).name(),
                            ((PluginAuthenticationPolicy) securityPolicy).configuration());
                }

                return null;
            }
        }).collect(Collectors.toList());
    }

    @Override
    protected List<Policy> calculate(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        throw new IllegalStateException("You must never go there!");
    }

    public void setSecurityManager(SecurityProviderManager securityManager) {
        this.securityManager = securityManager;
    }
}
