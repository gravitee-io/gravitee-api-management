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
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiHostValidatorDomainServiceGoogleImpl;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CreateCategoryApiDomainServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.InstallationAccessQueryServiceInMemory;
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
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateAgentApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewAgentApi;
import io.gravitee.apim.core.api.use_case.CreateAgentApiUseCase.Input;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.AgentApi;
import io.gravitee.definition.model.v4.agent.StandaloneAgentDefinition;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateAgentApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

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
    CreateCategoryApiDomainServiceInMemory createCategoryApiDomainService = new CreateCategoryApiDomainServiceInMemory();

    CreateAgentApiUseCase useCase;

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
            workflowCrudService,
            createCategoryApiDomainService
        );
        var validateAgentApiDomainService = new ValidateAgentApiDomainService(
            new VerifyApiPathDomainService(
                new ApiQueryServiceInMemory(apiCrudService),
                new InstallationAccessQueryServiceInMemory(),
                new ApiHostValidatorDomainServiceGoogleImpl(),
                new ApiPathIndex()
            )
        );
        useCase = new CreateAgentApiUseCase(apiPrimaryOwnerFactory, createApiDomainService, validateAgentApiDomainService);

        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                )
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
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
        ).forEach(InMemoryAlternative::reset);
        indexer.reset();
    }

    @Test
    void should_create_a_standalone_agent_persisted_as_v4() {
        // When
        var output = useCase.execute(new Input(aStandaloneAgent(), AUDIT_INFO));

        // Then
        var created = output.api();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(created.getId()).isEqualTo("generated-id");
            soft.assertThat(created.getType()).isEqualTo(ApiType.AGENT);
            // Agents are persisted as V4 + type=AGENT; AGENT is only a REST discriminator.
            soft.assertThat(created.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            soft.assertThat(created.getVisibility()).isEqualTo(Api.Visibility.PRIVATE);
            soft.assertThat(created.getLifecycleState()).isEqualTo(Api.LifecycleState.STOPPED);
            soft.assertThat(created.getApiDefinitionAgent()).isInstanceOf(AgentApi.class);
            soft.assertThat(created.getApiDefinitionAgent().getKind()).isEqualTo("standalone");
            soft.assertThat(created.getApiDefinitionAgent().getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        });
    }

    @Test
    void should_persist_and_index_the_agent() {
        // When
        useCase.execute(new Input(aStandaloneAgent(), AUDIT_INFO));

        // Then
        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(apiCrudService.storage().get(0).getType()).isEqualTo(ApiType.AGENT);
        assertThat(indexer.storage()).hasSize(1);
        assertThat(indexer.storage().get(0).getId()).isEqualTo("generated-id");
    }

    @Test
    void should_default_visibility_to_private_when_not_provided() {
        // When
        var created = useCase.execute(new Input(aStandaloneAgent(), AUDIT_INFO)).api();

        // Then
        assertThat(created.getVisibility()).isEqualTo(Api.Visibility.PRIVATE);
    }

    @Test
    void should_throw_when_agent_is_null() {
        var throwable = catchThrowable(() -> new Input(null, AUDIT_INFO));
        assertThat(throwable).isInstanceOf(ApiInvalidTypeException.class);
    }

    @Test
    void should_throw_when_type_is_not_agent() {
        var notAnAgent = aStandaloneAgent().toBuilder().type(ApiType.PROXY).build();
        var throwable = catchThrowable(() -> new Input(notAnAgent, AUDIT_INFO));
        assertThat(throwable).isInstanceOf(ApiInvalidTypeException.class);
    }

    @Test
    void should_sanitize_listener_paths() {
        var created = useCase.execute(new Input(anAgentOnPath("chat//x"), AUDIT_INFO)).api();

        var listener = (HttpListener) created.getApiDefinitionAgent().getListeners().get(0);
        assertThat(listener.getPaths()).extracting("path").containsExactly("/chat/x/");
    }

    @Test
    void should_reject_an_agent_whose_path_is_already_taken_by_another_api() {
        apiCrudService.initWith(
            List.of(fixtures.core.model.ApiFixtures.aProxyApiV4().toBuilder().id("existing").environmentId(ENVIRONMENT_ID).build())
        );
        var agent = anAgentOnPath("/http_proxy");

        var throwable = catchThrowable(() -> useCase.execute(new Input(agent, AUDIT_INFO)));

        assertThat(throwable).isInstanceOf(InvalidPathsException.class).hasMessageContaining("/http_proxy/");
        assertThat(apiCrudService.storage()).hasSize(1);
    }

    @Test
    void should_create_an_agent_whose_path_is_free() {
        apiCrudService.initWith(
            List.of(fixtures.core.model.ApiFixtures.aProxyApiV4().toBuilder().id("existing").environmentId(ENVIRONMENT_ID).build())
        );

        var created = useCase.execute(new Input(anAgentOnPath("/my-agent"), AUDIT_INFO)).api();

        assertThat(created.getType()).isEqualTo(ApiType.AGENT);
        assertThat(apiCrudService.storage()).hasSize(2);
    }

    private static NewAgentApi anAgentOnPath(String path) {
        return aStandaloneAgent()
            .toBuilder()
            .listeners(
                List.of(
                    HttpListener.builder()
                        .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path(path).build()))
                        .build()
                )
            )
            .build();
    }

    private static NewAgentApi aStandaloneAgent() {
        return NewAgentApi.builder()
            .name("My Agent")
            .apiVersion("1.0.0")
            .type(ApiType.AGENT)
            .kind("standalone")
            .listeners(List.of(HttpListener.builder().paths(List.of()).build()))
            .standalone(StandaloneAgentDefinition.builder().model(AgentModel.builder().type("openai").build()).output("answer").build())
            .build();
    }
}
