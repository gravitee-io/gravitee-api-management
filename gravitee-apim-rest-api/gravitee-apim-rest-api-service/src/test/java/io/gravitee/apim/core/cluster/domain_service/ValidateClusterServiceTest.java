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
package io.gravitee.apim.core.cluster.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.apim.infra.json.JsonSchemaCheckerImpl;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateClusterServiceTest {

    private ValidateClusterService validateClusterService;

    @BeforeEach
    void setUp() {
        JsonSchemaChecker jsonSchemaChecker = new JsonSchemaCheckerImpl(new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl()));
        ClusterConfigurationSchemaService clusterConfigurationSchemaService = new ClusterConfigurationSchemaService();
        validateClusterService = new ValidateClusterService(jsonSchemaChecker, clusterConfigurationSchemaService, new ObjectMapper());
    }

    private static Map<String, Object> validConfig() {
        return Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
    }

    @Test
    void should_throw_when_name_is_empty() {
        Cluster cluster = Cluster.builder().name("").configuration(validConfig()).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Name is required.");
    }

    @Test
    void should_throw_when_name_is_null() {
        Cluster cluster = Cluster.builder().name(null).configuration(validConfig()).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Name is required.");
    }

    @Test
    void should_throw_when_configuration_is_null() {
        Cluster cluster = Cluster.builder().name("my-cluster").configuration(null).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Configuration is required.");
    }

    @Test
    void should_pass_with_valid_plaintext_configuration() {
        Cluster cluster = Cluster.builder().name("my-cluster").configuration(validConfig()).build();

        assertThatCode(() -> validateClusterService.validate(cluster)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_protocol_has_invalid_value() {
        Cluster cluster = Cluster.builder()
            .name("my-cluster")
            .configuration(Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "INVALID_PROTOCOL")))
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_throw_when_bootstrap_servers_is_missing() {
        Cluster cluster = Cluster.builder().name("my-cluster").configuration(Map.of("security", Map.of("protocol", "PLAINTEXT"))).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }
}
