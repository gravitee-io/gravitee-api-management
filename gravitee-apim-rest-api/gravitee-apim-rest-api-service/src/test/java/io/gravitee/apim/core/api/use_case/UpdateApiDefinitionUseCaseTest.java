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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainServiceTestInitializer;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApiDefinitionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2024-04-23T11:06:00Z");
    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    ImportDefinitionUpdateDomainServiceTestInitializer importDefinitionUpdateDomainServiceTestInitializer;

    UpdateApiDefinitionUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        io.gravitee.common.utils.TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        io.gravitee.common.utils.TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        importDefinitionUpdateDomainServiceTestInitializer = new ImportDefinitionUpdateDomainServiceTestInitializer(apiCrudService);

        useCase = new UpdateApiDefinitionUseCase(
            apiCrudService,
            importDefinitionUpdateDomainServiceTestInitializer.initialize(ENVIRONMENT_ID),
            flowCrudService
        );
    }

    @AfterEach
    void tearDown() {
        importDefinitionUpdateDomainServiceTestInitializer.tearDown();
        Stream.of(apiCrudService, flowCrudService).forEach(InMemoryAlternative::reset);
    }

    @ParameterizedTest(name = "Test for API update with {0} definition version")
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void should_not_allow_update_with_non_v4_definition_version(DefinitionVersion definitionVersion) {
        // Given
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().definitionVersion(definitionVersion).build())
            .build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new UpdateApiDefinitionUseCase.Input(API_ID, importDefinition, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiDefinitionVersionNotSupportedException.class);
    }

    @Test
    void should_throw_when_api_export_is_null() {
        // Given
        var importDefinition = ImportDefinition.builder().apiExport(null).build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new UpdateApiDefinitionUseCase.Input(API_ID, importDefinition, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(InvalidApiDefinitionException.class).hasMessageContaining("API definition is required");
    }

    @Test
    void should_throw_when_api_does_not_exist() {
        // Given — apiCrudService starts empty
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().id(API_ID).definitionVersion(DefinitionVersion.V4).build())
            .build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new UpdateApiDefinitionUseCase.Input(API_ID, importDefinition, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Nested
    class UpdateProxyApi {

        @BeforeEach
        void setUp() {
            // Stub ValidateApiDomainService so update flow proceeds
            when(
                importDefinitionUpdateDomainServiceTestInitializer.validateApiDomainService.validateAndSanitizeForUpdate(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer(inv -> inv.getArgument(1));

            // Stub ApiService.update used by UpdateApiDomainServiceImpl.updateV4:
            // delegate.update(executionContext, api.getId(), updateApiEntity, false, userId) — 5 args
            when(importDefinitionUpdateDomainServiceTestInitializer.apiService.update(any(), any(), any(), anyBoolean(), any())).thenAnswer(
                inv -> {
                    io.gravitee.rest.api.model.v4.api.UpdateApiEntity updateApi = inv.getArgument(2);
                    return io.gravitee.rest.api.model.v4.api.ApiEntity.builder()
                        .id(API_ID)
                        .name(updateApi.getName())
                        .apiVersion(updateApi.getApiVersion())
                        .build();
                }
            );
        }

        @Test
        void should_update_proxy_api_from_import_definition() {
            // Given
            var existingApi = aProxyApiV4().toBuilder().id(API_ID).environmentId(ENVIRONMENT_ID).build();
            apiCrudService.initWith(List.of(existingApi));

            var importDefinition = ImportDefinition.builder()
                .apiExport(
                    ApiExport.builder().id(API_ID).name("Updated API").apiVersion("2.0").definitionVersion(DefinitionVersion.V4).build()
                )
                .build();

            // When
            var output = useCase.execute(new UpdateApiDefinitionUseCase.Input(API_ID, importDefinition, AUDIT_INFO));

            // Then
            assertThat(output).isNotNull();
            assertThat(output.apiWithFlows()).isNotNull();
            assertThat(output.apiWithFlows().getId()).isEqualTo(API_ID);
        }

        @Test
        void should_return_actual_api_flows_after_update() {
            // Given
            var existingApi = aProxyApiV4().toBuilder().id(API_ID).environmentId(ENVIRONMENT_ID).build();
            apiCrudService.initWith(List.of(existingApi));
            flowCrudService.saveApiFlows(API_ID, List.of(Flow.builder().id("flow-1").enabled(true).build()));

            var importDefinition = ImportDefinition.builder()
                .apiExport(
                    ApiExport.builder().id(API_ID).name("Updated API").apiVersion("2.0").definitionVersion(DefinitionVersion.V4).build()
                )
                .build();

            // When
            var output = useCase.execute(new UpdateApiDefinitionUseCase.Input(API_ID, importDefinition, AUDIT_INFO));

            // Then
            assertThat(output.apiWithFlows().getFlows()).hasSize(1);
            assertThat(((Flow) output.apiWithFlows().getFlows().get(0)).getId()).isEqualTo("flow-1");
        }
    }
}
