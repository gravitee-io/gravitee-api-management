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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.impl.promotion.PromotionServiceTest.USER_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.PropertiesService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImpl_findAllTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private VirtualHostService virtualHostService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private AlertService alertService;

    @Mock
    private RoleService roleService;

    @Mock
    private PolicyService policyService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ConnectorService connectorService;

    @Mock
    private PlanService planService;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EventService eventService;

    @Mock
    private TopApiService topApiService;

    @Mock
    private FlowService flowService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiValidationService apiValidationService;

    @Mock
    private MediaService mediaService;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private CategoryMapper categoryMapper;

    private ApiService apiService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void setUp() {
        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planService,
            flowService,
            parameterService,
            workflowService,
            new CategoryMapper(categoryService)
        );

        ApiConverter apiConverter = new ApiConverter(
            new ObjectMapper(),
            mock(io.gravitee.rest.api.service.PlanService.class),
            mock(io.gravitee.rest.api.service.configuration.flow.FlowService.class),
            mock(CategoryMapper.class),
            mock(ParameterService.class),
            mock(WorkflowService.class)
        );

        GenericApiMapper genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
        apiService =
            new ApiServiceImpl(
                apiRepository,
                apiMapper,
                genericApiMapper,
                primaryOwnerService,
                apiValidationService,
                parameterService,
                workflowService,
                auditService,
                membershipService,
                genericNotificationConfigService,
                apiMetadataService,
                flowService,
                searchEngineService,
                planService,
                planSearchService,
                subscriptionService,
                eventService,
                pageService,
                topApiService,
                portalNotificationConfigService,
                alertService,
                apiQualityRuleRepository,
                mediaService,
                propertiesService,
                apiNotificationService,
                tagsValidationService,
                apiAuthorizationService,
                groupService,
                categoryMapper
            );
    }

    @Test
    public void should_find_empty_page() {
        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environmentId(GraviteeContext.getExecutionContext().getEnvironmentId()).build()),
                isNull(),
                eq(pageable),
                eq(new ApiFieldFilter.Builder().excludePicture().build())
            )
        )
            .thenReturn(new Page<>(List.of(), 1, 0, 0));

        final Page<GenericApiEntity> apis = apiService.findAll(
            GraviteeContext.getExecutionContext(),
            "UnitTests",
            true,
            new PageableImpl(1, 10)
        );

        assertThat(apis).isNotNull();
        assertThat(apis.getContent().size()).isEqualTo(0);
        assertThat(apis.getTotalElements()).isEqualTo(0);
        assertThat(apis.getPageNumber()).isEqualTo(1);
        assertThat(apis.getPageElements()).isEqualTo(0);

        verify(apiRepository, times(1)).search(any(), any(), any(), any());
    }

    @Test
    public void should_find_api_entity_page() {
        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

        var api1 = new Api();
        api1.setId("API_1");
        var api2 = new Api();
        api2.setId("API_2");

        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environmentId(GraviteeContext.getExecutionContext().getEnvironmentId()).build()),
                isNull(),
                eq(pageable),
                eq(new ApiFieldFilter.Builder().excludePicture().build())
            )
        )
            .thenReturn(new Page<>(List.of(api1, api2), 1, 2, 2));

        final Page<GenericApiEntity> apis = apiService.findAll(
            GraviteeContext.getExecutionContext(),
            "UnitTests",
            true,
            new PageableImpl(1, 10)
        );

        assertThat(apis).isNotNull();
        assertThat(apis.getContent().size()).isEqualTo(2);
        assertThat(apis.getTotalElements()).isEqualTo(2);
        assertThat(apis.getPageNumber()).isEqualTo(1);
        assertThat(apis.getPageElements()).isEqualTo(2);

        verify(apiRepository, times(1)).search(any(), any(), any(), any());
    }

    @Test
    public void should_find_api_entity_page_if_not_admin() {
        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

        var api1 = new Api();
        api1.setId("API_1");
        var api2 = new Api();
        api2.setId("API_2");

        when(apiAuthorizationService.findApiIdsByUserId(eq(GraviteeContext.getExecutionContext()), eq(USER_ID), isNull()))
            .thenReturn(Set.of("API_1"));

        when(
            apiRepository.search(
                eq(
                    new ApiCriteria.Builder()
                        .environmentId(GraviteeContext.getExecutionContext().getEnvironmentId())
                        .ids(Set.of("API_1"))
                        .build()
                ),
                isNull(),
                eq(pageable),
                eq(new ApiFieldFilter.Builder().excludePicture().build())
            )
        )
            .thenReturn(new Page<>(List.of(api1), 1, 1, 1));

        final Page<GenericApiEntity> apis = apiService.findAll(
            GraviteeContext.getExecutionContext(),
            USER_ID,
            false,
            new PageableImpl(1, 10)
        );

        assertThat(apis).isNotNull();
        assertThat(apis.getContent().size()).isEqualTo(1);
        assertThat(apis.getTotalElements()).isEqualTo(1);
        assertThat(apis.getPageNumber()).isEqualTo(1);
        assertThat(apis.getPageElements()).isEqualTo(1);

        verify(apiRepository, times(1)).search(any(), any(), any(), any());
    }

    @Test
    public void should_return_empty_page_and_not_call_repository_if_not_admin_and_no_apis() {
        when(apiAuthorizationService.findApiIdsByUserId(eq(GraviteeContext.getExecutionContext()), eq(USER_ID), isNull()))
            .thenReturn(Set.of());

        final Page<GenericApiEntity> apis = apiService.findAll(
            GraviteeContext.getExecutionContext(),
            USER_ID,
            false,
            new PageableImpl(1, 10)
        );

        assertThat(apis).isNotNull();
        assertThat(apis.getContent().size()).isEqualTo(0);
        assertThat(apis.getTotalElements()).isEqualTo(0);
        assertThat(apis.getPageNumber()).isEqualTo(0);
        assertThat(apis.getPageElements()).isEqualTo(0);

        verify(apiRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    public void should_expand_primary_owner() {
        var sortable = new SortableBuilder().field("name").order(Order.ASC).build();
        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        var api1 = new Api();
        api1.setId("API_1");

        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environmentId(GraviteeContext.getExecutionContext().getEnvironmentId()).build()),
                eq(sortable),
                eq(pageable),
                eq(new ApiFieldFilter.Builder().excludePicture().build())
            )
        )
            .thenReturn(new Page<>(List.of(api1), 1, 1, 1));

        when(primaryOwnerService.getPrimaryOwner(any(ExecutionContext.class), anyString()))
            .thenReturn(PrimaryOwnerEntity.builder().id("po-id").displayName("a PO").build());

        final Page<GenericApiEntity> apis = apiService.findAll(
            GraviteeContext.getExecutionContext(),
            "UnitTests",
            true,
            Set.of("primaryOwner"),
            new SortableImpl("name", true),
            new PageableImpl(1, 10)
        );

        assertThat(apis).isNotNull();
        assertThat(apis.getContent().size()).isEqualTo(1);
        assertThat(apis.getContent().get(0))
            .extracting(GenericApiEntity::getPrimaryOwner)
            .isEqualTo(PrimaryOwnerEntity.builder().id("po-id").displayName("a PO").build());

        verify(primaryOwnerService).getPrimaryOwner(any(ExecutionContext.class), eq("API_1"));
    }
}
