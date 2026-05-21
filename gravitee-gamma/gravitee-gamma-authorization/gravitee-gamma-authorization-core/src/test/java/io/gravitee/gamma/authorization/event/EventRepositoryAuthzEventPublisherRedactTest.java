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
package io.gravitee.gamma.authorization.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventRepositoryAuthzEventPublisherRedactTest {

    @Test
    void redacts_keys_containing_password_token_secret_credential_apikey_case_insensitive() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("email", "alice@example.com");
        attributes.put("password", "hunter2");
        attributes.put("OAUTH_TOKEN", "abc-xyz");
        attributes.put("api_key", "sk-1234");
        attributes.put("ApiKeyHeader", "header-name");
        attributes.put("USER_SECRET", "shh");
        attributes.put("X-Credentials", "raw-cred");
        attributes.put("active", true);

        Map<String, Object> redacted = EventRepositoryAuthzEventPublisher.redactSensitive(attributes);

        assertThat(redacted)
            .containsEntry("email", "alice@example.com")
            .containsEntry("password", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("OAUTH_TOKEN", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("api_key", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("ApiKeyHeader", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("USER_SECRET", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("X-Credentials", EventRepositoryAuthzEventPublisher.REDACTED)
            .containsEntry("active", true);
    }

    @Test
    void leaves_innocuous_keys_alone() {
        Map<String, Object> attributes = Map.of("name", "alice", "tenant", "acme", "active", true);

        Map<String, Object> redacted = EventRepositoryAuthzEventPublisher.redactSensitive(attributes);

        assertThat(redacted).isEqualTo(attributes);
    }

    @Test
    void empty_or_null_input_returns_input_unchanged() {
        assertThat(EventRepositoryAuthzEventPublisher.redactSensitive(null)).isNull();
        assertThat(EventRepositoryAuthzEventPublisher.redactSensitive(Map.of())).isEmpty();
    }
}
