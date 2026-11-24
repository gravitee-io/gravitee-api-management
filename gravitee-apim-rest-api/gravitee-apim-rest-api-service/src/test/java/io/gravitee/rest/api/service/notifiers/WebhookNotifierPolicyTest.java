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
package io.gravitee.rest.api.service.notifiers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WebhookNotifierPolicyTest {

    @Nested
    class WithAWhitelistEntryEndingWithASlash {

        private final WebhookNotifierPolicy policy = new WebhookNotifierPolicy(
            new MockEnvironment().withProperty("notifiers.webhook.whitelist[0]", "https://a.example/")
        );

        @Test
        void should_allow_a_url_under_the_entry() {
            assertThat(policy.isAllowed("https://a.example/x")).isTrue();
        }

        @Test
        void should_reject_a_host_that_only_shares_the_entry_without_its_slash() {
            assertThat(policy.isAllowed("https://a.example.evil/")).isFalse();
        }
    }
}
