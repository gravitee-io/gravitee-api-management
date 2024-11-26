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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateNativeApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.UpdateNativeApi;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.notification.model.hook.ApiUpdatedApiHookContext;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.service.common.UuidString;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UpdateNativeApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MY_MEMBER_ID = "my-member-id";
    private static final String MEMBER_EMAIL = "one_valid@email.com";

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService;
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    IndexerInMemory indexer = new IndexerInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);
    ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);
    DataEncryptor dataEncryptor = mock(DataEncryptor.class);

    PropertyDomainService propertyDomainService;
    UpdateNativeApiDomainService updateNativeApiDomainService;

    UpdateNativeApiUseCase cut;

    @BeforeEach
    void setUp() {
        planQueryService = new PlanQueryServiceInMemory(planCrudService);

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group
                    .builder()
                    .id("group-1")
                    .environmentId("environment-id")
                    .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(MY_MEMBER_ID).email(MEMBER_EMAIL).build()));

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        updateNativeApiDomainService =
            new UpdateNativeApiDomainService(
                apiCrudService,
                planQueryService,
                new DeprecatePlanDomainService(planCrudService, auditDomainService),
                triggerNotificationDomainService,
                flowCrudService,
                categoryDomainService,
                auditDomainService,
                new ApiIndexerDomainService(
                    new ApiMetadataDecoderDomainService(
                        new ApiMetadataQueryServiceInMemory(metadataCrudService),
                        new FreemarkerTemplateProcessor()
                    ),
                    apiPrimaryOwnerService,
                    new ApiCategoryQueryServiceInMemory(),
                    indexer
                )
            );

        propertyDomainService = new PropertyDomainService(dataEncryptor);

        cut =
            new UpdateNativeApiUseCase(
                apiPrimaryOwnerService,
                propertyDomainService,
                validateApiDomainService,
                updateNativeApiDomainService
            );
    }

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_throw_exception_if_api_not_found() {
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = anUpdateNativeApi();

        assertThatExceptionOfType(ApiNotFoundException.class)
            .isThrownBy(() -> cut.execute(new UpdateNativeApiUseCase.Input(apiToUpdate, auditInfo)));
    }

    @ParameterizedTest
    @MethodSource("apiValidationInError")
    void should_throw_validation_error(Api existingApi, UpdateNativeApi apiToUpdate) {
        apiCrudService.initWith(List.of(existingApi));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-does-not-exist");

        doThrow(new ValidationDomainException("error in validation"))
            .when(validateApiDomainService)
            .validateAndSanitizeForUpdate(eq(existingApi), any(), any(), eq(ENVIRONMENT_ID), eq(ORGANIZATION_ID));

        assertThatExceptionOfType(ValidationDomainException.class)
            .isThrownBy(() -> cut.execute(new UpdateNativeApiUseCase.Input(apiToUpdate, auditInfo)));
    }

    private static Stream<Arguments> apiValidationInError() {
        return Stream.of(
            Arguments.of(ApiFixtures.aNativeApi().toBuilder().name("old").build(), anUpdateNativeApi()),
            Arguments.of(ApiFixtures.aProxyApiV4(), anUpdateNativeApi()),
            Arguments.of(ApiFixtures.aProxyApiV2(), anUpdateNativeApi()),
            Arguments.of(ApiFixtures.aMessageApiV4(), anUpdateNativeApi()),
            Arguments.of(ApiFixtures.aFederatedApi(), anUpdateNativeApi())
        );
    }

    @Test
    void should_update_api() throws GeneralSecurityException {
        var existingApi = ApiFixtures.aNativeApi();
        apiCrudService.initWith(List.of(existingApi));

        var updateNativeApi = UpdateNativeApi
            .builder()
            .id(existingApi.getId())
            .name("new")
            .definitionVersion(existingApi.getDefinitionVersion())
            .description("new")
            .apiVersion("new")
            .lifecycleState(Api.ApiLifecycleState.UNPUBLISHED)
            .visibility(Api.Visibility.PRIVATE)
            .labels(List.of("new"))
            .categories(Set.of("new"))
            .groups(Set.of("new"))
            .tags(Set.of("new"))
            .resources(List.of(Resource.builder().name("new").build()))
            .listeners(List.of(KafkaListener.builder().host("new").build()))
            .endpointGroups(List.of(NativeEndpointGroup.builder().type("new").build()))
            .flows(List.of(NativeFlow.builder().id("new").build()))
            .services(NativeApiServices.builder().dynamicProperty(Service.builder().type("new").build()).build())
            .properties(
                List.of(
                    EncryptableProperty
                        .builder()
                        .key("encrypt-me")
                        .value("not encrypted")
                        .encryptable(true)
                        .encrypted(false)
                        .dynamic(false)
                        .build()
                )
            )
            .disableMembershipNotifications(true)
            .build();

        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-does-not-exist");

        var apiToUpdate = ApiFixtures
            .aNativeApi()
            .toBuilder()
            .name("new")
            .description("new")
            .version("new")
            .apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED)
            .visibility(Api.Visibility.PRIVATE)
            .labels(List.of("new"))
            .categories(Set.of("new"))
            .groups(Set.of("new"))
            .disableMembershipNotifications(true)
            .apiDefinitionNativeV4(
                existingApi
                    .getApiDefinitionNativeV4()
                    .toBuilder()
                    .name("new")
                    .apiVersion("new")
                    .tags(Set.of("new"))
                    .resources(List.of(Resource.builder().name("new").build()))
                    .listeners(List.of(KafkaListener.builder().host("new").build()))
                    .endpointGroups(List.of(NativeEndpointGroup.builder().type("new").build()))
                    .flows(List.of(NativeFlow.builder().id("new").build()))
                    .services(NativeApiServices.builder().dynamicProperty(Service.builder().type("new").build()).build())
                    .properties(List.of(Property.builder().key("encrypt-me").value("new").encrypted(true).dynamic(false).build()))
                    .build()
            )
            .build();

        when(dataEncryptor.encrypt(eq("not encrypted"))).thenReturn("new");

        when(
            validateApiDomainService.validateAndSanitizeForUpdate(
                eq(existingApi),
                eq(apiToUpdate),
                any(),
                eq(ENVIRONMENT_ID),
                eq(ORGANIZATION_ID)
            )
        )
            .thenReturn(apiToUpdate);

        when(categoryDomainService.toCategoryKey(eq(apiToUpdate), eq(ENVIRONMENT_ID))).thenReturn(Set.of("new-key"));

        var output = cut.execute(new UpdateNativeApiUseCase.Input(updateNativeApi, auditInfo));

        assertThat(output.updatedApi())
            .satisfies(api -> {
                assertThat(api.getId()).isEqualTo(existingApi.getId());
                assertThat(api.getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
                assertThat(api.getName()).isEqualTo("new");
                assertThat(api.getDescription()).isEqualTo("new");
                assertThat(api.getVersion()).isEqualTo("new");
                assertThat(api.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.UNPUBLISHED);
                assertThat(api.getVisibility()).isEqualTo(Api.Visibility.PRIVATE);
                assertThat(api.getLabels()).containsExactly("new");
                assertThat(api.getCategories()).containsExactly("new-key");
                assertThat(api.getGroups()).containsExactly("new");
                assertThat(api.isDisableMembershipNotifications()).isEqualTo(true);
            })
            .extracting(Api::getApiDefinitionNativeV4)
            .satisfies(definition -> {
                assertThat(definition.getName()).isEqualTo("new");
                assertThat(definition.getApiVersion()).isEqualTo("new");
                assertThat(definition.getTags()).containsExactly("new");
                assertThat(definition.getResources()).containsExactly(Resource.builder().name("new").build());
                assertThat(definition.getListeners()).containsExactly(KafkaListener.builder().host("new").build());
                assertThat(definition.getEndpointGroups()).containsExactly(NativeEndpointGroup.builder().type("new").build());
                assertThat(definition.getFlows()).containsExactly(NativeFlow.builder().id("new").build());
                assertThat(definition.getServices())
                    .isEqualTo(NativeApiServices.builder().dynamicProperty(Service.builder().type("new").build()).build());
                assertThat(definition.getProperties())
                    .isNotNull()
                    .containsExactly(Property.builder().key("encrypt-me").value("new").encrypted(true).dynamic(false).build());
            });

        assertThat(flowCrudService.storage()).containsExactly(NativeFlow.builder().id("new").build());
        verify(categoryDomainService, times(1)).updateOrderCategoriesOfApi(eq(existingApi.getId()), eq(Set.of("new")));
        assertThat(auditCrudService.storage()).hasSize(1);
        assertThat(triggerNotificationDomainService.getApiNotifications())
            .containsExactly(new ApiUpdatedApiHookContext(existingApi.getId()));
        verify(categoryDomainService, times(1)).toCategoryKey(eq(apiToUpdate), eq(ENVIRONMENT_ID));
        assertThat(indexer.storage()).hasSize(1);
    }

    private static UpdateNativeApi anUpdateNativeApi() {
        return UpdateNativeApi
            .builder()
            .id(API_ID)
            .name("NAME")
            .description("DESCRIPTION")
            .type(ApiType.NATIVE)
            .definitionVersion(DefinitionVersion.V4)
            .tags(Set.of("tag1"))
            .listeners(
                List.of(
                    KafkaListener
                        .builder()
                        .host("native.kafka")
                        .port(1000)
                        .entrypoints(List.of(NativeEntrypoint.builder().type("native-type").configuration("{}").build()))
                        .build()
                )
            )
            .endpointGroups(
                List.of(
                    NativeEndpointGroup
                        .builder()
                        .name("default-group")
                        .type("mock")
                        .sharedConfiguration("{}")
                        .endpoints(
                            List.of(
                                NativeEndpoint
                                    .builder()
                                    .name("default-endpoint")
                                    .type("mock")
                                    .inheritConfiguration(true)
                                    .configuration("{}")
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .flows(List.of())
            .properties(
                List.of(
                    EncryptableProperty
                        .builder()
                        .key("encrypted")
                        .value("encrypted value")
                        .encryptable(true)
                        .encrypted(true)
                        .dynamic(false)
                        .build()
                )
            )
            .build();
    }
}
