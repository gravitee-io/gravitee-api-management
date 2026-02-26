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
package io.gravitee.gateway.reactive.handlers.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class SecurityChainDiagnosticTest {

    private SecurityChainDiagnostic diagnostic;

    @BeforeEach
    void setUp() {
        diagnostic = new SecurityChainDiagnostic();
    }

    @Nested
    @DisplayName("message")
    class MessageTests {

        @Test
        @DisplayName("Should return Unauthorized when verbose401 is false regardless of diagnostics")
        void shouldReturnUnauthorizedWhenVerboseDisabled() {
            diagnostic.markPlanHasInvalidToken("plan1");

            assertThat(diagnostic.message()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("Should return Unauthorized when no issues are recorded")
        void shouldReturnUnauthorizedWhenEmpty() {
            assertThat(diagnostic.message()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("Should return invalid token message when verbose401 is true")
        void shouldReturnInvalidTokenMessage() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasInvalidToken("plan1");

            assertThat(verbose.message()).isEqualTo("The provided authentication token is invalid");
        }

        @Test
        @DisplayName("Should return not authorized message when verbose401 is true")
        void shouldReturnNotAuthorizedMessage() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasNoSubscription("plan1", "API_KEY", "1234567890abcdef");

            assertThat(verbose.message()).isEqualTo("The provided credentials are not authorized");
        }

        @Test
        @DisplayName("Should return expired message when verbose401 is true")
        void shouldReturnExpiredMessage() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasExpiredSubscription("plan1", "app1");

            assertThat(verbose.message()).isEqualTo("Access has expired for the provided credentials");
        }

        @Test
        @DisplayName("Should return no plan matched message when verbose401 is true")
        void shouldReturnNoMatchingRuleMessage() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasNoMachingRule("plan1");

            assertThat(verbose.message()).isEqualTo("No plan matched the request");
        }

        @Test
        @DisplayName("Should return no token message when verbose401 is true")
        void shouldReturnNoTokenMessage() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasNoToken("plan1");

            assertThat(verbose.message()).isEqualTo("The request did not include an authentication token");
        }

        @Test
        @DisplayName("Should prioritize invalid token over all other issues when verbose401 is true")
        void shouldPrioritizeInvalidTokenOverAll() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasNoToken("plan1");
            verbose.markPlanHasNoSubscription("plan2", "CLIENT_ID", "my-client-id");
            verbose.markPlanHasExpiredSubscription("plan3", "app1");
            verbose.markPlanHasInvalidToken("plan4");

            assertThat(verbose.message()).isEqualTo("The provided authentication token is invalid");
        }

        @Test
        @DisplayName("Should prioritize no subscription over expired subscription when verbose401 is true")
        void shouldPrioritizeNoSubscriptionOverExpired() {
            var verbose = new SecurityChainDiagnostic(true);
            verbose.markPlanHasExpiredSubscription("plan1", "app1");
            verbose.markPlanHasNoSubscription("plan2", "API_KEY", "short");

            assertThat(verbose.message()).isEqualTo("The provided credentials are not authorized");
        }
    }

    @Nested
    @DisplayName("cause")
    class CauseTests {

        @Test
        @DisplayName("Should return exception for invalid token plans")
        void shouldReturnExceptionForInvalidTokenPlans() {
            // Given
            diagnostic.markPlanHasInvalidToken("plan1");
            diagnostic.markPlanHasInvalidToken("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("The provided authentication token is invalid for the following plans: plan1, plan2");
        }

        @Test
        @DisplayName("Should return exception for no subscription plans with API key")
        void shouldReturnExceptionForNoSubscriptionPlansWithApiKey() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "API_KEY", "1234567890abcdef");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for API Key: 1234***cdef (plan: plan1)");
        }

        @Test
        @DisplayName("Should return exception for no subscription plans with client ID")
        void shouldReturnExceptionForNoSubscriptionPlansWithClientId() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "CLIENT_ID", "my-client-id-12345");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for Client ID: my-c***2345 (plan: plan1)");
        }

        @Test
        @DisplayName("Should return exception for no subscription plans with client ID short token")
        void shouldReturnExceptionForNoSubscriptionPlansWithClientIdShortToken() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "CLIENT_ID", "my");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for Client ID: m*** (plan: plan1)");
        }

        @Test
        @DisplayName("Should return exception for no subscription plans with certificate")
        void shouldReturnExceptionForNoSubscriptionPlansWithCertificate() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "CERTIFICATE", "CN=mycert,O=myorg");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for Certificate: CN=m***yorg (plan: plan1)");
        }

        @Test
        @DisplayName("Should mention API key and client ID when mixed")
        void shouldMentionApiKeyAndClientIdWhenMixed() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "API_KEY", "1234567890abcdef");
            diagnostic.markPlanHasNoSubscription("plan2", "CLIENT_ID", "my-client-id-12345");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo(
                "No subscription was found for API Key: 1234***cdef (plan: plan1) or for Client ID: my-c***2345 (plan: plan2)"
            );
        }

        @Test
        @DisplayName("Should mention certificate and API key when mixed")
        void shouldMentionCertificateAndApiKeyWhenMixed() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1", "API_KEY", "1234567890abcdef");
            diagnostic.markPlanHasNoSubscription("plan2", "CERTIFICATE", "CN=mycert,O=myorg");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo(
                "No subscription was found for API Key: 1234***cdef (plan: plan1) or for Certificate: CN=m***yorg (plan: plan2)"
            );
        }

        @Test
        @DisplayName("Should list multiple plans for same credential type")
        void shouldListMultiplePlansWithAndForSameCredentialType() {
            // Given
            diagnostic.markPlanHasNoSubscription("key", "API_KEY", "a657000000097e6");
            diagnostic.markPlanHasNoSubscription("second key plan", "API_KEY", "a657000000097e6");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for API Key: a657***97e6 (plans: key, second key plan)");
        }

        @Test
        @DisplayName("Should return exception for expired subscription plans")
        void shouldReturnExceptionForExpiredSubscriptionPlans() {
            // Given
            diagnostic.markPlanHasExpiredSubscription("plan1", "app1");
            diagnostic.markPlanHasExpiredSubscription("plan2", "app2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo(
                "The subscription has expired for the following plans: plan1 (application: app1), plan2 (application: app2)"
            );
        }

        @Test
        @DisplayName("Should return exception for no matching rule plans")
        void shouldReturnExceptionForNoMatchingRulePlans() {
            // Given
            diagnostic.markPlanHasNoMachingRule("plan1");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("None of the selection rules matched for the following plan: plan1");
        }

        @Test
        @DisplayName("Should return exception for no token plans")
        void shouldReturnExceptionForNoTokenPlans() {
            // Given
            diagnostic.markPlanHasNoToken("plan1");
            diagnostic.markPlanHasNoToken("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo(
                "The request did not include an authentication token for the following plans: plan1, plan2"
            );
        }

        @Test
        @DisplayName("Should return generic exception when no specific issues")
        void shouldReturnGenericExceptionWhenNoSpecificIssues() {
            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No valid plan was found for this request.");
        }

        @Test
        @DisplayName("Should prioritize invalid token over other issues")
        void shouldPrioritizeInvalidTokenOverOtherIssues() {
            // Given
            diagnostic.markPlanHasNoToken("plan1");
            diagnostic.markPlanHasNoSubscription("plan2", "CLIENT_ID", "my-client-id-12345");
            diagnostic.markPlanHasInvalidToken("plan3");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("The provided authentication token is invalid for the following plan: plan3");
        }

        @Test
        @DisplayName("Should prioritize no subscription over expired subscription")
        void shouldPrioritizeNoSubscriptionOverExpiredSubscription() {
            // Given
            diagnostic.markPlanHasExpiredSubscription("plan1", "app1");
            diagnostic.markPlanHasNoSubscription("plan2", "API_KEY", "short");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No subscription was found for API Key: sh***rt (plan: plan2)");
        }

        @Test
        @DisplayName("Should prioritize expired subscription over no matching rule")
        void shouldPrioritizeExpiredSubscriptionOverNoMatchingRule() {
            // Given
            diagnostic.markPlanHasNoMachingRule("plan1");
            diagnostic.markPlanHasExpiredSubscription("plan2", "app1");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("The subscription has expired for the following plan: plan2 (application: app1)");
        }

        @Test
        @DisplayName("Should prioritize no matching rule over no token")
        void shouldPrioritizeNoMatchingRuleOverNoToken() {
            // Given
            diagnostic.markPlanHasNoToken("plan1");
            diagnostic.markPlanHasNoMachingRule("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("None of the selection rules matched for the following plan: plan2");
        }
    }

    @Nested
    @DisplayName("formatPlans")
    class FormatPlansTests {

        @Test
        @DisplayName("Should use singular form for single plan")
        void shouldUseSingularFormForSinglePlan() {
            // Given
            diagnostic.markPlanHasNoToken("plan1");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).contains("plan: plan1");
        }

        @Test
        @DisplayName("Should use plural form for multiple plans")
        void shouldUsePluralFormForMultiplePlans() {
            // Given
            diagnostic.markPlanHasNoToken("plan1");
            diagnostic.markPlanHasNoToken("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).contains("plans: plan1, plan2");
        }
    }
}
