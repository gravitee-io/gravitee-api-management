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
package io.gravitee.management.service;

import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.TaskEntity;
import io.gravitee.management.service.impl.TaskServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceTest {

    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APPLICATION_ID = "my-application";
    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";
    private static final String SUBSCRIPTION_VALIDATOR = "validator";

    @InjectMocks
    private TaskService taskService = new TaskServiceImpl();

    @Mock
    private ApiService apiService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private ApiRepository apiRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private PlanService planService;

    @Test
    public void shouldFindAll() throws TechnicalException {
        Membership m1 = new Membership();

        m1.setReferenceId("api1");
        m1.setReferenceType(MembershipReferenceType.API);
        m1.setRoles(Collections.singletonMap(RoleScope.API.getId(), "PO"));

        Membership m2 = new Membership();
        m2.setReferenceId("api2");
        m2.setReferenceType(MembershipReferenceType.API);
        m2.setRoles(Collections.singletonMap(RoleScope.API.getId(), "USER"));

        Map<String, char[]> withPerm = new HashMap<>();
        withPerm.put("SUBSCRIPTION", new char[]{'C', 'R', 'U', 'D'});
        Map<String, char[]> withoutPerm = new HashMap<>();
        withoutPerm.put("SUBSCRIPTION", new char[]{'C', 'R', 'D'});

        RoleEntity roleEntityWithPerm = new RoleEntity();
        roleEntityWithPerm.setName("PO");
        roleEntityWithPerm.setPermissions(withPerm);

        RoleEntity roleEntityWithoutPerm = new RoleEntity();
        roleEntityWithoutPerm.setName("USER");
        roleEntityWithoutPerm.setPermissions(withoutPerm);

        when(roleService.findById(RoleScope.API, "PO"))
                .thenReturn(roleEntityWithPerm);

        when(roleService.findById(RoleScope.API, "USER"))
                .thenReturn(roleEntityWithoutPerm);

        Set<Membership> memberships = new HashSet<>();
        memberships.add(m1);
        memberships.add(m2);
        when(membershipRepository.findByUserAndReferenceType(any(), any()))
                .thenReturn(memberships);

        List<TaskEntity> tasks = taskService.findAll("user");

        verify(subscriptionService, times(1)).search(any());

    }
}
