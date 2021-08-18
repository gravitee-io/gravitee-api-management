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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindAllTest {

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Test
    public void shouldTryFindAll() throws Exception {
        Application application = new Application();
        application.setId("appId");
        application.setType(ApplicationType.SIMPLE);
        application.setStatus(ApplicationStatus.ACTIVE);
        when(applicationRepository.findAllByEnvironment(eq("DEFAULT"), eq(ApplicationStatus.ACTIVE)))
            .thenReturn(new HashSet<>(Collections.singletonList(application)));
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(new RoleEntity());
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any()))
            .thenReturn(new HashSet<>(Collections.singletonList(new MembershipEntity())));
        when(userService.findByIds(any())).thenReturn(Collections.emptySet());

        Set<ApplicationListItem> set = applicationService.findAll();
        assertThat(set).hasSize(1);
        verify(applicationRepository, times(1)).findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE);
    }

    @Test
    public void shouldTryFindAll_noResult() throws Exception {
        Set<ApplicationListItem> set = applicationService.findAll();
        assertThat(set).isEmpty();
        verify(applicationRepository, times(1)).findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldTryFindAll_exception() throws Exception {
        when(applicationRepository.findAllByEnvironment(eq("DEFAULT"), eq(ApplicationStatus.ACTIVE))).thenThrow(TechnicalException.class);
        Set<ApplicationListItem> set = applicationService.findAll();
    }
}
