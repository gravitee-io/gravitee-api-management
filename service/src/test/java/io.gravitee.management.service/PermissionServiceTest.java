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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.service.impl.PermissionServiceImpl;
import io.gravitee.repository.api.management.TeamMembershipRepository;
import io.gravitee.repository.api.management.UserRepository;
import io.gravitee.repository.exceptions.TechnicalException;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

    private static final String USER_NAME = "myUser";
    private static final String API_NAME = "myAPI";

    @InjectMocks
    private PermissionService permissionService = new PermissionServiceImpl();

    @Mock
    private TeamMembershipRepository teamMembershipRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private ApiService apiService;
    @Mock
    private ApplicationService applicationService;

    @Test
    @Ignore
    public void shouldHavePermissionOnViewAPI() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_API);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnViewAPI() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_API);
    }

    @Test
    @Ignore
    public void shouldHavePermissionOnEditAPI() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_API);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnEditAPI() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_API);
    }

    @Test
    @Ignore
    public void shouldHavePermissionOnViewApplication() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_APPLICATION);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnViewApplication() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_APPLICATION);
    }

    @Test
    @Ignore
    public void shouldHavePermissionOnEditApplication() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_APPLICATION);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnEditApplication() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_APPLICATION);
    }

    @Test
    @Ignore
    public void shouldHavePermissionOnViewTeam() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_TEAM);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnViewTeam() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_TEAM);
    }

    @Test
    @Ignore
    public void shouldHavePermissionOnEditTeam() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_TEAM);
    }

    @Test
    @Ignore
    public void shouldNotHavePermissionOnEditTeam() throws TechnicalException {
        permissionService.hasPermission(USER_NAME, API_NAME, PermissionType.EDIT_TEAM);
    }
}