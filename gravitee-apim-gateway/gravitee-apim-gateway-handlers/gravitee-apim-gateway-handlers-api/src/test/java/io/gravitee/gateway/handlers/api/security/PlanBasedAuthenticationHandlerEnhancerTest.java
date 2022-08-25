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

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.security.apikey.ApiKeyAuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.jwt.JWTAuthenticationHandler;
import io.gravitee.gateway.security.keyless.KeylessAuthenticationHandler;
import io.gravitee.gateway.security.oauth2.OAuth2AuthenticationHandler;
import io.gravitee.repository.management.model.Plan.PlanSecurityType;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanBasedAuthenticationHandlerEnhancerTest {

    @InjectMocks
    private PlanBasedAuthenticationHandlerEnhancer authenticationHandlerEnhancer;

    @Mock
    private Api api;

    private List<AuthenticationHandler> allAuthenticationHandlers = List.of(
        new ApiKeyAuthenticationHandler(),
        new JWTAuthenticationHandler(),
        new KeylessAuthenticationHandler(),
        new OAuth2AuthenticationHandler()
    );

    @Test
    public void filter_shouldKeepNoHandler_becauseNoPlan() {
        List<AuthenticationHandler> authenticationHandlers = authenticationHandlerEnhancer.filter(allAuthenticationHandlers);

        assertNotNull(authenticationHandlers);
        assertTrue(authenticationHandlers.isEmpty());
    }

    @Test
    public void filter_shouldKeepNoHandler_becauseNoneMatching() {
        mockApiPlans(List.of(API_KEY));

        List<AuthenticationHandler> authenticationHandlers = authenticationHandlerEnhancer.filter(List.of(new JWTAuthenticationHandler()));

        assertNotNull(authenticationHandlers);
        assertTrue(authenticationHandlers.isEmpty());
    }

    @Test
    public void filter_shouldKeepKeylessHandler_becausePlanMatching() {
        mockApiPlans(List.of(KEY_LESS));

        List<AuthenticationHandler> authenticationHandlers = authenticationHandlerEnhancer.filter(allAuthenticationHandlers);

        assertNotNull(authenticationHandlers);
        assertEquals(1, authenticationHandlers.size());
        assertTrue(authenticationHandlers.iterator().next() instanceof DefaultPlanBasedAuthenticationHandler);
    }

    @Test
    public void filter_shouldKeepMutlipleHandlers_correspondingToPlansTypes() {
        // api has 1 keyless plan, 2 jwt plans, and 2 oauth2 plans
        mockApiPlans(List.of(KEY_LESS, JWT, API_KEY, OAUTH2, JWT, OAUTH2));

        List<AuthenticationHandler> authenticationHandlers = authenticationHandlerEnhancer.filter(allAuthenticationHandlers);

        assertNotNull(authenticationHandlers);
        assertEquals(6, authenticationHandlers.size());
        assertTrue(authenticationHandlers.get(0) instanceof DefaultPlanBasedAuthenticationHandler);
        assertTrue(authenticationHandlers.get(1) instanceof JwtPlanBasedAuthenticationHandler);
        assertTrue(authenticationHandlers.get(2) instanceof ApiKeyPlanBasedAuthenticationHandler);
        assertTrue(authenticationHandlers.get(3) instanceof DefaultPlanBasedAuthenticationHandler);
        assertTrue(authenticationHandlers.get(4) instanceof JwtPlanBasedAuthenticationHandler);
        assertTrue(authenticationHandlers.get(5) instanceof DefaultPlanBasedAuthenticationHandler);
    }

    private void mockApiPlans(List<PlanSecurityType> securityTypes) {
        List<Plan> plans = securityTypes
            .stream()
            .map(
                securityType -> {
                    Plan plan = new Plan();
                    plan.setSecurity(securityType.name());
                    return plan;
                }
            )
            .collect(Collectors.toList());
        when(api.getPlans()).thenReturn(plans);
    }
}
