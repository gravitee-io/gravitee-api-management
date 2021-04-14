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
package io.gravitee.rest.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.impl.TaskServiceImpl;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceTest {

    @InjectMocks
    private TaskService taskService = new TaskServiceImpl();

    @Mock
    private ApiService apiService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private PlanService planService;

    @Test
    public void shouldFindAll() throws TechnicalException {
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

        Set<MembershipEntity> memberships = new HashSet<>();
        memberships.add(m1);
        memberships.add(m2);
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(memberships);
        taskService.findAll("user");

        verify(subscriptionService, times(1)).search(any());
    }
}
