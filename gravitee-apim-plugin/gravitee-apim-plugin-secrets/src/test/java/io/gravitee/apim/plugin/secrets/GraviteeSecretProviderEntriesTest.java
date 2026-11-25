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
package io.gravitee.apim.plugin.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class GraviteeSecretProviderEntriesTest {

    private final Map<String, Object> gravitee = new HashMap<>();

    @Test
    void should_accept_a_gravitee_entry_without_an_id() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_accept_a_gravitee_entry_that_sets_the_gravitee_id() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].id", "gravitee");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_accept_a_gateway_with_no_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "vault");
        gravitee.put("api.secrets.providers[0].id", "all");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_accept_an_empty_secrets_block() {
        gravitee.put("api.secrets", "");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_refuse_a_gravitee_entry_whose_id_is_not_gravitee() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].id", "internal");

        assertThat(misconfiguration()).hasValueSatisfying(reason ->
            assertThat(reason).contains("api.secrets.providers[0]", "id", "[internal]")
        );
    }

    @Test
    void should_refuse_the_gateway_when_any_gravitee_entry_has_another_id() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[1].plugin", "gravitee");
        gravitee.put("api.secrets.providers[1].id", "internal");

        assertThat(misconfiguration()).hasValueSatisfying(reason -> assertThat(reason).contains("api.secrets.providers[1]"));
    }

    @Test
    void should_ignore_a_disabled_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].id", "internal");
        gravitee.put("api.secrets.providers[0].configuration.enabled", "false");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_ignore_the_id_of_an_entry_of_another_plugin_whatever_its_type() {
        gravitee.put("api.secrets.providers[0].plugin", "vault");
        gravitee.put("api.secrets.providers[0].id", 2024);

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_refuse_a_gravitee_entry_it_cannot_read_rather_than_throw() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].id", 2024);

        assertThat(misconfiguration()).hasValueSatisfying(reason -> assertThat(reason).contains("api.secrets.providers[0]"));
    }

    @Test
    void should_give_the_key_of_the_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "vault");
        gravitee.put("api.secrets.providers[1].plugin", "gravitee");
        gravitee.put("api.secrets.providers[1].configuration.encryption.key", "entry-key");

        assertThat(encryptionKey()).contains("entry-key");
    }

    @Test
    void should_give_the_key_of_the_first_enabled_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.enabled", "false");
        gravitee.put("api.secrets.providers[0].configuration.encryption.key", "disabled-key");
        gravitee.put("api.secrets.providers[1].plugin", "gravitee");
        gravitee.put("api.secrets.providers[1].configuration.encryption.key", "enabled-key");

        assertThat(encryptionKey()).contains("enabled-key");
    }

    @Test
    void should_give_no_key_when_the_gravitee_entry_has_a_blank_key() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.encryption.key", " ");

        assertThat(encryptionKey()).isEmpty();
    }

    @Test
    void should_give_no_key_when_no_gravitee_entry_is_enabled() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.enabled", "false");
        gravitee.put("api.secrets.providers[0].configuration.encryption.key", "disabled-key");

        assertThat(encryptionKey()).isEmpty();
    }

    @Test
    void should_skip_a_gravitee_entry_whose_key_it_cannot_read_rather_than_throw() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.enabled", 2024);
        gravitee.put("api.secrets.providers[0].configuration.encryption.key", "unread-key");
        gravitee.put("api.secrets.providers[1].plugin", "gravitee");
        gravitee.put("api.secrets.providers[1].configuration.encryption.key", "entry-key");

        assertThat(encryptionKey()).contains("entry-key");
    }

    private Optional<String> misconfiguration() {
        return GraviteeSecretProviderEntries.misconfiguration(environment());
    }

    private Optional<String> encryptionKey() {
        return GraviteeSecretProviderEntries.encryptionKey(environment());
    }

    private StandardEnvironment environment() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("gravitee", gravitee));
        return environment;
    }
}
