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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fakes.FakeApiImagesService;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportDefinitionUpdateDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String TARGET_ENVIRONMENT_ID = "target-environment-id";
    private static final String USER = "user";
    private static final String USER_EMAIL = "example@mail.com";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(TARGET_ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER).build())
        .build();
    private static final String PROMOTED_API_ID = "promoted-api-id";
    private static final String PROMOTED_API_CROSS_ID = "promoted-api-cross-id";

    private final ImportDefinitionUpdateDomainServiceTestInitializer importDefinitionUpdateInitializer =
        new ImportDefinitionUpdateDomainServiceTestInitializer();

    private final ApiService apiService = importDefinitionUpdateInitializer.apiService;
    private final FakeApiImagesService apiImagesService = importDefinitionUpdateInitializer.apiImagesServiceProvider;
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = importDefinitionUpdateInitializer.apiCrudServiceInMemory;
    private final ApiQueryServiceInMemory apiQueryServiceInMemory = importDefinitionUpdateInitializer.apiQueryServiceInMemory;

    private final ImportDefinitionUpdateDomainService service = importDefinitionUpdateInitializer.initialize(TARGET_ENVIRONMENT_ID);

    @BeforeEach
    void setUp() {
        importDefinitionUpdateInitializer.roleQueryServiceInMemory.resetSystemRoles(ORGANIZATION_ID);
        importDefinitionUpdateInitializer.membershipQueryServiceInMemory.initWith(
            List.of(
                Membership.builder()
                    .id("member-id")
                    .memberId(USER)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(PROMOTED_API_ID)
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        importDefinitionUpdateInitializer.userCrudServiceInMemory.initWith(
            List.of(BaseUserEntity.builder().id(USER).email(USER_EMAIL).build())
        );
    }

    @AfterEach
    void tearDown() {
        importDefinitionUpdateInitializer.tearDown();
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
        assertThat(apiImagesService.apiPictures.get(PROMOTED_API_ID)).isEqualTo(picture);
        assertThat(apiImagesService.apiBackgrounds.get(PROMOTED_API_ID)).isEqualTo(background);
    }

    @Test
    public void should_preserve_existing_properties_when_proxy_api_updated_with_oas_import_having_no_properties() {
        List<Property> existingProperties = new ArrayList<>(
            List.of(Property.builder().key("existing-key").value("existing-value").build())
        );
        var baseProxyApi = ApiFixtures.aProxyApiV4();
        ((io.gravitee.definition.model.v4.Api) baseProxyApi.getApiDefinitionValue()).setProperties(existingProperties);
        var existingApi = baseProxyApi
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        // OAS import produces no Gravitee-specific properties
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).name("updated name").build())
            .build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        assertThat(updated).isNotNull();
        assertThat(((io.gravitee.definition.model.v4.Api) updated.getApiDefinitionValue()).getProperties())
            .usingRecursiveComparison()
            .isEqualTo(existingProperties);
    }

    @Test
    public void should_preserve_existing_properties_when_native_api_updated_with_oas_import_having_no_properties() {
        List<Property> existingProperties = new ArrayList<>(
            List.of(Property.builder().key("existing-key").value("existing-value").build())
        );
        var baseNativeApi = ApiFixtures.aNativeApi();
        ((NativeApi) baseNativeApi.getApiDefinitionValue()).setProperties(existingProperties);
        var existingApi = baseNativeApi
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        var apiExport = ApiExport.builder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .name("updated name")
            .type(ApiType.NATIVE)
            // No properties — simulating OAS import
            .build();

        when(
            importDefinitionUpdateInitializer.validateApiDomainService.validateAndSanitizeForUpdate(
                any(),
                any(),
                any(),
                eq(TARGET_ENVIRONMENT_ID),
                eq(ORGANIZATION_ID)
            )
        ).thenAnswer(invocation -> invocation.getArgument(1));

        var importDefinition = ImportDefinition.builder().apiExport(apiExport).build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        assertThat(updated).isNotNull();
        assertThat(updated.getApiDefinitionNativeV4().getProperties()).usingRecursiveComparison().isEqualTo(existingProperties);
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

    @Test
    public void should_update_v4_native_api() {
        var picture = "picture";
        var background = "background";
        var existingApi = ApiFixtures.aNativeApi()
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        var apiExport = ApiExport.builder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .name("updated name")
            .description("updated description")
            .apiVersion("updated version")
            .visibility(Visibility.PUBLIC)
            .labels(List.of("label updated"))
            .disableMembershipNotifications(true)
            .picture(picture)
            .background(background)
            .properties(List.of(Property.builder().key("foo").value("bar").encrypted(true).dynamic(false).build()))
            .resources(List.of(Resource.builder().name("updated resource").build()))
            .listeners(List.of(KafkaListener.builder().host("updated listener").build()))
            .endpointGroups(List.of(NativeEndpointGroup.builder().type("updated group").build()))
            .tags(Set.of("tag updated"))
            .type(ApiType.NATIVE)
            .build();

        when(
            importDefinitionUpdateInitializer.validateApiDomainService.validateAndSanitizeForUpdate(
                any(),
                any(),
                any(),
                eq(TARGET_ENVIRONMENT_ID),
                eq(ORGANIZATION_ID)
            )
        ).thenReturn(ApiModelFactory.fromApiExport(apiExport, TARGET_ENVIRONMENT_ID));

        var importDefinition = ImportDefinition.builder().apiExport(apiExport).build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        assertThat(updated).isNotNull();
        var sanitizationCaptor = ArgumentCaptor.forClass(Api.class);
        verify(importDefinitionUpdateInitializer.validateApiDomainService).validateAndSanitizeForUpdate(
            any(),
            sanitizationCaptor.capture(),
            any(),
            eq(TARGET_ENVIRONMENT_ID),
            eq(ORGANIZATION_ID)
        );

        var capturedApi = sanitizationCaptor.getValue();
        assertNativeApiMatchExport(capturedApi, apiExport);

        assertThat(apiImagesService.apiPictures.get(PROMOTED_API_ID)).isEqualTo(picture);
        assertThat(apiImagesService.apiBackgrounds.get(PROMOTED_API_ID)).isEqualTo(background);
        assertNativeApiMatchExport(updated, apiExport);
    }

    @Nested
    class PlansUpsert {

        private final PlanWithFlows anApiKeyPlan = PlanWithFlows.builder()
            .crossId("api-key-cross-id")
            .name("API Key Plan")
            .apiId(PROMOTED_API_ID)
            .type(Plan.PlanType.API)
            .referenceType(Plan.ReferenceType.API)
            .referenceId(PROMOTED_API_ID)
            .environmentId(TARGET_ENVIRONMENT_ID)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .security(PlanSecurity.builder().type("api-key").build())
                    .mode(PlanMode.STANDARD)
                    .build()
            )
            .flows(List.of())
            .build();

        @BeforeEach
        void stubApiService() {
            when(apiService.update(any(), eq(PROMOTED_API_ID), any(), any(Boolean.class), any())).thenAnswer(inv ->
                io.gravitee.rest.api.model.v4.api.ApiEntity.builder().id(PROMOTED_API_ID).name("api name").apiVersion("1.0").build()
            );
        }

        @Test
        void should_create_plan_when_not_present_in_saved_plans() {
            var existingApi = ApiFixtures.aProxyApiV4()
                .toBuilder()
                .id(PROMOTED_API_ID)
                .crossId(PROMOTED_API_CROSS_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .build();
            apiCrudServiceInMemory.initWith(List.of(existingApi));
            apiQueryServiceInMemory.initWith(List.of(existingApi));
            // no saved plans

            var importDefinition = ImportDefinition.builder()
                .apiExport(ApiExport.builder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).name("api name").build())
                .plans(Set.of(anApiKeyPlan))
                .build();

            service.update(importDefinition, existingApi, AUDIT_INFO);

            var savedPlans = importDefinitionUpdateInitializer.planDomainServiceInitializer.planCrudServiceInMemory.findByApiId(
                PROMOTED_API_ID
            );
            assertThat(savedPlans)
                .hasSize(1)
                .first()
                .satisfies(p -> {
                    assertThat(p.getCrossId()).isEqualTo("api-key-cross-id");
                    assertThat(p.getName()).isEqualTo("API Key Plan");
                    assertThat(p.getApiId()).isEqualTo(PROMOTED_API_ID);
                });
        }

        @Test
        void should_update_plan_when_matching_by_crossId() {
            var existingApi = ApiFixtures.aProxyApiV4()
                .toBuilder()
                .id(PROMOTED_API_ID)
                .crossId(PROMOTED_API_CROSS_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .build();
            apiCrudServiceInMemory.initWith(List.of(existingApi));
            apiQueryServiceInMemory.initWith(List.of(existingApi));

            var savedPlan = PlanFixtures.HttpV4.anApiKey()
                .toBuilder()
                .crossId("api-key-cross-id")
                .apiId(PROMOTED_API_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(PROMOTED_API_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .name("Old Name")
                .build();
            importDefinitionUpdateInitializer.planDomainServiceInitializer.planCrudServiceInMemory.initWith(List.of(savedPlan));
            importDefinitionUpdateInitializer.planDomainServiceInitializer.planQueryServiceInMemory.initWith(List.of(savedPlan));

            var incomingPlan = anApiKeyPlan.toBuilder().id(savedPlan.getId()).name("Updated Name").build();
            var importDefinition = ImportDefinition.builder()
                .apiExport(ApiExport.builder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).name("api name").build())
                .plans(Set.of(incomingPlan))
                .build();

            service.update(importDefinition, existingApi, AUDIT_INFO);

            var savedPlans = importDefinitionUpdateInitializer.planDomainServiceInitializer.planCrudServiceInMemory.findByApiId(
                PROMOTED_API_ID
            );
            assertThat(savedPlans)
                .hasSize(1)
                .first()
                .satisfies(p -> {
                    assertThat(p.getId()).isEqualTo(savedPlan.getId());
                    assertThat(p.getCrossId()).isEqualTo("api-key-cross-id");
                    assertThat(p.getName()).isEqualTo("Updated Name");
                });
        }

        @Test
        void should_delete_plan_absent_from_import_definition() {
            var existingApi = ApiFixtures.aProxyApiV4()
                .toBuilder()
                .id(PROMOTED_API_ID)
                .crossId(PROMOTED_API_CROSS_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .build();
            apiCrudServiceInMemory.initWith(List.of(existingApi));
            apiQueryServiceInMemory.initWith(List.of(existingApi));

            var planToKeep = PlanFixtures.HttpV4.anApiKey()
                .toBuilder()
                .crossId("api-key-cross-id")
                .apiId(PROMOTED_API_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(PROMOTED_API_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .build();
            var planToDelete = PlanFixtures.HttpV4.aKeyless()
                .toBuilder()
                .crossId("keyless-to-delete")
                .apiId(PROMOTED_API_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(PROMOTED_API_ID)
                .environmentId(TARGET_ENVIRONMENT_ID)
                .build();
            importDefinitionUpdateInitializer.planDomainServiceInitializer.planCrudServiceInMemory.initWith(
                List.of(planToKeep, planToDelete)
            );
            importDefinitionUpdateInitializer.planDomainServiceInitializer.planQueryServiceInMemory.initWith(
                List.of(planToKeep, planToDelete)
            );

            // import only contains planToKeep — planToDelete is absent
            var importDefinition = ImportDefinition.builder()
                .apiExport(ApiExport.builder().id(PROMOTED_API_ID).crossId(PROMOTED_API_CROSS_ID).name("api name").build())
                .plans(Set.of(anApiKeyPlan.toBuilder().id(planToKeep.getId()).build()))
                .build();

            service.update(importDefinition, existingApi, AUDIT_INFO);

            var savedPlans = importDefinitionUpdateInitializer.planDomainServiceInitializer.planCrudServiceInMemory.findByApiId(
                PROMOTED_API_ID
            );
            assertThat(savedPlans).hasSize(1).extracting(Plan::getCrossId).containsOnly("api-key-cross-id");
        }
    }

    private static void assertNativeApiMatchExport(Api api, ApiExport apiExport) {
        assertThat(api.getName()).isEqualTo(apiExport.getName());
        assertThat(api.getDescription()).isEqualTo(apiExport.getDescription());
        assertThat(api.getVersion()).isEqualTo(apiExport.getApiVersion());
        assertThat(api.getVisibility()).isEqualTo(Api.Visibility.PUBLIC);
        assertThat(api.getLabels()).containsExactlyElementsOf(apiExport.getLabels());
        assertThat(api.getTags()).containsExactlyInAnyOrderElementsOf(apiExport.getTags());
        assertThat(api.isDisableMembershipNotifications()).isEqualTo(apiExport.isDisableMembershipNotifications());
        assertThat(api.getType()).isEqualTo(apiExport.getType());

        var definition = api.getApiDefinitionNativeV4();
        assertThat(definition.getProperties()).usingRecursiveComparison().isEqualTo(apiExport.getProperties());
        assertThat(definition.getResources()).usingRecursiveComparison().isEqualTo(apiExport.getResources());
        assertThat(definition.getListeners()).usingRecursiveComparison().isEqualTo(apiExport.getListeners());
        assertThat(definition.getEndpointGroups()).usingRecursiveComparison().isEqualTo(apiExport.getEndpointGroups());
        assertThat(definition.getFlows()).usingRecursiveComparison().isEqualTo(apiExport.getFlows());
        assertThat(definition.getTags()).containsExactlyElementsOf(apiExport.getTags());
        assertThat(definition.getApiVersion()).isEqualTo(apiExport.getApiVersion());
    }
}
