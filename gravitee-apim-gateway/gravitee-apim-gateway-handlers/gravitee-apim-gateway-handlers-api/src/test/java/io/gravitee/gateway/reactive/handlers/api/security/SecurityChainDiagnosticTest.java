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
        @DisplayName("Should return exception for no subscription plans")
        void shouldReturnExceptionForNoSubscriptionPlans() {
            // Given
            diagnostic.markPlanHasNoSubscription("plan1");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No active subscription was found for the following plan: plan1");
        }

        @Test
        @DisplayName("Should return exception for expired subscription plans")
        void shouldReturnExceptionForExpiredSubscriptionPlans() {
            // Given
            diagnostic.markPlanHasExpiredSubscription("plan1");
            diagnostic.markPlanHasExpiredSubscription("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("The subscription has expired for the following plans: plan1, plan2");
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
            diagnostic.markPlanHasNoSubscription("plan2");
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
            diagnostic.markPlanHasExpiredSubscription("plan1");
            diagnostic.markPlanHasNoSubscription("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("No active subscription was found for the following plan: plan2");
        }

        @Test
        @DisplayName("Should prioritize expired subscription over no matching rule")
        void shouldPrioritizeExpiredSubscriptionOverNoMatchingRule() {
            // Given
            diagnostic.markPlanHasNoMachingRule("plan1");
            diagnostic.markPlanHasExpiredSubscription("plan2");

            // When
            Exception cause = diagnostic.cause();

            // Then
            assertThat(cause.getMessage()).isEqualTo("The subscription has expired for the following plan: plan2");
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
