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
package io.gravitee.gateway.handlers.api.security;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.LazyJwtToken;
import io.gravitee.reporter.api.http.Metrics;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class JwtPlanBasedAuthenticationHandlerTest {

    private static final String PLAN_ID = "PLAN_ID";
    private static final String API_ID = "API_ID";
    private static final String CLIENT_ID = "CLIENT_ID";

    @InjectMocks
    private JwtPlanBasedAuthenticationHandler authenticationHandler;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private SubscriptionCacheService subscriptionService;

    @Mock
    private Plan plan;

    @Mock
    private LazyJwtToken token;

    @Mock
    private Request request;

    @Mock
    private Subscription subscription;

    @Before
    public void setup() {
        when(plan.getId()).thenReturn(PLAN_ID);
        when(authenticationContext.getApi()).thenReturn(API_ID);
        when(authenticationContext.request()).thenReturn(request);
    }

    @Test
    public void getClientId_should_return_clientId_from_custom_claim() {
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"my-custom-claim\"}");
        Map<String, Object> claims = Map.of("azp", "value1", "my-custom-claim", "value2", "aud", "value3");

        assertEquals("value2", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_extract_custom_claim_once_then_use_cache() {
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"my-custom-claim\"}");
        Map<String, Object> claims = Map.of("azp", "value1", "my-custom-claim", "value2", "aud", "value3");

        for (int i = 0; i < 10; i++) {
            assertEquals("value2", authenticationHandler.getClientId(claims));
        }

        // Security definition should be accessed only once.
        verify(plan).getSecurityDefinition();
    }

    @Test
    public void getClientId_should_return_clientId_from_authorized_party_claim() {
        Map<String, Object> claims = Map.of("aud", "value1", "my-custom-claim", "value2", "azp", "value3");

        assertEquals("value3", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_clientId_from_audience_claim() {
        Map<String, Object> claims = Map.of("another", "value1", "my-custom-claim", "value2", "aud", "value3");

        assertEquals("value3", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_clientId_from_client_id_claim() {
        Map<String, Object> claims = Map.of("another", "value1", "my-custom-claim", "value2", "client_id", "value3");

        assertEquals("value3", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_null_cause_no_clientId_found() {
        Map<String, Object> claims = Map.of("another", "value1", "my-custom-claim", "value2", "anotheragain", "value3");

        assertNull(authenticationHandler.getClientId(claims));
    }

    @Test
    public void preCheckSubscription_should_return_false_cause_no_token_in_context() {
        assertFalse(authenticationHandler.preCheckSubscription(authenticationContext));
    }

    @Test
    public void preCheckSubscription_should_return_false_cause_null_token_claims() {
        when(token.getClaims()).thenReturn(null);
        when(authenticationContext.get("jwt")).thenReturn(token);

        assertFalse(authenticationHandler.preCheckSubscription(authenticationContext));
    }

    @Test
    public void preCheckSubscription_should_return_false_cause_no_client_id_in_token() {
        when(token.getClaims()).thenReturn(new HashMap<>());
        when(authenticationContext.get("jwt")).thenReturn(token);

        assertFalse(authenticationHandler.preCheckSubscription(authenticationContext));
    }

    @Test
    public void preCheckSubscription_should_return_false_cause_subscription_not_found_and_not_last_handler() {
        when(token.getClaims()).thenReturn(Map.of("client_id", CLIENT_ID));
        when(authenticationContext.get("jwt")).thenReturn(token);
        when(
            authenticationContext.getInternalAttribute(AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE)
        ).thenReturn(false);

        assertFalse(authenticationHandler.preCheckSubscription(authenticationContext));
        verify(subscriptionService).getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
    }

    @Test
    public void preCheckSubscription_should_return_true_and_skip_when_subscription_not_found_and_last_handler() {
        when(token.getClaims()).thenReturn(Map.of("client_id", CLIENT_ID));
        when(authenticationContext.get("jwt")).thenReturn(token);
        when(
            authenticationContext.getInternalAttribute(AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE)
        ).thenReturn(true);

        assertTrue(authenticationHandler.preCheckSubscription(authenticationContext));
        verifyNoInteractions(subscriptionService);
    }

    @Test
    public void preCheckSubscription_should_return_false_cause_subscription_ended() {
        when(token.getClaims()).thenReturn(Map.of("client_id", CLIENT_ID));
        when(authenticationContext.get("jwt")).thenReturn(token);
        when(subscription.isTimeValid(anyLong())).thenReturn(false);
        when(request.timestamp()).thenReturn(new java.util.Date().getTime());
        when(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).thenReturn(Optional.of(subscription));

        assertFalse(authenticationHandler.preCheckSubscription(authenticationContext));
        verify(subscriptionService).getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
    }

    @Test
    public void preCheckSubscription_should_return_true() {
        when(token.getClaims()).thenReturn(Map.of("client_id", CLIENT_ID));
        when(authenticationContext.get("jwt")).thenReturn(token);
        when(subscription.isTimeValid(anyLong())).thenReturn(true);
        when(request.timestamp()).thenReturn(new java.util.Date().getTime());
        when(request.metrics()).thenReturn(Metrics.on(new java.util.Date().getTime()).build());
        when(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).thenReturn(Optional.of(subscription));

        assertTrue(authenticationHandler.preCheckSubscription(authenticationContext));
        verify(subscriptionService).getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
    }
}
