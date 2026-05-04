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
package io.gravitee.gamma.core.domain.resource.domain_service;

import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.aCreateCommand;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.aResource;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anAuditInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamma.inmemory.ResourceCrudServiceInMemory;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.ResourcePluginProvider;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import io.gravitee.json.validation.JsonSchemaValidator;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateCreateResourceCommandDomainServiceTest {

    private static final String EMPTY_SCHEMA = "{}";
    private static final String SCHEMA_REJECTING_INPUT =
        "{\"type\":\"object\",\"properties\":{\"required-key\":{\"type\":\"string\"}},\"required\":[\"required-key\"]}";
    private static final String SCHEMA_INJECTING_DEFAULT =
        "{\"type\":\"object\",\"properties\":{\"ttl\":{\"type\":\"integer\"},\"strategy\":{\"type\":\"string\",\"default\":\"LRU\"}},\"required\":[\"strategy\"]}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidatorImpl();

    private ResourceCrudServiceInMemory resourceCrudService;

    @Mock
    private ResourcePluginProvider resourcePluginProvider;

    private ValidateCreateResourceCommandDomainService service;

    @BeforeEach
    void setUp() {
        resourceCrudService = new ResourceCrudServiceInMemory();
        service = new ValidateCreateResourceCommandDomainService(resourceCrudService, jsonSchemaValidator, resourcePluginProvider);
    }

    @Nested
    class should_return_errors {

        @ParameterizedTest
        @NullSource
        @EmptySource
        void when_id_is_absent(String id) {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            var input = anInput(c -> c.id(id));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("id is required"))
            );
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        void when_name_is_absent(String name) {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            var input = anInput(c -> c.name(name));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("name is required"))
            );
        }

        @Test
        void when_name_already_exists_in_same_environment() {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            resourceCrudService.initWith(List.of(aResource()));
            var input = anInput(c -> c.name("my-cache"));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("already exists"))
            );
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        void when_type_is_absent(String type) {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            var input = anInput(c -> c.type(type));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("type is required"))
            );
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        void when_configuration_is_absent(String configuration) {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            var input = anInput(c -> c.configuration(configuration));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("configuration is required"))
            );
        }

        @Test
        void when_schema_validation_fails() {
            givenRepositoryPluginSchema(SCHEMA_REJECTING_INPUT);
            var input = anInput();

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).hasValueSatisfying(errors ->
                assertThat(errors).anyMatch(e -> e.getMessage().contains("required-key"))
            );
        }
    }

    @Nested
    class should_succeed {

        @Test
        void with_valid_command() {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            var input = anInput();

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).isEmpty();
            assertThat(result.value()).isPresent();
        }

        @Test
        void and_return_sanitized_configuration() {
            givenRepositoryPluginSchema(SCHEMA_INJECTING_DEFAULT);
            var input = anInput();

            var result = service.validateAndSanitize(input);

            assertThat(result.value()).hasValueSatisfying(v -> {
                JsonNode sanitized = readJson(v.command().configuration());
                assertThat(sanitized.get("ttl").asInt()).isEqualTo(30);
                assertThat(sanitized.get("strategy").asText()).isEqualTo("LRU");
            });
        }

        @Test
        void when_name_exists_in_different_environment() {
            givenRepositoryPluginSchema(EMPTY_SCHEMA);
            resourceCrudService.initWith(List.of(aResource(r -> r.referenceId("other-env"))));
            var input = anInput(c -> c.name("my-cache"));

            var result = service.validateAndSanitize(input);

            assertThat(result.severe()).isEmpty();
        }
    }

    private ValidateCreateResourceCommandDomainService.Input anInput() {
        return new ValidateCreateResourceCommandDomainService.Input(aCreateCommand(), anAuditInfo());
    }

    private ValidateCreateResourceCommandDomainService.Input anInput(
        UnaryOperator<ResourceFixture.CreateResourceCommandValues> customizer
    ) {
        return new ValidateCreateResourceCommandDomainService.Input(aCreateCommand(customizer), anAuditInfo());
    }

    private void givenRepositoryPluginSchema(String schema) {
        lenient().when(resourcePluginProvider.getSchema(nullable(String.class))).thenReturn(schema);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new AssertionError("Invalid JSON: " + json, e);
        }
    }
}
