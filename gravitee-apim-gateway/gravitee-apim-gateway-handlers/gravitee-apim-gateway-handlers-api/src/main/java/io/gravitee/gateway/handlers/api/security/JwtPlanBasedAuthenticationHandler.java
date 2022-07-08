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
package io.gravitee.gateway.handlers.api.security;

import static io.gravitee.reporter.api.http.SecurityType.JWT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.jwt.LazyJwtToken;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class JwtPlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtPlanBasedAuthenticationHandler.class);
    private static final String CLIENT_ID_CLAIM_PARAMETER = "clientIdClaim";

    private static final String CONTEXT_ATTRIBUTE_JWT = "jwt";

    private static final String CLAIM_CLIENT_ID = "client_id";
    private static final String CLAIM_AUDIENCE = "aud";
    private static final String CLAIM_AUTHORIZED_PARTY = "azp";

    private ObjectMapper objectMapper = new ObjectMapper();

    private SubscriptionRepository subscriptionRepository;

    public JwtPlanBasedAuthenticationHandler(AuthenticationHandler handler, Plan plan, SubscriptionRepository subscriptionRepository) {
        super(handler, plan);
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    protected boolean canHandleSubscription(AuthenticationContext authenticationContext) {
        LazyJwtToken token = (LazyJwtToken) authenticationContext.get(CONTEXT_ATTRIBUTE_JWT);
        if (token == null || token.getClaims() == null) {
            return false;
        }

        String clientId = getClientId(token.getClaims());
        if (clientId == null) {
            return false;
        }

        return canHandleSubscription(authenticationContext.getApi(), clientId, authenticationContext);
    }

    private boolean canHandleSubscription(String api, String clientId, AuthenticationContext authenticationContext) {
        try {
            List<Subscription> subscriptions = subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                    .apis(Collections.singleton(api))
                    .plans(Collections.singleton(plan.getId()))
                    .clientId(clientId)
                    .status(Subscription.Status.ACCEPTED)
                    .build()
            );

            if (subscriptions != null && !subscriptions.isEmpty()) {
                final Subscription subscription = subscriptions.get(0);
                if (
                    subscription != null &&
                    (
                        subscription.getEndingAt() == null ||
                        subscription.getEndingAt().after(new Date(authenticationContext.request().timestamp()))
                    )
                ) {
                    authenticationContext.setApplication(subscription.getApplication());
                    authenticationContext.setPlan(subscription.getPlan());
                    authenticationContext.setSubscription(subscription.getId());
                    authenticationContext.request().metrics().setSecurityType(JWT);
                    authenticationContext.request().metrics().setSecurityToken(clientId);
                    return true;
                }
            }
        } catch (TechnicalException e) {
            LOGGER.error("Failed to check JWT plan subscription", e);
        }
        return false;
    }

    /**
     * Get clientID from JWT claims
     * FIXME : This is duplicated from JWT policy
     *
     * @param claims
     * @return clientId
     */
    protected String getClientId(Map<String, Object> claims) {
        if (getCustomClientIdClaimName() != null) {
            Object clientIdClaim = claims.get(getCustomClientIdClaimName());
            return extractClientId(clientIdClaim);
        }

        String clientId = null;

        // Look for the client_id of the Authorized party claim
        String authorizedParty = (String) claims.get(CLAIM_AUTHORIZED_PARTY);
        if (authorizedParty != null && !authorizedParty.isEmpty()) {
            clientId = authorizedParty;
        }

        // Look for the client_id of the audience claim
        if (clientId == null) {
            Object audClaim = claims.get(CLAIM_AUDIENCE);
            clientId = extractClientId(audClaim);
        }

        // Is there any client_id claim ?
        if (clientId == null) {
            clientId = (String) claims.get(CLAIM_CLIENT_ID);
        }

        return clientId;
    }

    /**
     * Extract client_id from claim.
     * FIXME : This is duplicated from JWT policy
     *
     * @param claim
     * @return clientId
     */
    private String extractClientId(Object claim) {
        if (claim != null) {
            if (claim instanceof List) {
                List<String> claims = (List<String>) claim;
                // For the moment, we took only the first value of the array
                return claims.get(0);
            } else {
                return (String) claim;
            }
        }
        return null;
    }

    /**
     * Get custom claim name containing client_id if it has been specified in JWT policy configuration.
     * FIXME : Policy configuration should be handled by the policy itself
     *
     * @return name of the claim containing client_id configured at policy level
     */
    private String getCustomClientIdClaimName() {
        try {
            if (plan.getSecurityDefinition() != null) {
                JsonNode clientIdClaimNode = objectMapper.readTree(plan.getSecurityDefinition()).get(CLIENT_ID_CLAIM_PARAMETER);
                if (clientIdClaimNode != null) {
                    return clientIdClaimNode.asText();
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to read plan security definition", e);
        }
        return null;
    }
}
