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

import static fixtures.core.model.RoleFixtures.apiPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.NewApiFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.CreateHttpApiUseCase.Input;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CreateHttpApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String GROUP_ID = "group-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ValidateApiDomainService validateApiDomainService;

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();

    CreateHttpApiUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        validateApiDomainService = mock(ValidateApiDomainService.class);

        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var createApiDomainService = new CreateApiDomainService(
            apiCrudService,
            auditService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            ),
            new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryService, auditService),
            apiPrimaryOwnerDomainService,
            flowCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService
        );
        useCase = new CreateHttpApiUseCase(validateApiDomainService, apiPrimaryOwnerFactory, createApiDomainService);

        when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        enableApiPrimaryOwnerMode(ApiPrimaryOwnerMode.USER);
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                auditCrudService,
                flowCrudService,
                groupQueryService,
                membershipCrudService,
                metadataCrudService,
                notificationConfigCrudService,
                parametersQueryService,
                userCrudService,
                workflowCrudService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_when_api_is_invalid() {
        // Given
        when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
            .thenThrow(new ValidationDomainException("Definition version is unsupported, should be V4 or higher"));
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new Input(newApi, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_throw_when_api_type_is_invalid() {
        // Given
        var newApi = NewApiFixtures.aProxyApiV4().toBuilder().type(ApiType.NATIVE).build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new Input(newApi, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiInvalidTypeException.class);
    }

    @Test
    void should_create_and_index_a_new_api() {
        // Given
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        var output = useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        var expectedApi = newApi
            .toApiBuilder()
            .id("generated-id")
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .environmentId(ENVIRONMENT_ID)
            .apiLifecycleState(Api.ApiLifecycleState.CREATED)
            .lifecycleState(Api.LifecycleState.STOPPED)
            .visibility(Api.Visibility.PRIVATE)
            .apiDefinitionHttpV4(newApi.toApiDefinitionBuilder().id("generated-id").build())
            .build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(output.api()).isEqualTo(new ApiWithFlows(expectedApi, newApi.getFlows()));
            soft.assertThat(apiCrudService.storage()).contains(expectedApi);
            soft
                .assertThat(indexer.storage())
                .containsExactly(
                    new IndexableApi(
                        expectedApi,
                        new PrimaryOwnerEntity(USER_ID, "jane.doe@gravitee.io", "Jane Doe", PrimaryOwnerEntity.Type.USER),
                        Map.ofEntries(Map.entry("email-support", "jane.doe@gravitee.io")),
                        Collections.emptySet()
                    )
                );
        });
    }

    @Test
    void should_create_an_audit() {
        // Given
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                // API Audit
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId("generated-id")
                    .user(USER_ID)
                    .properties(Collections.emptyMap())
                    .event(ApiAuditEvent.API_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build(),
                // Membership Audit
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId("generated-id")
                    .user(USER_ID)
                    .properties(Map.of("USER", USER_ID))
                    .event(MembershipAuditEvent.MEMBERSHIP_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build(),
                // Metadata Audit
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId("generated-id")
                    .user(USER_ID)
                    .properties(Map.of("METADATA", "email-support"))
                    .event(ApiAuditEvent.METADATA_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @ParameterizedTest
    @EnumSource(value = ApiPrimaryOwnerMode.class, mode = EnumSource.Mode.INCLUDE, names = { "USER", "HYBRID" })
    void should_create_primary_owner_membership_when_user_or_hybrid_mode_is_enabled(ApiPrimaryOwnerMode mode) {
        // Given
        enableApiPrimaryOwnerMode(mode);
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(membershipCrudService.storage())
            .contains(
                Membership
                    .builder()
                    .id("generated-id")
                    .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceId("generated-id")
                    .referenceType(Membership.ReferenceType.API)
                    .source("system")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_create_primary_owner_membership_when_group_mode_is_enabled() {
        // Given
        enableApiPrimaryOwnerMode(ApiPrimaryOwnerMode.GROUP);
        givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("Group name").apiPrimaryOwner(USER_ID).build()));
        givenExistingMemberships(
            List.of(
                Membership
                    .builder()
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId(GROUP_ID)
                    .memberType(Membership.Type.USER)
                    .memberId(USER_ID)
                    .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                    .build()
            )
        );
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(membershipCrudService.storage())
            .contains(
                Membership
                    .builder()
                    .id("generated-id")
                    .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                    .memberId(GROUP_ID)
                    .memberType(Membership.Type.GROUP)
                    .referenceId("generated-id")
                    .referenceType(Membership.ReferenceType.API)
                    .source("system")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_create_default_email_notification_configuration() {
        // Given
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(notificationConfigCrudService.storage())
            .containsExactly(
                NotificationConfig
                    .builder()
                    .id("generated-id")
                    .type(NotificationConfig.Type.GENERIC)
                    .name("Default Mail Notifications")
                    .referenceType("API")
                    .referenceId("generated-id")
                    .hooks(
                        List.of(
                            "APIKEY_EXPIRED",
                            "APIKEY_RENEWED",
                            "APIKEY_REVOKED",
                            "API_DEPLOYED",
                            "API_DEPRECATED",
                            "API_STARTED",
                            "API_STOPPED",
                            "API_UPDATED",
                            "ASK_FOR_REVIEW",
                            "MESSAGE",
                            "NEW_RATING",
                            "NEW_RATING_ANSWER",
                            "NEW_SPEC_GENERATED",
                            "NEW_SUPPORT_TICKET",
                            "REQUEST_FOR_CHANGES",
                            "REVIEW_OK",
                            "SUBSCRIPTION_ACCEPTED",
                            "SUBSCRIPTION_CLOSED",
                            "SUBSCRIPTION_NEW",
                            "SUBSCRIPTION_PAUSED",
                            "SUBSCRIPTION_REJECTED",
                            "SUBSCRIPTION_RESUMED",
                            "SUBSCRIPTION_TRANSFERRED"
                        )
                    )
                    .notifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID)
                    .config("${(api.primaryOwner.email)!''}")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_create_default_api_metadata() {
        // Given
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(metadataCrudService.storage())
            .containsExactly(
                Metadata
                    .builder()
                    .key("email-support")
                    .format(Metadata.MetadataFormat.MAIL)
                    .name(MetadataService.METADATA_EMAIL_SUPPORT_KEY)
                    .value("${(api.primaryOwner.email)!''}")
                    .referenceType(Metadata.ReferenceType.API)
                    .referenceId("generated-id")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_save_all_flows() {
        // Given
        List<Flow> flows = List.of(Flow.builder().name("flow").selectors(List.of(new HttpSelector())).build());
        var newApi = NewApiFixtures.aProxyApiV4().toBuilder().flows(flows).build();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(flowCrudService.storage()).containsExactlyElementsOf(flows);
    }

    @Test
    void should_create_an_api_review_workflow_when_api_review_is_enabled() {
        // Given
        enableApiReview();
        var newApi = NewApiFixtures.aProxyApiV4();

        // When
        useCase.execute(new Input(newApi, AUDIT_INFO));

        // Then
        assertThat(workflowCrudService.storage())
            .contains(
                Workflow
                    .builder()
                    .id("generated-id")
                    .referenceType(Workflow.ReferenceType.API)
                    .referenceId("generated-id")
                    .type(Workflow.Type.REVIEW)
                    .state(Workflow.State.DRAFT)
                    .user(USER_ID)
                    .comment("")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    private void enableApiPrimaryOwnerMode(ApiPrimaryOwnerMode mode) {
        parametersQueryService.initWith(
            List.of(new Parameter(Key.API_PRIMARY_OWNER_MODE.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, mode.name()))
        );
    }

    private void enableApiReview() {
        parametersQueryService.define(
            new Parameter(Key.API_REVIEW_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
        );
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipCrudService.initWith(memberships);
    }

    private void givenExistingGroup(List<Group> groups) {
        groupQueryService.initWith(groups);
    }
}
