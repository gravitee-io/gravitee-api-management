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
import net.minidev.json.JSONObject;
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
    public void getClientId_should_resolve_nested_claim_via_dot_notation() {
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"act.repository\"}");

        JSONObject act = new JSONObject();
        act.put("repository", "my-nested-client-id");
        Map<String, Object> claims = new HashMap<>();
        claims.put("act", act);

        assertEquals("my-nested-client-id", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_prefer_flat_claim_over_nested_walk_for_backward_compat() {
        // A claim whose key literally contains a dot must be found by flat lookup
        // even when a matching nested structure also exists.
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"act.repository\"}");

        JSONObject act = new JSONObject();
        act.put("repository", "nested-value");
        Map<String, Object> claims = new HashMap<>();
        claims.put("act.repository", "flat-literal-value"); // flat key with dot
        claims.put("act", act);

        // Flat key wins
        assertEquals("flat-literal-value", authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_null_when_nested_segment_is_missing() {
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"act.missing\"}");

        JSONObject act = new JSONObject();
        act.put("repository", "my-nested-client-id");
        Map<String, Object> claims = new HashMap<>();
        claims.put("act", act);

        assertNull(authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_null_when_intermediate_is_not_a_map() {
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"sub.nested\"}");

        // "sub" is a String, not a nested object — walk must abort gracefully
        Map<String, Object> claims = Map.of("sub", "plain-string-value");

        assertNull(authenticationHandler.getClientId(claims));
    }

    @Test
    public void getClientId_should_return_null_when_clientIdClaim_is_empty_string() {
        // empty string is non-null so it enters the custom-claim branch;
        // resolveNestedClaim returns null and the azp/aud/client_id fallback is bypassed
        when(plan.getSecurityDefinition()).thenReturn("{\"clientIdClaim\": \"\"}");
        Map<String, Object> claims = Map.of("azp", "should-not-be-returned", "client_id", "also-not-returned");

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
