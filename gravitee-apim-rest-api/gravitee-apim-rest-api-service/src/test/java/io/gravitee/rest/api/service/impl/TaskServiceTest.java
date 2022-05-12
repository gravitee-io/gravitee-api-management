/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import java.util.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceTest {

    @InjectMocks
    private final TaskService taskService = new TaskServiceImpl();

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PromotionTasksService promotionTasksService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentService environmentService;

    @Before
    public void setUp() {
        MembershipEntity m1 = new MembershipEntity();
        m1.setId("1");
        m1.setReferenceId("api1");
        m1.setReferenceType(MembershipReferenceType.API);
        m1.setRoleId("API_PO");

        MembershipEntity m2 = new MembershipEntity();
        m2.setId("2");
        m2.setReferenceId("api2");
        m2.setReferenceType(MembershipReferenceType.API);
        m2.setRoleId("API_USER");

        Map<String, char[]> withPerm = new HashMap<>();
        withPerm.put("SUBSCRIPTION", new char[] { 'C', 'R', 'U', 'D' });
        Map<String, char[]> withoutPerm = new HashMap<>();
        withoutPerm.put("SUBSCRIPTION", new char[] { 'C', 'R', 'D' });

        RoleEntity roleEntityWithPerm = new RoleEntity();
        roleEntityWithPerm.setName("PO");
        roleEntityWithPerm.setPermissions(withPerm);
        roleEntityWithPerm.setScope(io.gravitee.rest.api.model.permissions.RoleScope.API);

        RoleEntity roleEntityWithoutPerm = new RoleEntity();
        roleEntityWithoutPerm.setName("USER");
        roleEntityWithoutPerm.setPermissions(withoutPerm);
        roleEntityWithoutPerm.setScope(io.gravitee.rest.api.model.permissions.RoleScope.API);

        when(roleService.findById("API_PO")).thenReturn(roleEntityWithPerm);

        when(roleService.findById("API_USER")).thenReturn(roleEntityWithoutPerm);

        when(promotionTasksService.getPromotionTasks(GraviteeContext.getExecutionContext())).thenReturn(emptyList());

        Set<MembershipEntity> memberships = new HashSet<>();
        memberships.add(m1);
        memberships.add(m2);
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(memberships);

        when(userService.search(eq(GraviteeContext.getExecutionContext()), any(UserCriteria.class), any()))
            .thenReturn(new Page<>(emptyList(), 1, 0, 0));
    }

    @AfterClass
    public static void afterClass() {
        // Clean up Spring security context.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Authentication authentication = mock(Authentication.class);

        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        taskService.findAll(GraviteeContext.getExecutionContext(), "user");

        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any());
        verify(promotionTasksService, times(1)).getPromotionTasks(GraviteeContext.getExecutionContext());
        verify(userService, times(0)).search(eq(GraviteeContext.getExecutionContext()), any(UserCriteria.class), any());
    }

    @Test
    public void shouldFindAllAsAdmin() throws TechnicalException {
        final Authentication authentication = mock(Authentication.class);
        Collection authorities = new ArrayList<>();
        GrantedAuthority admin = (GrantedAuthority) () -> "ENVIRONMENT:ADMIN";
        authorities.add(admin);
        when(authentication.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(GraviteeContext.getCurrentEnvironment());

        when(environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())).thenReturn(List.of(environment));

        taskService.findAll(GraviteeContext.getExecutionContext(), "admin");

        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any());
        verify(promotionTasksService, times(1)).getPromotionTasks(GraviteeContext.getExecutionContext());
        verify(userService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(UserCriteria.class), any());
    }

    @Test
    public void shouldGetMetadata() throws TechnicalException {
        final Authentication authentication = mock(Authentication.class);

        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        // Task Subscription
        TaskEntity taskSubscription = new TaskEntity();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        taskSubscription.setData(subscriptionEntity);
        subscriptionEntity.setApplication("appId");
        subscriptionEntity.setPlan("planId");

        Application application = new Application();
        application.setName("App Name");
        when(applicationRepository.findById(eq("appId"))).thenReturn(Optional.of(application));

        Plan plan = new Plan();
        plan.setName("Plan Name");
        plan.setApi("planApiId");
        when(planRepository.findById(eq("planId"))).thenReturn(Optional.of(plan));
        Api planApi = new Api();
        planApi.setName("Plan Api Name");
        when(apiRepository.findById(eq("planApiId"))).thenReturn(Optional.of(planApi));

        // Task Workflow
        TaskEntity taskWorkflow = new TaskEntity();
        Workflow workflow = new Workflow();
        taskWorkflow.setData(workflow);
        workflow.setReferenceType("API");
        workflow.setReferenceId("workflowApiId");
        Api workflowApi = new Api();
        workflowApi.setName("Workflow Api Name");
        when(apiRepository.findById(eq("workflowApiId"))).thenReturn(Optional.of(workflowApi));

        Metadata metadata = taskService.getMetadata(GraviteeContext.getExecutionContext(), Arrays.asList(taskSubscription, taskWorkflow));

        Map<String, Map<String, Object>> expectedMetadata = new HashMap<>();
        // expected Metadata for Task Subscription
        expectedMetadata.put("appId", Collections.singletonMap("name", "App Name"));
        expectedMetadata.put("planApiId", Collections.singletonMap("name", "Plan Api Name"));
        Map<String, Object> expectedPlanIdMetadata = new HashMap<>();
        expectedPlanIdMetadata.put("name", "Plan Name");
        expectedPlanIdMetadata.put("api", "planApiId");
        expectedMetadata.put("planId", expectedPlanIdMetadata);
        // expected Metadata for Task Workflow
        expectedMetadata.put("workflowApiId", Collections.singletonMap("name", "Workflow Api Name"));

        assertEquals(metadata.toMap(), expectedMetadata);
    }

    @Test
    public void shouldGetMetadataWithoutThrowTechnicalException() throws TechnicalException {
        final Authentication authentication = mock(Authentication.class);

        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        // Task Subscription
        TaskEntity taskSubscription = new TaskEntity();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        taskSubscription.setData(subscriptionEntity);
        subscriptionEntity.setApplication("appId");
        subscriptionEntity.setPlan("planId");

        when(applicationRepository.findById(eq("appId"))).thenThrow(new TechnicalException());

        when(planRepository.findById(eq("planId"))).thenThrow(new TechnicalException());

        // Task Workflow
        TaskEntity taskWorkflow = new TaskEntity();
        Workflow workflow = new Workflow();
        taskWorkflow.setData(workflow);
        workflow.setReferenceType("API");
        workflow.setReferenceId("workflowApiId");
        when(apiRepository.findById(eq("workflowApiId"))).thenThrow(new TechnicalException());

        Metadata metadata = taskService.getMetadata(GraviteeContext.getExecutionContext(), Arrays.asList(taskSubscription, taskWorkflow));

        assertEquals(metadata.toMap(), Collections.emptyMap());
    }
}
