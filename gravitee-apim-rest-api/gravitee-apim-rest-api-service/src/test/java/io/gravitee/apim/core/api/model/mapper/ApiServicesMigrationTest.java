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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.api.model.utils.MigrationResultUtils.get;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiServicesMigrationTest {

    JsonMapper jsonMapper = JsonMapperFactory.build();
    ApiServicesMigration apiServicesMigration = new ApiServicesMigration(jsonMapper);

    @Test
    void convert_should_return_null_when_input_is_null() {
        var result = apiServicesMigration.convert(null, "ENDPOINT", "testEndpoint");
        assertThat(result).isNull();
    }

    @Test
    void convert_healthCheckService_without_steps_should_map_basic_fields() throws Exception {
        HealthCheckService v2 = new HealthCheckService();
        v2.setEnabled(true);
        v2.setSchedule("*/10 * * * * *");
        v2.setSteps(null);
        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");
        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                assertThat(v4.getType()).isEqualTo("http-health-check");
                assertThat(v4.isEnabled()).isTrue();
                assertThat(v4.isOverrideConfiguration()).isFalse();

                JsonNode cfg = jsonMapper.readTree(v4.getConfiguration());
                assertThat(cfg.get("schedule").asText()).isEqualTo("*/10 * * * * *");
                assertThat(cfg.get("failureThreshold").asInt()).isEqualTo(2);
                assertThat(cfg.get("successThreshold").asInt()).isEqualTo(2);

                assertThat(cfg.get("headers")).isNull();
                assertThat(cfg.get("method")).isNull();
                assertThat(cfg.get("target")).isNull();
                assertThat(cfg.get("assertion")).isNull();
                assertThat(cfg.get("overrideEndpointPath")).isNull();
            });
    }

    @Test
    void convert_healthCheckService_with_null_schedule_should_write_null_schedule() throws Exception {
        HealthCheckService v2 = new HealthCheckService();
        v2.setEnabled(false);
        v2.setSchedule(null);
        v2.setSteps(null);

        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");
        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                JsonNode cfg = jsonMapper.readTree(v4.getConfiguration());
                assertThat(cfg.get("schedule").isNull()).isTrue();
            });
    }

    @Test
    void convert_healthCheckService_with_one_step_should_fill_step_derived_config() throws Exception {
        // given
        String assertStr = "#response.status == 200";

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest healthCheckRequest = new HealthCheckRequest();
        healthCheckRequest.setMethod(HttpMethod.GET);
        healthCheckRequest.setPath("/_health");
        healthCheckRequest.setFromRoot(true);
        healthCheckRequest.setHeaders(List.of(new HttpHeader("X-Test", "1")));
        step.setRequest(healthCheckRequest);
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
        healthCheckResponse.setAssertions(List.of(assertStr));
        step.setResponse(healthCheckResponse);

        HealthCheckService v2 = new HealthCheckService();
        v2.setEnabled(true);
        v2.setSchedule("*/30 * * * * *");
        v2.setSteps(List.of(step));

        // when
        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");

        // then
        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                assertThat(v4.getType()).isEqualTo("http-health-check");
                assertThat(v4.isOverrideConfiguration()).isFalse();

                JsonNode cfg = jsonMapper.readTree(v4.getConfiguration());
                assertThat(cfg.get("schedule").asText()).isEqualTo("*/30 * * * * *");
                assertThat(cfg.get("failureThreshold").asInt()).isEqualTo(2);
                assertThat(cfg.get("successThreshold").asInt()).isEqualTo(2);

                // Step-derived fields
                assertThat(cfg.get("method").asText()).isEqualTo("GET");
                assertThat(cfg.get("target").asText()).isEqualTo("/_health");
                assertThat(cfg.get("overrideEndpointPath").asBoolean()).isTrue();

                assertThat(cfg.get("headers")).isNotNull();
                assertThat(cfg.get("headers").get(0).get("name").asText()).isEqualTo("X-Test");
                assertThat(cfg.get("headers").get(0).get("value").asText()).isEqualTo("1");
                assertThat(cfg.get("assertion")).isNotNull();
                assertThat(cfg.get("assertion").asText()).isEqualTo("{" + assertStr + "}");
            });
    }

    @Test
    void convert_healthCheckService_with_multiple_assertions_should_throw() {
        // given
        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest healthCheckRequest = new HealthCheckRequest();
        healthCheckRequest.setMethod(HttpMethod.GET);
        healthCheckRequest.setPath("/_health");
        healthCheckRequest.setFromRoot(true);
        healthCheckRequest.setHeaders(List.of(new HttpHeader("X-Test", "1")));
        step.setRequest(healthCheckRequest);
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
        healthCheckResponse.setAssertions(List.of("status == 200", "body.contains('OK')"));
        step.setResponse(healthCheckResponse);

        HealthCheckService v2 = new HealthCheckService();
        v2.setEnabled(true);
        v2.setSchedule("*/5 * * * * *");
        v2.setSteps(List.of(step));
        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");
        assertThrows(NullPointerException.class, () -> get(result));
        assertThat(result.issues())
            .map(MigrationResult.Issue::message)
            .containsExactly("Health check for endpoint : testEndpoint cannot have more than one assertion");
        assertThat(result.issues()).map(MigrationResult.Issue::state).containsExactly(MigrationResult.State.IMPOSSIBLE);
    }

    @Test
    void convert_endpointHealthCheckService_should_flip_override_by_inherit_flag() throws Exception {
        // given
        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest healthCheckRequest = new HealthCheckRequest();
        healthCheckRequest.setMethod(HttpMethod.HEAD);
        healthCheckRequest.setPath("/hc");
        healthCheckRequest.setFromRoot(false);
        healthCheckRequest.setHeaders(List.of(new HttpHeader("X-Test", "1")));
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
        healthCheckResponse.setAssertions(List.of("status == 200"));
        step.setResponse(healthCheckResponse);
        step.setRequest(healthCheckRequest);

        EndpointHealthCheckService v2 = new EndpointHealthCheckService();
        v2.setEnabled(true);
        v2.setSchedule("0 */1 * * * *");
        v2.setSteps(List.of(step));
        v2.setInherit(false);

        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");

        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                assertThat(v4.getType()).isEqualTo("http-health-check");
                assertThat(v4.isEnabled()).isTrue();
                assertThat(v4.isOverrideConfiguration()).isTrue(); // flipped because inherit=false

                JsonNode cfg = jsonMapper.readTree(v4.getConfiguration());
                assertThat(cfg.get("schedule").asText()).isEqualTo("0 */1 * * * *");
                assertThat(cfg.get("method").asText()).isEqualTo("HEAD");
                assertThat(cfg.get("target").asText()).isEqualTo("/hc");
            });
    }

    @Test
    void convert_endpointHealthCheckService_inherit_true_should_keep_override_false() {
        // given
        EndpointHealthCheckService v2 = new EndpointHealthCheckService();
        v2.setEnabled(false);
        v2.setSchedule(null);
        v2.setSteps(null);
        v2.setInherit(true);

        var result = apiServicesMigration.convert(v2, "ENDPOINT", "testEndpoint");
        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                assertThat(v4.isOverrideConfiguration()).isFalse();
            });
    }

    @Test
    void shouldConvertValidDynamicPropertyService() throws Exception {
        // Arrange
        HttpDynamicPropertyProviderConfiguration config = new HttpDynamicPropertyProviderConfiguration();
        config.setHeaders(List.of(new HttpHeader("key", "value")));
        config.setMethod(HttpMethod.GET);
        config.setUseSystemProxy(true);
        config.setSpecification("JSON");
        config.setUrl("https://example.com/api");
        DynamicPropertyService v2DynamicPropertyService = DynamicPropertyService
            .builder()
            .schedule("*/5 * * * * *")
            .provider(DynamicPropertyProvider.HTTP)
            .configuration(config)
            .build();

        // Act
        var result = apiServicesMigration.convert(v2DynamicPropertyService, null, null);
        // Assert
        assertThat(get(result))
            .satisfies(v4 -> {
                assertThat(v4).isNotNull();
                assertThat(v4.isOverrideConfiguration()).isFalse();
                assertThat(v4.getType()).isEqualTo("http-dynamic-properties");
                assertThat(v4.getConfiguration()).isNotNull();
                assertThat(
                    v4
                        .getConfiguration()
                        .equals(
                            "{\"schedule\":\"*/5 * * * * *\",\"headers\":[{\"name\":\"key\",\"value\":\"value\"}],\"method\":\"GET\",\"systemProxy\":true,\"transformation\":\"JSON\",\"url\":\"https://example.com/api\"}"
                        )
                );
            });
    }
}
