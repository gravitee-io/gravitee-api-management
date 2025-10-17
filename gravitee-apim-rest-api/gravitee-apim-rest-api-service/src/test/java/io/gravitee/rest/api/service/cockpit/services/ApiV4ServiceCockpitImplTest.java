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
package io.gravitee.rest.api.service.cockpit.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.converter.PageConverter;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiV4ServiceCockpitImplTest {

    private static final String API_ID = "api#id";
    private static final String API_CROSS_ID = "api#crossId";
    private static final List<String> LABELS = List.of("label1", "label2");
    private static final String USER_ID = "user#id";
    private static final String ENVIRONMENT_ID = "environment#id";
    private static final String ORGANIZATION_ID = "organization#id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
    private final PageConverter pageConverter = new PageConverter();

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private ApiService apiService;

    @Mock
    private SwaggerService swaggerService;

    @Mock
    private PageService pageService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private PlanService planService;

    @Mock
    private VerifyApiPathDomainService verifyApiPathsDomainService;

    @Mock
    private io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Mock
    private ApiStateDomainService apiStateDomainService;

    private ApiServiceCockpitImpl service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "random-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    public void setUp() throws Exception {
        UuidString.overrideGenerator(() -> "random-id");

        apiConverter = new ApiConverter(
            new ObjectMapper(),
            mock(PlanService.class),
            mock(FlowService.class),
            mock(CategoryMapper.class),
            mock(ParameterService.class),
            mock(WorkflowService.class)
        );
        service = new ApiServiceCockpitImpl(
            new ObjectMapper(),
            apiService,
            swaggerService,
            pageService,
            apiMetadataService,
            planService,
            apiConverter,
            pageConverter,
            verifyApiPathsDomainService,
            apiServiceV4,
            apiStateDomainService
        );
    }

    @Test
    public void should_documented_v4_api() {
        var up = new io.gravitee.rest.api.model.v4.api.UpdateApiEntity();
        up.setId(API_ID);
        up.setLabels(LABELS);
        up.setCrossId(API_CROSS_ID);
        up.setVisibility(Visibility.PRIVATE);
        up.setLifecycleState(ApiLifecycleState.CREATED);
        up.setPlans(
            Set.of(
                io.gravitee.rest.api.model.v4.plan.PlanEntity.builder()
                    .id("random-id")
                    .apiId(API_ID)
                    .crossId(API_CROSS_ID)
                    .mode(PlanMode.STANDARD)
                    .apiType(ApiType.PROXY)
                    .name("Default plan")
                    .type(io.gravitee.rest.api.model.v4.plan.PlanType.API)
                    .status(PlanStatus.PUBLISHED)
                    .security(PlanSecurity.builder().type("key-less").build())
                    .build()
            )
        );

        service.manageV4Api(
            EXECUTION_CONTEXT,
            AuditInfo.builder()
                .environmentId(ENVIRONMENT_ID)
                .organizationId(ORGANIZATION_ID)
                .actor(AuditActor.builder().userId(USER_ID).build())
                .build(),
            DeploymentMode.API_DOCUMENTED,
            Api.builder().id(API_ID).crossId(API_CROSS_ID).labels(LABELS).build(),
            API_CROSS_ID,
            LABELS
        );

        verify(apiServiceV4).update(eq(EXECUTION_CONTEXT), eq(API_ID), eq(up), eq(USER_ID));
        verify(apiStateDomainService).deploy(any(), any(), any());
        verify(apiStateDomainService, never()).start(any(), any());
    }

    @Test
    public void should_mocked_v4_api() {
        var up = new io.gravitee.rest.api.model.v4.api.UpdateApiEntity();
        up.setId(API_ID);
        up.setLabels(LABELS);
        up.setCrossId(API_CROSS_ID);
        up.setVisibility(Visibility.PRIVATE);
        up.setLifecycleState(ApiLifecycleState.CREATED);
        up.setPlans(
            Set.of(
                io.gravitee.rest.api.model.v4.plan.PlanEntity.builder()
                    .id("random-id")
                    .apiId(API_ID)
                    .crossId(API_CROSS_ID)
                    .mode(PlanMode.STANDARD)
                    .apiType(ApiType.PROXY)
                    .name("Default plan")
                    .type(io.gravitee.rest.api.model.v4.plan.PlanType.API)
                    .status(PlanStatus.PUBLISHED)
                    .security(PlanSecurity.builder().type("key-less").build())
                    .build()
            )
        );

        service.manageV4Api(
            EXECUTION_CONTEXT,
            AuditInfo.builder()
                .environmentId(ENVIRONMENT_ID)
                .organizationId(ORGANIZATION_ID)
                .actor(AuditActor.builder().userId(USER_ID).build())
                .build(),
            DeploymentMode.API_MOCKED,
            Api.builder().id(API_ID).crossId(API_CROSS_ID).labels(LABELS).build(),
            API_CROSS_ID,
            LABELS
        );

        verify(apiServiceV4).update(eq(EXECUTION_CONTEXT), eq(API_ID), eq(up), eq(USER_ID));
        verify(apiStateDomainService).deploy(any(), any(), any());
        verify(apiStateDomainService).start(any(), any());
    }

    @Test
    public void should_published_v4_api() {
        var up = new io.gravitee.rest.api.model.v4.api.UpdateApiEntity();
        up.setId(API_ID);
        up.setLabels(LABELS);
        up.setCrossId(API_CROSS_ID);
        up.setVisibility(Visibility.PUBLIC);
        up.setLifecycleState(ApiLifecycleState.PUBLISHED);
        up.setPlans(
            Set.of(
                io.gravitee.rest.api.model.v4.plan.PlanEntity.builder()
                    .id("random-id")
                    .apiId(API_ID)
                    .crossId(API_CROSS_ID)
                    .mode(PlanMode.STANDARD)
                    .apiType(ApiType.PROXY)
                    .name("Default plan")
                    .type(io.gravitee.rest.api.model.v4.plan.PlanType.API)
                    .status(PlanStatus.PUBLISHED)
                    .security(PlanSecurity.builder().type("key-less").build())
                    .build()
            )
        );

        service.manageV4Api(
            EXECUTION_CONTEXT,
            AuditInfo.builder()
                .environmentId(ENVIRONMENT_ID)
                .organizationId(ORGANIZATION_ID)
                .actor(AuditActor.builder().userId(USER_ID).build())
                .build(),
            DeploymentMode.API_PUBLISHED,
            Api.builder().id(API_ID).crossId(API_CROSS_ID).labels(LABELS).build(),
            API_CROSS_ID,
            LABELS
        );

        verify(apiServiceV4).update(eq(EXECUTION_CONTEXT), eq(API_ID), eq(up), eq(USER_ID));
        verify(apiStateDomainService).deploy(any(), any(), any());
        verify(apiStateDomainService).start(any(), any());
    }
}
