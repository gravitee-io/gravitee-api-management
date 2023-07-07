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
package io.gravitee.gateway.security.oauth2.policy;

import static io.gravitee.reporter.api.http.SecurityType.OAUTH2;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CheckSubscriptionPolicy implements Policy {

    static final String CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED =
        ExecutionContext.ATTR_PREFIX + ExecutionContext.ATTR_PLAN + ".selection.rule.based";

    static final String CONTEXT_ATTRIBUTE_CLIENT_ID = "oauth.client_id";
    static final String BEARER_AUTHORIZATION_TYPE = "Bearer";
    static final String GATEWAY_OAUTH2_ACCESS_DENIED_KEY = "GATEWAY_OAUTH2_ACCESS_DENIED";
    static final String GATEWAY_OAUTH2_INVALID_CLIENT_KEY = "GATEWAY_OAUTH2_INVALID_CLIENT";
    static final String OAUTH2_UNAUTHORIZED_MESSAGE = "Unauthorized";

    @Override
    public void execute(PolicyChain policyChain, ExecutionContext executionContext) throws PolicyException {
        SubscriptionService subscriptionService = executionContext.getComponent(SubscriptionService.class);

        final String api = (String) executionContext.getAttribute(ExecutionContext.ATTR_API);
        final String clientId = (String) executionContext.getAttribute(CONTEXT_ATTRIBUTE_CLIENT_ID);
        final String plan = (String) executionContext.getAttribute(ExecutionContext.ATTR_PLAN);

        // client_id is mandatory
        if (clientId == null || clientId.trim().isEmpty()) {
            sendError(GATEWAY_OAUTH2_INVALID_CLIENT_KEY, executionContext.response(), policyChain);
            return;
        }

        executionContext.request().metrics().setSecurityType(OAUTH2);
        executionContext.request().metrics().setSecurityToken(clientId);

        Optional<io.gravitee.gateway.api.service.Subscription> optionalSubscription = subscriptionService.getByApiAndClientIdAndPlan(
            api,
            clientId,
            plan
        );

        if (optionalSubscription.isPresent()) {
            final boolean selectionRuleBasedPlan = Boolean.TRUE.equals(
                executionContext.getAttribute(CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED)
            );

            final io.gravitee.gateway.api.service.Subscription subscription = optionalSubscription
                // FIXME: Remove `!selectionRuleBasedPlan` wild behavior when plan selection based on rules will be fixed
                .filter(sub -> !selectionRuleBasedPlan || sub.getPlan().equals(plan))
                .orElse(null);

            if (subscription != null && subscription.isTimeValid(executionContext.request().timestamp())) {
                executionContext.setAttribute(ExecutionContext.ATTR_APPLICATION, subscription.getApplication());
                executionContext.setAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID, subscription.getId());
                executionContext.setAttribute(ExecutionContext.ATTR_PLAN, subscription.getPlan());

                policyChain.doNext(executionContext.request(), executionContext.response());
                return;
            }
        }

        policyChain.failWith(
            PolicyResult.failure(GATEWAY_OAUTH2_ACCESS_DENIED_KEY, HttpStatusCode.UNAUTHORIZED_401, OAUTH2_UNAUTHORIZED_MESSAGE)
        );
    }

    /**
     * As per https://tools.ietf.org/html/rfc6750#page-7:
     *
     *      HTTP/1.1 401 Unauthorized
     *      WWW-Authenticate: Bearer realm="example",
     *      error="invalid_token",
     *      error_description="The access token expired"
     */
    private void sendError(String key, Response response, PolicyChain policyChain) {
        String headerValue = BEARER_AUTHORIZATION_TYPE + " realm=\"gravitee.io\"";
        response.headers().add(HttpHeaders.WWW_AUTHENTICATE, headerValue);
        policyChain.failWith(PolicyResult.failure(key, HttpStatusCode.UNAUTHORIZED_401, OAUTH2_UNAUTHORIZED_MESSAGE));
    }

    @Override
    public String id() {
        return "check-subscription";
    }
}
