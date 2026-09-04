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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fakes.FakeApiImagesService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
    public void should_update_the_given_api_when_definition_has_no_id_and_cross_id_does_not_match() {
        // Promotion case: definition exported without ids, target API resolved by another mean than crossId.
        var existingApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId("cross-id-of-the-existing-api")
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().crossId(PROMOTED_API_CROSS_ID).name("updated name").build())
            .build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(PROMOTED_API_ID);
        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
        verify(apiService).update(
            eq(new ExecutionContext(ORGANIZATION_ID, TARGET_ENVIRONMENT_ID)),
            eq(PROMOTED_API_ID),
            // The crossId of the definition is applied so that the next promotion is resolved by crossId again.
            argThat(update -> "updated name".equals(update.getName()) && PROMOTED_API_CROSS_ID.equals(update.getCrossId())),
            eq(false),
            eq(USER)
        );
    }

    @Test
    public void should_update_the_given_api_when_definition_carries_the_id_of_another_api() {
        var existingApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(PROMOTED_API_ID)
            .crossId(PROMOTED_API_CROSS_ID)
            .environmentId(TARGET_ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(existingApi));
        apiQueryServiceInMemory.initWith(List.of(existingApi));

        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().id("source-api-id").crossId(PROMOTED_API_CROSS_ID).name("updated name").build())
            .build();

        var updated = service.update(importDefinition, existingApi, AUDIT_INFO);

        assertThat(updated.getId()).isEqualTo(PROMOTED_API_ID);
        verify(apiService).update(any(), eq(PROMOTED_API_ID), any(), eq(false), eq(USER));
        verify(apiService, never()).update(any(), eq("source-api-id"), any(), anyBoolean(), any());
    }

    @Test
    public void should_fail_fast_when_no_existing_api_is_given() {
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().crossId(PROMOTED_API_CROSS_ID).name("updated name").build())
            .build();

        var throwable = catchThrowable(() -> service.update(importDefinition, null, AUDIT_INFO));

        assertThat(throwable).isInstanceOf(NullPointerException.class);
        verify(apiService, never()).update(any(), any(), any(), anyBoolean(), any());
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
