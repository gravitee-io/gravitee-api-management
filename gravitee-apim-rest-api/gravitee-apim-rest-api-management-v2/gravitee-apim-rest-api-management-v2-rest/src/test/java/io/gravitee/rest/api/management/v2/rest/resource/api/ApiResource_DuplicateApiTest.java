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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.api.use_case.ExportApiUseCase;
import io.gravitee.apim.core.api.use_case.ImportApiDefinitionUseCase;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.management.v2.rest.mapper.DuplicateApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.DuplicateApiOptions;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDuplicateException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class ApiResource_DuplicateApiTest extends ApiResourceTest {

    @Inject
    private ExportApiUseCase exportApiUseCase;

    @Inject
    private ImportApiDefinitionUseCase importApiDefinitionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_duplicate";
    }

    @Test
    void should_return_404_if_not_found() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenThrow(
            new ApiNotFoundException(API)
        );

        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(NOT_FOUND_404);
        assertThat(error.getMessage()).isEqualTo("Api [" + API + "] cannot be found.");
    }

    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(
        delimiterString = "|",
        useHeadersInDisplayName = true,
        textBlock = """
           API_DEFINITION[READ] |  ENVIRONMENT_API[CREATE]
           false                  |  false
           true                   |  false
           false                  |  true
        """
    )
    void should_return_403_if_incorrect_permissions(boolean apiDefinitionRead, boolean currentEnvironmentApiCreate) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(apiDefinitionRead);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq(ENVIRONMENT),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(currentEnvironmentApiCreate);
        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(FORBIDDEN_403);
        assertThat(error.getMessage()).isEqualTo("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_400_when_duplicate_exception_is_thrown() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicateService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.map(duplicateOptions))
            )
        ).thenThrow(new ApiDuplicateException("duplication exception message"));

        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("duplication exception message");
    }

    @Test
    void should_return_400_when_duplicating_a_native_api_without_a_host() {
        var apiEntity = ApiFixtures.aModelNativeApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(apiEntity);

        // the host is part of the gateway's unique Kafka hostname, so a copy cannot share the source's
        var options = new DuplicateApiOptions().filteredFields(Set.of());

        final Response response = rootTarget().request().post(Entity.json(options));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("Cannot find a host for Kafka Listener");

        verifyNoInteractions(exportApiUseCase, importApiDefinitionUseCase);
    }

    @Test
    void should_duplicate_v4_api() {
        ApiEntity apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicateService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.map(duplicateOptions))
            )
        ).thenReturn(ApiFixtures.aModelHttpApiV4().toBuilder().id("duplicate").build());

        final Response response = rootTarget().request().post(Entity.json(duplicateOptions));
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final ApiV4 duplicated = response.readEntity(ApiV4.class);
        assertThat(duplicated.getId()).isEqualTo("duplicate");

        verifyNoInteractions(apiDuplicatorService);
    }

    @Test
    void should_duplicate_v2_api() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = ApiFixtures.aModelApiV2().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicatorService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.mapToV2(duplicateOptions))
            )
        ).thenReturn(ApiFixtures.aModelApiV2().toBuilder().id("duplicate").build());

        final Response response = rootTarget().request().post(Entity.json(duplicateOptions));
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final ApiV2 duplicated = response.readEntity(ApiV2.class);
        assertThat(duplicated.getId()).isEqualTo("duplicate");

        verifyNoInteractions(apiDuplicateService);
    }

    @Test
    void should_duplicate_a_native_api_through_the_import_pipeline() {
        var apiEntity = ApiFixtures.aModelNativeApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(apiEntity);

        var sourceDescriptor = ApiDescriptor.Native.builder()
            .id(API)
            .crossId("source-cross-id")
            .name("source name")
            .apiVersion("1.0")
            .listeners(List.of(KafkaListener.builder().host("source.kafka").port(9092).build()))
            .build();
        // a port-routed plan: its range belongs to the source, so the copy cannot inherit it
        var sourcePlan = PlanDescriptor.Native.builder()
            .id("source-plan")
            .name("source plan")
            .bootstrapPort(9092)
            .brokerRangeStart(10)
            .brokerRangeEnd(20)
            .build();
        when(exportApiUseCase.execute(any())).thenReturn(
            new ExportApiUseCase.Output(GraviteeDefinition.from(sourceDescriptor, null, null, null, Set.of(sourcePlan), null, null, null))
        );
        when(importApiDefinitionUseCase.execute(any())).thenReturn(
            new ImportApiDefinitionUseCase.Output(ApiWithFlows.builder().id("duplicate").build())
        );

        var options = new DuplicateApiOptions()
            .name("copy name")
            .version("2.0")
            .host("copy.kafka")
            .filteredFields(Set.of(DuplicateApiOptions.FilteredFieldsEnum.MEMBERS));

        // the branch answers with a read of the freshly created API, so that read has to resolve too
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), "duplicate", true, true, true)).thenReturn(
            ApiFixtures.aModelNativeApiV4().toBuilder().id("duplicate").build()
        );

        final Response response = rootTarget().request().post(Entity.json(options));
        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(ApiV4.class).getId()).isEqualTo("duplicate");

        var captor = ArgumentCaptor.forClass(ImportApiDefinitionUseCase.Input.class);
        verify(importApiDefinitionUseCase).execute(captor.capture());
        var imported = captor.getValue().importDefinition().getApiExport();
        SoftAssertions.assertSoftly(softly -> {
            // a duplicate is a new API: the platform generates its identifiers
            softly.assertThat(imported.getId()).isNull();
            softly.assertThat(imported.getCrossId()).isNull();
            // the caller owns the copy; it must not inherit the source's primary owner
            softly.assertThat(imported.getPrimaryOwner()).isNull();
            softly.assertThat(imported.getName()).isEqualTo("copy name");
            softly.assertThat(imported.getApiVersion()).isEqualTo("2.0");
            // only the host moves; the port and the rest of the listener are kept
            var listener = (io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener) imported.getListeners().getFirst();
            softly.assertThat(listener.getHost()).isEqualTo("copy.kafka");
            softly.assertThat(listener.getPort()).isEqualTo(9092);
        });

        // the plan keeps everything but its port range, which the source still owns
        SoftAssertions.assertSoftly(softly -> {
            var plan = captor.getValue().importDefinition().getPlans().iterator().next();
            var planDefinition = plan.getPlanDefinitionNativeV4();
            softly.assertThat(planDefinition.getBootstrapPort()).isNull();
            softly.assertThat(planDefinition.getBrokerRangeStart()).isNull();
            softly.assertThat(planDefinition.getBrokerRangeEnd()).isNull();
            softly.assertThat(planDefinition.getName()).isEqualTo("source plan");
        });
    }

    private DuplicateApiOptions aDuplicateApiOptions() {
        return new DuplicateApiOptions().contextPath("/duplicate").filteredFields(Set.of(DuplicateApiOptions.FilteredFieldsEnum.GROUPS));
    }
}
