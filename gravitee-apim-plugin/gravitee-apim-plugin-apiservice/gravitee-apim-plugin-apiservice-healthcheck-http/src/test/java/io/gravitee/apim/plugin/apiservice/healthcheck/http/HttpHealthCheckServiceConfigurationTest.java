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
package io.gravitee.apim.plugin.apiservice.healthcheck.http;

import static io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint.DEFAULT_FAILURE_THRESHOLD;
import static io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint.DEFAULT_SUCCESS_THRESHOLD;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpHealthCheckServiceConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_set_all_defaults_when_optional_fields_are_omitted_from_json() throws Exception {
        String json = """
            {"target":"/health","schedule":"*/5 * * * * *"}
            """;

        HttpHealthCheckServiceConfiguration configuration = objectMapper.readValue(json, HttpHealthCheckServiceConfiguration.class);

        assertThat(configuration.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(configuration.getAssertion()).isEqualTo(HttpHealthCheckServiceConfiguration.DEFAULT_ASSERTION);
        assertThat(configuration.isOverrideEndpointPath()).isTrue();
        assertThat(configuration.getSuccessThreshold()).isEqualTo(DEFAULT_SUCCESS_THRESHOLD);
        assertThat(configuration.getFailureThreshold()).isEqualTo(DEFAULT_FAILURE_THRESHOLD);
    }

    @Test
    void should_apply_runtime_defaults_when_assertion_is_blank_or_thresholds_are_zero() throws Exception {
        String json = """
            {
              "target":"/health",
              "schedule":"*/5 * * * * *",
              "assertion":"",
              "successThreshold":0,
              "failureThreshold":0
            }
            """;

        HttpHealthCheckServiceConfiguration configuration = objectMapper.readValue(json, HttpHealthCheckServiceConfiguration.class);
        configuration.applyRuntimeDefaults();

        assertThat(configuration.getAssertion()).isEqualTo(HttpHealthCheckServiceConfiguration.DEFAULT_ASSERTION);
        assertThat(configuration.getSuccessThreshold()).isEqualTo(DEFAULT_SUCCESS_THRESHOLD);
        assertThat(configuration.getFailureThreshold()).isEqualTo(DEFAULT_FAILURE_THRESHOLD);
    }

    @Test
    void should_keep_explicit_values_from_json() throws Exception {
        String json = """
            {
              "target":"/health",
              "schedule":"*/5 * * * * *",
              "assertion":"{#response.status == 404}",
              "method":"POST",
              "overrideEndpointPath":false,
              "successThreshold":5,
              "failureThreshold":7
            }
            """;

        HttpHealthCheckServiceConfiguration configuration = objectMapper.readValue(json, HttpHealthCheckServiceConfiguration.class);

        assertThat(configuration.getAssertion()).isEqualTo("{#response.status == 404}");
        assertThat(configuration.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(configuration.isOverrideEndpointPath()).isFalse();
        assertThat(configuration.getSuccessThreshold()).isEqualTo(5);
        assertThat(configuration.getFailureThreshold()).isEqualTo(7);
    }

    @Test
    void should_apply_builder_defaults_for_all_optional_fields() {
        HttpHealthCheckServiceConfiguration configuration = HttpHealthCheckServiceConfiguration.builder()
            .target("/health")
            .schedule("*/5 * * * * *")
            .build();

        assertThat(configuration.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(configuration.getAssertion()).isEqualTo(HttpHealthCheckServiceConfiguration.DEFAULT_ASSERTION);
        assertThat(configuration.isOverrideEndpointPath()).isTrue();
        assertThat(configuration.getSuccessThreshold()).isEqualTo(DEFAULT_SUCCESS_THRESHOLD);
        assertThat(configuration.getFailureThreshold()).isEqualTo(DEFAULT_FAILURE_THRESHOLD);
    }

    @Test
    void java_constants_should_match_schema_form_defaults() throws Exception {
        JsonNode schema = objectMapper.readTree(getClass().getResourceAsStream("/schemas/schema-form.json"));
        JsonNode properties = schema.get("properties");

        assertThat(properties.get("method").get("default").asText()).isEqualTo(HttpMethod.GET.name());
        assertThat(properties.get("assertion").get("default").asText()).isEqualTo(HttpHealthCheckServiceConfiguration.DEFAULT_ASSERTION);
        assertThat(properties.get("overrideEndpointPath").get("default").asBoolean()).isTrue();
        assertThat(properties.get("successThreshold").get("default").asInt()).isEqualTo(DEFAULT_SUCCESS_THRESHOLD);
        assertThat(properties.get("failureThreshold").get("default").asInt()).isEqualTo(DEFAULT_FAILURE_THRESHOLD);
    }
}
