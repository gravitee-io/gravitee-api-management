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

import static io.gravitee.common.http.HttpMethod.POST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanBasedAuthenticationHandlerTest {

    // test implementation in order to test abstract class behavior
    private static class TestPlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

        public TestPlanBasedAuthenticationHandler(AuthenticationHandler handler, Plan plan) {
            super(handler, plan);
        }

        protected boolean canHandleSubscription(AuthenticationContext authenticationContext) {
            return false;
        }
    }

    @InjectMocks
    private TestPlanBasedAuthenticationHandler authenticationHandler;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private Plan plan;

    @Mock
    private Request request;

    @Test
    public void canHandleSelectionRule_should_return_true_cause_null_selectionRule_on_plan() {
        when(plan.getSelectionRule()).thenReturn(null);

        boolean result = authenticationHandler.canHandleSelectionRule(authenticationContext);
        assertTrue(result);
    }

    @Test
    public void canHandleSelectionRule_should_return_true_cause_empty_selectionRule_on_plan() {
        when(plan.getSelectionRule()).thenReturn("");

        boolean result = authenticationHandler.canHandleSelectionRule(authenticationContext);
        assertTrue(result);
    }

    @Test
    public void canHandleSelectionRule_should_return_false_cause_selectionRule_syntax_is_wrong() {
        when(plan.getSelectionRule()).thenReturn("this is an invalid syntax");

        boolean result = authenticationHandler.canHandleSelectionRule(authenticationContext);
        assertFalse(result);
    }

    @Test
    public void canHandleSelectionRule_should_return_true_cause_selectionRule_evaluation_is_truthy() {
        when(request.method()).thenReturn(POST);
        when(authenticationContext.request()).thenReturn(request);

        when(plan.getSelectionRule()).thenReturn("#request.method == 'POST'");

        boolean result = authenticationHandler.canHandleSelectionRule(authenticationContext);
        assertTrue(result);
    }

    @Test
    public void canHandleSelectionRule_should_return_true_cause_selectionRule_evaluation_is_falsy() {
        when(request.method()).thenReturn(POST);
        when(authenticationContext.request()).thenReturn(request);

        when(plan.getSelectionRule()).thenReturn("#request.method == 'GET'");

        boolean result = authenticationHandler.canHandleSelectionRule(authenticationContext);
        assertFalse(result);
    }
}
