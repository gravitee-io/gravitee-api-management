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
package io.gravitee.rest.api.management.v2.rest.resource.promotions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PromotionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PromotionCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.use_case.ProcessPromotionUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class PromotionsResourceTest extends AbstractResourceTest {

    private static final String PROMOTION_ID = "promotion-id";
    private static final String API_ID = "api-id";
    private static final String API_CROSS_ID = "api-cross-id";
    private static final String BASE64_PICTURE = "data:image/jpeg;base64,picture...";
    private static final String DEFAULT_ENV_ID = "default-env-id";
    private static final String TARGET_ENV_ID = "target-env-id";
    private static final String TARGET_ENV_COCKPIT_ID = "target-env-cockpit-id";
    private WebTarget target;

    @Autowired
    private ProcessPromotionUseCase processPromotionUseCase;

    @Autowired
    private PromotionCrudServiceInMemory promotionCrudServiceInMemory;

    @Autowired
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    private EnvironmentCrudServiceInMemory environmentCrudServiceInMemory;

    @Autowired
    private GraviteeDefinitionSerializer graviteeDefinitionSerializer;

    @BeforeEach
    public void init() {
        environmentCrudServiceInMemory.initWith(
            List.of(Environment.builder().id(TARGET_ENV_ID).cockpitId(TARGET_ENV_COCKPIT_ID).organizationId(ORGANIZATION).build())
        );
        target = rootTarget();
    }

    @AfterEach
    public void clean() {
        Stream.of(apiCrudServiceInMemory, promotionCrudServiceInMemory, environmentCrudServiceInMemory).forEach(InMemoryAlternative::reset);
        reset(processPromotionUseCase, permissionService);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/promotions/" + PROMOTION_ID + "/_process";
    }

    @Test
    @SneakyThrows
    void should_map_exported_definition_to_import_definition() {
        var export = GraviteeDefinition.from(
            ApiDescriptor.ApiDescriptorV4.builder().id(API_ID).crossId(API_CROSS_ID).build(),
            Collections.emptySet(),
            List.of(NewApiMetadata.builder().apiId(API_ID).name("metadata").value("metadata-value").build()),
            List.of(PageExport.builder().id("page-id").build()),
            List.of(PlanDescriptor.V4.builder().id("plan-id").build()),
            Collections.emptyList(),
            BASE64_PICTURE,
            BASE64_PICTURE
        );
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).crossId(API_CROSS_ID).environmentId(DEFAULT_ENV_ID).build()));

        var promotion = PromotionFixtures.aPromotion()
            .toBuilder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .status(PromotionStatus.TO_BE_VALIDATED)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(graviteeDefinitionSerializer.serialize(export))
            .build();
        promotionCrudServiceInMemory.initWith(List.of(promotion));

        when(processPromotionUseCase.execute(any())).thenReturn(
            new ProcessPromotionUseCase.Output(PromotionFixtures.aPromotion().toBuilder().status(PromotionStatus.REJECTED).build())
        );

        Response response = target.request().post(Entity.json(false));

        var captor = ArgumentCaptor.forClass(ProcessPromotionUseCase.Input.class);
        verify(processPromotionUseCase).execute(captor.capture());

        var inputValue = captor.getValue();
        assertThat(inputValue)
            .isNotNull()
            .satisfies(input -> {
                assertThat(input.promotion()).isEqualTo(promotion);
                assertThat(input.definitionVersion()).isEqualTo(DefinitionVersion.V4);
                assertThat(input.importDefinition()).isNull();
                assertThat(input.isAccepted()).isFalse();
                assertThat(input.auditInfo().environmentId()).isEqualTo(TARGET_ENV_ID);
            });

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    @Test
    @SneakyThrows
    void should_call_use_case_with_promotion_and_v2_definition_version_without_mapping_exported_definition() {
        var promotion = PromotionFixtures.aPromotion()
            .toBuilder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .status(PromotionStatus.TO_BE_VALIDATED)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition("{ \"gravitee\" : \"2.0.0\", \"id\" : \"api-id\", \"crossId\" : \"api-cross-id\" }")
            .build();
        promotionCrudServiceInMemory.initWith(List.of(promotion));
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).crossId(API_CROSS_ID).environmentId(DEFAULT_ENV_ID).build()));

        when(processPromotionUseCase.execute(any())).thenReturn(
            new ProcessPromotionUseCase.Output(PromotionFixtures.aPromotion().toBuilder().status(PromotionStatus.ACCEPTED).build())
        );

        Response response = target.request().post(Entity.json(true));

        var captor = ArgumentCaptor.forClass(ProcessPromotionUseCase.Input.class);
        verify(processPromotionUseCase).execute(captor.capture());

        var inputValue = captor.getValue();
        assertThat(inputValue)
            .isNotNull()
            .satisfies(input -> {
                assertThat(input.promotion()).isEqualTo(promotion);
                assertThat(input.definitionVersion()).isEqualTo(DefinitionVersion.V2);
                assertThat(input.importDefinition()).isNull();
                assertThat(input.isAccepted()).isTrue();
                assertThat(input.auditInfo()).isNull();
            });

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    @Test
    void should_reject_a_v2_promotion_when_the_caller_has_rights() {
        var promotion = PromotionFixtures.aPromotion()
            .toBuilder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .status(PromotionStatus.TO_BE_VALIDATED)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition("{ \"gravitee\" : \"2.0.0\", \"id\" : \"api-id\", \"crossId\" : \"api-cross-id\" }")
            .build();
        promotionCrudServiceInMemory.initWith(List.of(promotion));
        when(processPromotionUseCase.execute(any())).thenReturn(
            new ProcessPromotionUseCase.Output(promotion.toBuilder().status(PromotionStatus.REJECTED).build())
        );

        Response response = target.request().post(Entity.json(false));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(processPromotionUseCase).execute(argThat(input -> !input.isAccepted() && input.definitionVersion() == DefinitionVersion.V2));
        assertThat(apiCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_accept_a_v4_promotion_that_updates_an_existing_api_when_the_caller_has_update_right() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        apiCrudServiceInMemory.initWith(
            List.of(
                Api.builder()
                    .id("target-api")
                    .crossId(API_CROSS_ID)
                    .environmentId(TARGET_ENV_ID)
                    .definitionVersion(DefinitionVersion.V4)
                    .build()
            )
        );
        givenUseCaseSucceeds();

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(processPromotionUseCase).execute(
            argThat(input -> input.isAccepted() && "target-api".equals(input.existingPromotedApi().getId()))
        );
        verify(permissionService).hasPermission(
            argThat(context -> TARGET_ENV_ID.equals(context.getEnvironmentId())),
            eq(RolePermission.ENVIRONMENT_API),
            eq(TARGET_ENV_ID),
            eq(RolePermissionAction.UPDATE)
        );
    }

    @Test
    void should_check_rights_on_the_target_environment_when_accepting_a_v4_promotion() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        givenUseCaseSucceeds();

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(permissionService, times(2)).hasPermission(
            argThat(context -> TARGET_ENV_ID.equals(context.getEnvironmentId()) && ORGANIZATION.equals(context.getOrganizationId())),
            eq(RolePermission.ENVIRONMENT_API),
            eq(TARGET_ENV_ID),
            eq(RolePermissionAction.CREATE)
        );
    }

    @Test
    void should_return_403_when_accepting_a_v4_promotion_that_creates_an_api_without_create_right() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        givenNoRightOnTargetEnvironment(RolePermissionAction.CREATE);

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(processPromotionUseCase, never()).execute(any());
        assertThat(apiCrudServiceInMemory.storage()).isEmpty();
        assertThat(promotionCrudServiceInMemory.storage())
            .extracting(promotion -> promotion.getStatus())
            .containsExactly(PromotionStatus.TO_BE_VALIDATED);
    }

    @Test
    void should_return_403_when_accepting_a_v4_promotion_that_updates_an_api_without_update_right() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        apiCrudServiceInMemory.initWith(
            List.of(
                Api.builder()
                    .id("target-api")
                    .crossId(API_CROSS_ID)
                    .environmentId(TARGET_ENV_ID)
                    .definitionVersion(DefinitionVersion.V4)
                    .build()
            )
        );
        givenNoRightOnTargetEnvironment(RolePermissionAction.UPDATE);

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(processPromotionUseCase, never()).execute(any());
        assertThat(apiCrudServiceInMemory.storage())
            .singleElement()
            .satisfies(api -> {
                assertThat(api.getId()).isEqualTo("target-api");
                assertThat(api.getCrossId()).isEqualTo(API_CROSS_ID);
            });
    }

    @Test
    void should_return_403_when_rejecting_a_v4_promotion_without_right_on_the_target_environment() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        givenNoRightOnTargetEnvironment(RolePermissionAction.CREATE);
        givenNoRightOnTargetEnvironment(RolePermissionAction.UPDATE);

        Response response = target.request().post(Entity.json(false));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(processPromotionUseCase, never()).execute(any());
    }

    @Test
    void should_reject_a_v4_promotion_when_the_caller_has_only_update_right() {
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);
        givenNoRightOnTargetEnvironment(RolePermissionAction.CREATE);
        givenUseCaseSucceeds();

        Response response = target.request().post(Entity.json(false));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(processPromotionUseCase).execute(argThat(input -> !input.isAccepted()));
    }

    @Test
    void should_return_404_when_the_promotion_targets_an_environment_of_another_organization() {
        environmentCrudServiceInMemory.reset();
        environmentCrudServiceInMemory.initWith(
            List.of(Environment.builder().id(TARGET_ENV_ID).cockpitId(TARGET_ENV_COCKPIT_ID).organizationId("another-org").build())
        );
        givenAV4Promotion(PromotionStatus.TO_BE_VALIDATED);

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(processPromotionUseCase, never()).execute(any());
    }

    @Test
    void should_return_403_when_the_caller_has_no_right_on_the_target_environment() {
        givenAV4Promotion(PromotionStatus.ACCEPTED);
        givenNoRightOnTargetEnvironment(RolePermissionAction.CREATE);
        givenNoRightOnTargetEnvironment(RolePermissionAction.UPDATE);

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(processPromotionUseCase, never()).execute(any());
    }

    @Test
    void should_return_400_when_the_promotion_was_already_processed() {
        givenAV4Promotion(PromotionStatus.ACCEPTED);

        Response response = target.request().post(Entity.json(true));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        verify(processPromotionUseCase, never()).execute(any());
    }

    @SneakyThrows
    private void givenAV4Promotion(PromotionStatus status) {
        var export = GraviteeDefinition.from(
            ApiDescriptor.ApiDescriptorV4.builder().id(API_ID).crossId(API_CROSS_ID).build(),
            Collections.emptySet(),
            List.of(),
            List.of(),
            List.of(),
            Collections.emptyList(),
            BASE64_PICTURE,
            BASE64_PICTURE
        );
        promotionCrudServiceInMemory.initWith(
            List.of(
                PromotionFixtures.aPromotion()
                    .toBuilder()
                    .id(PROMOTION_ID)
                    .apiId(API_ID)
                    .status(status)
                    .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
                    .apiDefinition(graviteeDefinitionSerializer.serialize(export))
                    .build()
            )
        );
    }

    private void givenUseCaseSucceeds() {
        when(processPromotionUseCase.execute(any())).thenReturn(
            new ProcessPromotionUseCase.Output(PromotionFixtures.aPromotion().toBuilder().status(PromotionStatus.ACCEPTED).build())
        );
    }

    private void givenNoRightOnTargetEnvironment(RolePermissionAction action) {
        when(
            permissionService.hasPermission(any(ExecutionContext.class), eq(RolePermission.ENVIRONMENT_API), eq(TARGET_ENV_ID), eq(action))
        ).thenReturn(false);
    }
}
