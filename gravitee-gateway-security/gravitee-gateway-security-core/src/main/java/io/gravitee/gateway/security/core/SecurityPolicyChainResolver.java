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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyChainResolver extends AbstractPolicyChainResolver {

    private final Logger logger = LoggerFactory.getLogger(SecurityPolicyChainResolver.class);

    @Autowired
    private SecurityManager securityManager;

    @Override
    public PolicyChain resolve(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        if (streamType == StreamType.ON_REQUEST) {
            final SecurityProvider securityProvider = securityManager.resolve(request);

            if (securityProvider != null) {
                logger.debug("Security provider [{}] has been selected to secure incoming request {}",
                        securityProvider.name(), request.id());

                SecurityPolicy securityPolicy = securityProvider.create(executionContext);
                Policy policy = create(streamType, securityPolicy.policy(), securityPolicy.configuration());

                return RequestPolicyChain.create(
                        Collections.singletonList(policy), executionContext);
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

    @Override
    protected List<Policy> calculate(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        throw new IllegalStateException("You must never go there!");
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }
}
