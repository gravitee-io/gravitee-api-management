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
package io.gravitee.apim.core.api.use_case.cockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.domain_service.api.OAIDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.api.UpdateApiDomainServiceImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.v4.ApiService;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeployModelToApiUpdateUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final List<String> LABELS = List.of("label1", "label2");

    private static final URL resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
    public static final String RANDOM_ID = "random-id";
    public static final String API_ID = "apiId";
    public static final String API_CROSS_ID = "crossId";

    private String swaggerDefinition;

    private final PolicyOperationVisitorManagerImpl policyOperationVisitorManager = new PolicyOperationVisitorManagerImpl();
    private final EndpointConnectorPluginDomainService endpointConnectorPluginService = mock(EndpointConnectorPluginDomainService.class);

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PolicyPluginCrudServiceInMemory policyPluginCrudService = new PolicyPluginCrudServiceInMemory();

    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();

    @Mock
    private ApiService delegateApiService;

    @Mock
    private ApiStateDomainService apiStateDomainService;

    private DeployModelToApiUpdateUseCase useCase;

    @BeforeEach
    void setUp() throws IOException {
        UuidString.overrideGenerator(() -> RANDOM_ID);

        when(apiStateDomainService.isSynchronized(any(), eq(AUDIT_INFO))).thenReturn(false);
        lenient()
            .when(apiStateDomainService.start(any(), eq(AUDIT_INFO)))
            .thenAnswer(invocation -> {
                var apiToUpdate = invocation.<Api>getArgument(0);
                var api = apiCrudService.get(apiToUpdate.getId());

                api.setLifecycleState(Api.LifecycleState.STARTED);
                return api;
            });
        lenient()
            .when(apiStateDomainService.deploy(any(), anyString(), eq(AUDIT_INFO)))
            .thenAnswer(invocation -> invocation.<Api>getArgument(0));

        when(
            delegateApiService.update(
                eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                eq(API_ID),
                any(UpdateApiEntity.class),
                anyBoolean(),
                anyString()
            )
        ).thenAnswer(invocation -> {
            var updateApiEntity = invocation.<UpdateApiEntity>getArgument(2);
            var api = apiCrudService.get(updateApiEntity.getId());

            var apiStatus = switch (updateApiEntity.getLifecycleState()) {
                case CREATED -> Api.ApiLifecycleState.CREATED;
                case PUBLISHED -> Api.ApiLifecycleState.PUBLISHED;
                case UNPUBLISHED -> Api.ApiLifecycleState.UNPUBLISHED;
                case DEPRECATED -> Api.ApiLifecycleState.DEPRECATED;
                case ARCHIVED -> Api.ApiLifecycleState.ARCHIVED;
            };

            var visibility = switch (updateApiEntity.getVisibility()) {
                case PUBLIC -> Api.Visibility.PUBLIC;
                case PRIVATE -> Api.Visibility.PRIVATE;
            };

            api.setApiLifecycleState(apiStatus);
            api.setVisibility(visibility);

            api.setLabels(updateApiEntity.getLabels());

            return ApiAdapter.INSTANCE.toApiEntity(api);
        });

        var groupQueryService = new GroupQueryServiceInMemory();
        groupQueryService.initWith(List.of(Group.builder().id("1").name("group1").environmentId(ENVIRONMENT_ID).build()));
        var tagQueryService = new TagQueryServiceInMemory();
        tagQueryService.initWith(
            List.of(
                Tag.builder().id("1").name("tag1").referenceId(ORGANIZATION_ID).referenceType(Tag.TagReferenceType.ORGANIZATION).build()
            )
        );

        swaggerDefinition = Resources.toString(resource, Charsets.UTF_8);

        final var oaiDomainService = new OAIDomainServiceImpl(
            policyOperationVisitorManager,
            groupQueryService,
            tagQueryService,
            endpointConnectorPluginService,
            policyPluginCrudService
        );

        final var updateApiDomainService = new UpdateApiDomainServiceImpl(delegateApiService, apiCrudService);

        useCase = new DeployModelToApiUpdateUseCase(
            oaiDomainService,
            updateApiDomainService,
            apiStateDomainService,
            planCrudService,
            pageCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(policyPluginCrudService, apiCrudService, planCrudService, pageCrudService).forEach(InMemoryAlternative::reset);

        GraviteeContext.cleanContext();

        UuidString.reset();
    }

    @ParameterizedTest
    @MethodSource
    public void should_update_api(
        AbstractDeployModelToApiUseCase.Mode mode,
        Api.ApiLifecycleState apiLifecycleState,
        Api.LifecycleState lifecycleState,
        Api.Visibility visibility
    ) {
        apiCrudService.create(Api.builder().id(API_ID).crossId(API_CROSS_ID).definitionVersion(DefinitionVersion.V4).build());
        planCrudService.create(
            Plan.builder()
                .id(RANDOM_ID)
                .apiId(API_ID)
                .type(Plan.PlanType.API)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .status(PlanStatus.PUBLISHED)
                        .security(PlanSecurity.builder().type("key-less").build())
                        .build()
                )
                .validation(Plan.PlanValidationType.AUTO)
                .build()
        );
        pageCrudService.createDocumentationPage(
            Page.builder()
                .id(RANDOM_ID)
                .referenceId(API_ID)
                .referenceType(Page.ReferenceType.API)
                .name("Swagger")
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PUBLIC)
                .content("oldValue")
                .published(true)
                .build()
        );

        useCase.execute(new DeployModelToApiUpdateUseCase.Input(swaggerDefinition, AUDIT_INFO, API_ID, API_CROSS_ID, mode, LABELS));

        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(apiCrudService.storage().getFirst())
            .extracting(
                Api::getId,
                Api::getCrossId,
                Api::getDefinitionVersion,
                Api::getApiLifecycleState,
                Api::getLifecycleState,
                Api::getVisibility,
                Api::getLabels
            )
            .containsExactly(API_ID, API_CROSS_ID, DefinitionVersion.V4, apiLifecycleState, lifecycleState, visibility, LABELS);

        assertThat(planCrudService.storage()).hasSize(1);
        assertThat(planCrudService.storage().getFirst())
            .extracting(Plan::getId, Plan::getPlanStatus)
            .containsExactly(RANDOM_ID, PlanStatus.PUBLISHED);

        assertThat(pageCrudService.storage()).hasSize(1);
        assertThat(pageCrudService.storage().getFirst())
            .extracting(Page::getId, Page::getName, Page::getVisibility, Page::getType, Page::getContent)
            .containsExactly(RANDOM_ID, "Swagger", Page.Visibility.PUBLIC, Page.Type.SWAGGER, swaggerDefinition);

        verify(apiStateDomainService, lifecycleState == Api.LifecycleState.STOPPED ? never() : times(1)).start(any(), eq(AUDIT_INFO));
        verify(apiStateDomainService, times(1)).deploy(any(), any(), eq(AUDIT_INFO));
    }

    @ParameterizedTest
    @MethodSource("should_update_api")
    public void should_create_new_plan_if_the_plan_is_closed(
        AbstractDeployModelToApiUseCase.Mode mode,
        Api.ApiLifecycleState apiLifecycleState,
        Api.LifecycleState lifecycleState,
        Api.Visibility visibility
    ) {
        apiCrudService.create(Api.builder().id(API_ID).crossId(API_CROSS_ID).definitionVersion(DefinitionVersion.V4).build());
        planCrudService.create(
            Plan.builder()
                .id(RANDOM_ID)
                .apiId(API_ID)
                .type(Plan.PlanType.API)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .status(PlanStatus.CLOSED)
                        .security(PlanSecurity.builder().type("key-less").build())
                        .build()
                )
                .validation(Plan.PlanValidationType.AUTO)
                .build()
        );
        pageCrudService.createDocumentationPage(
            Page.builder()
                .id(RANDOM_ID)
                .referenceId(API_ID)
                .referenceType(Page.ReferenceType.API)
                .name("Swagger")
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PUBLIC)
                .content("oldValue")
                .published(true)
                .build()
        );

        useCase.execute(new DeployModelToApiUpdateUseCase.Input(swaggerDefinition, AUDIT_INFO, API_ID, API_CROSS_ID, mode, LABELS));

        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(apiCrudService.storage().getFirst())
            .extracting(
                Api::getId,
                Api::getCrossId,
                Api::getDefinitionVersion,
                Api::getApiLifecycleState,
                Api::getLifecycleState,
                Api::getVisibility,
                Api::getLabels
            )
            .containsExactly(API_ID, API_CROSS_ID, DefinitionVersion.V4, apiLifecycleState, lifecycleState, visibility, LABELS);

        final var plans = planCrudService
            .storage()
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED)
            .toList();
        assertThat(plans).hasSize(1);
        assertThat(plans.getFirst()).extracting(Plan::getId, Plan::getPlanStatus).containsExactly(RANDOM_ID, PlanStatus.PUBLISHED);

        assertThat(pageCrudService.storage()).hasSize(1);
        assertThat(pageCrudService.storage().getFirst())
            .extracting(Page::getId, Page::getName, Page::getVisibility, Page::getType, Page::getContent)
            .containsExactly(RANDOM_ID, "Swagger", Page.Visibility.PUBLIC, Page.Type.SWAGGER, swaggerDefinition);

        verify(apiStateDomainService, lifecycleState == Api.LifecycleState.STOPPED ? never() : times(1)).start(any(), eq(AUDIT_INFO));
        verify(apiStateDomainService, times(1)).deploy(any(), any(), eq(AUDIT_INFO));
    }

    @ParameterizedTest
    @MethodSource("should_update_api")
    public void should_create_new_page_if_no_swagger_page_exists(
        AbstractDeployModelToApiUseCase.Mode mode,
        Api.ApiLifecycleState apiLifecycleState,
        Api.LifecycleState lifecycleState,
        Api.Visibility visibility
    ) {
        apiCrudService.create(Api.builder().id(API_ID).crossId(API_CROSS_ID).definitionVersion(DefinitionVersion.V4).build());
        planCrudService.create(
            Plan.builder()
                .id(RANDOM_ID)
                .apiId(API_ID)
                .type(Plan.PlanType.API)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .status(PlanStatus.CLOSED)
                        .security(PlanSecurity.builder().type("key-less").build())
                        .build()
                )
                .validation(Plan.PlanValidationType.AUTO)
                .build()
        );

        useCase.execute(new DeployModelToApiUpdateUseCase.Input(swaggerDefinition, AUDIT_INFO, API_ID, API_CROSS_ID, mode, LABELS));

        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(apiCrudService.storage().getFirst())
            .extracting(
                Api::getId,
                Api::getCrossId,
                Api::getDefinitionVersion,
                Api::getApiLifecycleState,
                Api::getLifecycleState,
                Api::getVisibility,
                Api::getLabels
            )
            .containsExactly(API_ID, API_CROSS_ID, DefinitionVersion.V4, apiLifecycleState, lifecycleState, visibility, LABELS);

        final var plans = planCrudService
            .storage()
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED)
            .toList();
        assertThat(plans).hasSize(1);
        assertThat(plans.getFirst()).extracting(Plan::getId, Plan::getPlanStatus).containsExactly(RANDOM_ID, PlanStatus.PUBLISHED);

        assertThat(pageCrudService.storage()).hasSize(1);
        assertThat(pageCrudService.storage().getFirst())
            .extracting(Page::getId, Page::getName, Page::getVisibility, Page::getType, Page::getContent)
            .containsExactly(RANDOM_ID, "Swagger", Page.Visibility.PUBLIC, Page.Type.SWAGGER, swaggerDefinition);

        verify(apiStateDomainService, lifecycleState == Api.LifecycleState.STOPPED ? never() : times(1)).start(any(), eq(AUDIT_INFO));
        verify(apiStateDomainService, times(1)).deploy(any(), any(), eq(AUDIT_INFO));
    }

    private static Stream<Arguments> should_update_api() {
        return Stream.of(
            Arguments.of(
                AbstractDeployModelToApiUseCase.Mode.DOCUMENTED,
                Api.ApiLifecycleState.CREATED,
                Api.LifecycleState.STOPPED,
                Api.Visibility.PRIVATE
            ),
            Arguments.of(
                AbstractDeployModelToApiUseCase.Mode.MOCKED,
                Api.ApiLifecycleState.CREATED,
                Api.LifecycleState.STARTED,
                Api.Visibility.PRIVATE
            ),
            Arguments.of(
                AbstractDeployModelToApiUseCase.Mode.PUBLISHED,
                Api.ApiLifecycleState.PUBLISHED,
                Api.LifecycleState.STARTED,
                Api.Visibility.PUBLIC
            )
        );
    }
}
