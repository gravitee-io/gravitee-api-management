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
package io.gravitee.repository.management.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubscriptionFormTest {

    @Test
    void should_normalize_the_name_to_its_trimmed_lowercase_form() {
        var form = SubscriptionForm.builder().name("  Partner Onboarding  ").build();

        assertThat(form.getNormalizedName()).isEqualTo("partner onboarding");
    }

    @Test
    void should_give_two_names_differing_only_by_case_or_padding_the_same_normalized_form() {
        var form = SubscriptionForm.builder().name("Default").build();
        var otherForm = SubscriptionForm.builder().name(" DEFAULT ").build();

        assertThat(form.getNormalizedName()).isEqualTo(otherForm.getNormalizedName());
    }

    @Test
    void should_have_no_normalized_name_when_the_name_is_not_set() {
        var form = SubscriptionForm.builder().build();

        assertThat(form.getNormalizedName()).isNull();
    }
}
