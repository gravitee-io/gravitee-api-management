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
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.domain_service.UpdateAgentApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateAgentApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewAgentApi;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
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
class UpdateAgentApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "agent-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();

    UpdateAgentApiUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("member-id")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_ID).email("jane.doe@gravitee.io").build()));

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var updateAgentApiDomainService = new UpdateAgentApiDomainService(
            apiCrudService,
            auditDomainService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(
                    new ApiMetadataQueryServiceInMemory(metadataCrudService),
                    new FreemarkerTemplateProcessor()
                ),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            )
        );
        var validateAgentApiDomainService = new ValidateAgentApiDomainService(
            new VerifyApiPathDomainService(
                new ApiQueryServiceInMemory(apiCrudService),
                new InstallationAccessQueryServiceInMemory(),
                new ApiHostValidatorDomainServiceGoogleImpl(),
                new ApiPathIndex()
            )
        );
        useCase = new UpdateAgentApiUseCase(apiPrimaryOwnerDomainService, updateAgentApiDomainService, validateAgentApiDomainService);

        apiCrudService.initWith(List.of(existingAgent()));
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiCrudService,
            auditCrudService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            metadataCrudService,
            userCrudService
        ).forEach(InMemoryAlternative::reset);
        indexer.reset();
    }

    @Test
    void should_update_management_fields_and_agent_definition() {
        // When
        var updated = useCase.execute(new UpdateAgentApiUseCase.Input(API_ID, updatePayload(), AUDIT_INFO)).updatedApi();

        // Then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(updated.getId()).isEqualTo(API_ID);
            soft.assertThat(updated.getName()).isEqualTo("Updated Agent");
            soft.assertThat(updated.getVersion()).isEqualTo("2.0.0");
            // Agents stay persisted as V4 + type=AGENT
            soft.assertThat(updated.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            soft.assertThat(updated.getType()).isEqualTo(ApiType.AGENT);
            // The full agent body is replaced
            soft.assertThat(updated.getApiDefinitionAgent().getName()).isEqualTo("Updated Agent");
            soft.assertThat(updated.getApiDefinitionAgent().getStandalone().getOutput()).isEqualTo("new-output");
            soft.assertThat(updated.getApiDefinitionAgent().getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        });
    }

    @Test
    void should_persist_and_index_the_updated_agent() {
        // When
        useCase.execute(new UpdateAgentApiUseCase.Input(API_ID, updatePayload(), AUDIT_INFO));

        // Then
        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(apiCrudService.storage().get(0).getName()).isEqualTo("Updated Agent");
        assertThat(indexer.storage()).hasSize(1);
        assertThat(indexer.storage().get(0).getId()).isEqualTo(API_ID);
    }

    @Test
    void should_sanitize_listener_paths_on_update() {
        var updated = useCase.execute(new UpdateAgentApiUseCase.Input(API_ID, anUpdateOnPath("chat"), AUDIT_INFO)).updatedApi();

        var listener = (HttpListener) updated.getApiDefinitionAgent().getListeners().get(0);
        assertThat(listener.getPaths()).extracting("path").containsExactly("/chat/");
    }

    @Test
    void should_reject_an_update_moving_the_agent_onto_a_path_taken_by_another_api() {
        apiCrudService.initWith(
            List.of(
                existingAgent(),
                fixtures.core.model.ApiFixtures.aProxyApiV4().toBuilder().id("other").environmentId(ENVIRONMENT_ID).build()
            )
        );

        var throwable = catchThrowable(() ->
            useCase.execute(new UpdateAgentApiUseCase.Input(API_ID, anUpdateOnPath("/http_proxy"), AUDIT_INFO))
        );

        assertThat(throwable).isInstanceOf(InvalidPathsException.class).hasMessageContaining("/http_proxy/");
    }

    @Test
    void should_allow_an_update_keeping_the_agent_own_path() {
        apiCrudService.initWith(List.of(agentOnPath("/my-agent")));

        var updated = useCase.execute(new UpdateAgentApiUseCase.Input(API_ID, anUpdateOnPath("/my-agent"), AUDIT_INFO)).updatedApi();

        assertThat(updated.getName()).isEqualTo("Updated Agent");
    }

    private static NewAgentApi anUpdateOnPath(String path) {
        return updatePayload()
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

    private static Api agentOnPath(String path) {
        var agent = existingAgent();
        agent.setApiDefinitionValue(
            ((AgentApi) agent.getApiDefinitionValue()).toBuilder()
                .listeners(
                    List.of(
                        HttpListener.builder()
                            .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path(path).build()))
                            .build()
                    )
                )
                .build()
        );
        return agent;
    }

    private static Api existingAgent() {
        return Api.builder()
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("Old Agent")
            .version("1.0.0")
            .type(ApiType.AGENT)
            .definitionVersion(DefinitionVersion.V4)
            .visibility(Api.Visibility.PRIVATE)
            .lifecycleState(Api.LifecycleState.STOPPED)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .apiDefinitionValue(
                AgentApi.builder()
                    .id(API_ID)
                    .name("Old Agent")
                    .apiVersion("1.0.0")
                    .definitionVersion(DefinitionVersion.V4)
                    .kind("standalone")
                    .standalone(
                        StandaloneAgentDefinition.builder().model(AgentModel.builder().type("openai").build()).output("old-output").build()
                    )
                    .build()
            )
            .build();
    }

    private static NewAgentApi updatePayload() {
        return NewAgentApi.builder()
            .name("Updated Agent")
            .apiVersion("2.0.0")
            .type(ApiType.AGENT)
            .kind("standalone")
            .listeners(List.of(HttpListener.builder().paths(List.of()).build()))
            .standalone(StandaloneAgentDefinition.builder().model(AgentModel.builder().type("openai").build()).output("new-output").build())
            .build();
    }
}
