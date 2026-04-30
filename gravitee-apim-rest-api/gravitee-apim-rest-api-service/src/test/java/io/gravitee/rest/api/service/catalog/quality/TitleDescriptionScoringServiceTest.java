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
package io.gravitee.rest.api.service.catalog.quality;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TitleDescriptionScoringServiceTest {

    private TitleDescriptionScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new TitleDescriptionScoringService();
    }

    @Nested
    class TitleScoring {

        @Test
        void should_return_zero_for_null_title() {
            var result = scoringService.scoreTitle(null);
            assertThat(result.score()).isZero();
            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().getFirst()).contains("missing");
        }

        @Test
        void should_return_zero_for_blank_title() {
            var result = scoringService.scoreTitle("   ");
            assertThat(result.score()).isZero();
        }

        @Test
        void should_score_max_for_ideal_title() {
            var result = scoringService.scoreTitle("Customer Account Management Service");
            assertThat(result.score()).isEqualTo(50);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        void should_penalize_short_titles() {
            var result = scoringService.scoreTitle("Auth");
            assertThat(result.score()).isLessThan(50);
            assertThat(result.issues()).anyMatch(i -> i.contains("too short"));
        }

        @Test
        void should_penalize_generic_titles() {
            var result = scoringService.scoreTitle("My API");
            assertThat(result.score()).isLessThan(30);
            assertThat(result.issues()).anyMatch(i -> i.contains("generic"));
        }

        @Test
        void should_penalize_generic_title_test() {
            var result = scoringService.scoreTitle("test");
            assertThat(result.issues()).anyMatch(i -> i.contains("generic"));
        }

        @Test
        void should_penalize_kebab_case_titles() {
            var result = scoringService.scoreTitle("customer-account-service");
            assertThat(result.score()).isLessThan(50);
            assertThat(result.issues()).anyMatch(i -> i.contains("kebab-case") || i.contains("snake_case"));
        }

        @Test
        void should_penalize_snake_case_titles() {
            var result = scoringService.scoreTitle("payment_gateway_api");
            assertThat(result.issues()).anyMatch(i -> i.contains("kebab-case") || i.contains("snake_case"));
        }

        @Test
        void should_reward_human_readable_titles_with_spaces() {
            var humanResult = scoringService.scoreTitle("Payment Gateway Service");
            var kebabResult = scoringService.scoreTitle("payment-gateway-service");
            assertThat(humanResult.score()).isGreaterThan(kebabResult.score());
        }

        @Test
        void should_penalize_single_word_titles() {
            var result = scoringService.scoreTitle("Payments");
            assertThat(result.issues()).anyMatch(i -> i.contains("meaningful words"));
        }

        @Test
        void should_penalize_very_long_titles() {
            var longTitle = "A".repeat(100);
            var result = scoringService.scoreTitle(longTitle);
            assertThat(result.issues()).anyMatch(i -> i.contains("long"));
        }
    }

    @Nested
    class DescriptionScoring {

        @Test
        void should_return_zero_for_null_description() {
            var result = scoringService.scoreDescription(null, "Some Title");
            assertThat(result.score()).isZero();
            assertThat(result.issues()).anyMatch(i -> i.contains("missing"));
        }

        @Test
        void should_return_zero_for_blank_description() {
            var result = scoringService.scoreDescription("", "Some Title");
            assertThat(result.score()).isZero();
        }

        @Test
        void should_score_high_for_ideal_description() {
            var desc =
                "Manages customer account lifecycle including creation, updates, and deactivation. " +
                "Provides endpoints to retrieve account details, update billing information, and process account verification requests.";
            var result = scoringService.scoreDescription(desc, "Customer Account Service");
            assertThat(result.score()).isGreaterThanOrEqualTo(40);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        void should_penalize_short_descriptions() {
            var result = scoringService.scoreDescription("An API", "Some Title");
            assertThat(result.issues()).anyMatch(i -> i.contains("too short"));
        }

        @Test
        void should_penalize_description_that_duplicates_title() {
            var result = scoringService.scoreDescription("Customer Account Service", "Customer Account Service");
            assertThat(result.issues()).anyMatch(i -> i.contains("similar to the title"));
        }

        @Test
        void should_penalize_near_duplicate_descriptions() {
            var result = scoringService.scoreDescription("Customer Account Service API", "Customer Account Service");
            assertThat(result.issues()).anyMatch(i -> i.contains("similar to the title"));
        }

        @Test
        void should_penalize_boilerplate_descriptions() {
            var result = scoringService.scoreDescription(
                "This is an auto-generated server stub for the payment service. Please update with real documentation.",
                "Payment Service"
            );
            assertThat(result.issues()).anyMatch(i -> i.contains("boilerplate"));
        }

        @Test
        void should_penalize_swagger_boilerplate() {
            var result = scoringService.scoreDescription("Swagger definition for the user management API", "User Management");
            assertThat(result.issues()).anyMatch(i -> i.contains("boilerplate"));
        }

        @Test
        void should_reward_descriptions_with_domain_verbs() {
            var withVerbs = scoringService.scoreDescription(
                "This service manages user authentication and validates session tokens for all downstream services.",
                "Auth Service"
            );
            var withoutVerbs = scoringService.scoreDescription(
                "A backend component for the user login and session token system used by downstream services.",
                "Auth Service"
            );
            assertThat(withVerbs.score()).isGreaterThanOrEqualTo(withoutVerbs.score());
        }

        @Test
        void should_flag_excessively_long_descriptions() {
            var longDesc = "This service manages payments. ".repeat(50);
            var result = scoringService.scoreDescription(longDesc, "Payment Service");
            assertThat(result.issues()).anyMatch(i -> i.contains("excessively long"));
        }

        @Test
        void should_floor_score_at_zero_for_boilerplate_with_no_other_qualities() {
            var result = scoringService.scoreDescription("Auto-generated.", "My API");
            assertThat(result.score()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    class CombinedScoring {

        @Test
        void should_have_max_total_of_100() {
            var titleResult = scoringService.scoreTitle("Customer Account Management Service");
            var descResult = scoringService.scoreDescription(
                "Manages customer account lifecycle including creation, updates, and deactivation. " +
                    "Provides endpoints to retrieve account details, update billing information, and process account verification requests.",
                "Customer Account Management Service"
            );
            assertThat(titleResult.score() + descResult.score()).isLessThanOrEqualTo(100);
        }

        @Test
        void should_give_low_total_for_poorly_documented_api() {
            var titleResult = scoringService.scoreTitle("test");
            var descResult = scoringService.scoreDescription("", "test");
            assertThat(titleResult.score() + descResult.score()).isLessThan(35);
        }
    }
}
