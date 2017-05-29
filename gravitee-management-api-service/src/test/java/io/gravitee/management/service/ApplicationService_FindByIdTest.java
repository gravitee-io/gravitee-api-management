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

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByIdTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private Application application;

    @Test
    public void shouldFindById() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        Membership po = new Membership(USER_NAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setRoleScope(RoleScope.APPLICATION.getId());
        po.setRoleName("PRIMAY_OWNER");
        when(membershipRepository.findByReferenceAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(Collections.singleton(po));
        when(userService.findByName(USER_NAME, false)).thenReturn(new UserEntity());

        final ApplicationEntity applicationEntity = applicationService.findById(APPLICATION_ID);

        assertNotNull(applicationEntity);
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        applicationService.findById(APPLICATION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenThrow(TechnicalException.class);

        applicationService.findById(APPLICATION_ID);
    }
}
