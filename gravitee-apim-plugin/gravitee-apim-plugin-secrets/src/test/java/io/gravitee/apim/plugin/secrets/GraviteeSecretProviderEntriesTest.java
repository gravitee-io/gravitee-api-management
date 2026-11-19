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
    void should_accept_an_entry_bound_to_the_one_environment_it_is_deployed_for() {
        givenEntry(0, "prod-env", "prod-env");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_accept_one_entry_per_environment() {
        givenEntry(0, "dev-env", "dev-env");
        givenEntry(1, "test-env", "test-env");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_accept_a_gateway_with_no_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "vault");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_refuse_an_entry_deployed_for_every_environment() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.environment", "prod-env");

        assertThat(misconfiguration()).hasValueSatisfying(reason ->
            assertThat(reason).contains("api.secrets.providers[0]", "exactly one environment id")
        );
    }

    @Test
    void should_refuse_an_entry_deployed_for_several_environments() {
        givenEntry(0, "dev-env", "dev-env");
        gravitee.put("api.secrets.providers[0].environments[1]", "test-env");

        assertThat(misconfiguration()).hasValueSatisfying(reason -> assertThat(reason).contains("exactly one environment id"));
    }

    @Test
    void should_refuse_an_entry_bound_to_another_environment_than_the_one_it_is_deployed_for() {
        givenEntry(0, "dev-env", "prod-env");

        assertThat(misconfiguration()).hasValueSatisfying(reason ->
            assertThat(reason).contains("api.secrets.providers[0]", "configuration.environment", "[prod-env]", "[dev-env]")
        );
    }

    @Test
    void should_refuse_an_entry_with_no_environment_in_its_configuration() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].environments[0]", "prod-env");

        assertThat(misconfiguration()).hasValueSatisfying(reason -> assertThat(reason).contains("configuration.environment"));
    }

    @Test
    void should_refuse_the_gateway_when_any_gravitee_entry_is_misconfigured() {
        givenEntry(0, "prod-env", "prod-env");
        gravitee.put("api.secrets.providers[1].plugin", "gravitee");

        assertThat(misconfiguration()).hasValueSatisfying(reason -> assertThat(reason).contains("api.secrets.providers[1]"));
    }

    @Test
    void should_refuse_an_entry_whose_id_is_not_gravitee() {
        givenEntry(0, "prod-env", "prod-env");
        gravitee.put("api.secrets.providers[0].id", "internal");

        assertThat(misconfiguration()).hasValueSatisfying(reason ->
            assertThat(reason).contains("api.secrets.providers[0]", "id", "[internal]")
        );
    }

    @Test
    void should_accept_an_entry_that_sets_the_gravitee_id() {
        givenEntry(0, "prod-env", "prod-env");
        gravitee.put("api.secrets.providers[0].id", "gravitee");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_ignore_a_disabled_gravitee_entry() {
        gravitee.put("api.secrets.providers[0].plugin", "gravitee");
        gravitee.put("api.secrets.providers[0].configuration.enabled", "false");

        assertThat(misconfiguration()).isEmpty();
    }

    @Test
    void should_not_check_the_entries_of_other_plugins() {
        gravitee.put("api.secrets.providers[0].plugin", "vault");
        gravitee.put("api.secrets.providers[0].environments[0]", "dev-env");
        gravitee.put("api.secrets.providers[0].environments[1]", "test-env");

        assertThat(misconfiguration()).isEmpty();
    }

    private void givenEntry(int index, String deployedFor, String boundTo) {
        gravitee.put("api.secrets.providers[" + index + "].plugin", "gravitee");
        gravitee.put("api.secrets.providers[" + index + "].environments[0]", deployedFor);
        gravitee.put("api.secrets.providers[" + index + "].configuration.environment", boundTo);
    }

    private Optional<String> misconfiguration() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("gravitee", gravitee));
        return GraviteeSecretProviderEntries.misconfiguration(environment);
    }
}
