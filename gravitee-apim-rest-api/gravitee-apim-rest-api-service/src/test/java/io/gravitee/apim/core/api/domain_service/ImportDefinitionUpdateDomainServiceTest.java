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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.domain_service.api.ApiImagesServiceProviderImpl;
import io.gravitee.apim.infra.domain_service.api.UpdateApiDomainServiceImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportDefinitionUpdateDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String TARGET_ENVIRONMENT_ID = "target-environment-id";
    private static final String USER = "user";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(TARGET_ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER).build())
        .build();
    private static final String PROMOTED_API_ID = "promoted-api-id";
    private static final String PROMOTED_API_CROSS_ID = "promoted-api-cross-id";

    private final ApiService apiService = mock(ApiService.class);
    private final ApiImagesService apiImagesService = mock(ApiImagesService.class);
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();
    private final PageQueryServiceInMemory pageQueryServiceInMemory = new PageQueryServiceInMemory();
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService = new ApiIdsCalculatorDomainService(
        apiQueryServiceInMemory,
        pageQueryServiceInMemory,
        planQueryServiceInMemory
    );
    private ImportDefinitionUpdateDomainService service;

    @BeforeEach
    void setUp() {
        var updateApiDomainService = new UpdateApiDomainServiceImpl(apiService, apiCrudServiceInMemory);
        var apiImagesServiceProvider = new ApiImagesServiceProviderImpl(apiImagesService);
        service = new ImportDefinitionUpdateDomainService(updateApiDomainService, apiImagesServiceProvider, apiIdsCalculatorDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudServiceInMemory, apiQueryServiceInMemory, planQueryServiceInMemory, pageQueryServiceInMemory).forEach(
            InMemoryAlternative::reset
        );
        reset(apiService);
        reset(apiImagesService);
    }

    @Test
    public void should_update_api_proxy_v4() {
        var picture = "picture";
        var background = "background";
        var existingApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .name("api name")
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        var updatedName = "updated name";
        var importDefinition = ImportDefinition.builder()
            .apiExport(
                ApiExport.builder()
                    .id(PROMOTED_API_ID)
                    .crossId(PROMOTED_API_CROSS_ID)
                    .name(updatedName)
                    .picture(picture)
                    .background(background)
                    .build()
            )
            .build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        var executionContext = new ExecutionContext(ORGANIZATION_ID, TARGET_ENVIRONMENT_ID);
        assertThat(updated).isNotNull();
        verify(apiService).update(
            eq(executionContext),
            eq(PROMOTED_API_ID),
            argThat(update -> updatedName.equals(update.getName())),
            eq(false),
            eq(USER)
        );
        verify(apiImagesService).updateApiBackground(eq(executionContext), eq(PROMOTED_API_ID), eq(background));
        verify(apiImagesService).updateApiPicture(eq(executionContext), eq(PROMOTED_API_ID), eq(picture));
    }

    @Test
    void should_throw_an_exception_when_api_not_supported() {
        var existingApi = ApiFixtures.aLLMProxyApiV4().toBuilder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).build())
            .build();
        Throwable throwable = catchThrowable(() -> service.update(importDefinition, existingApi, AUDIT_INFO));
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Unsupported API type: LLM_PROXY");
    }
}
