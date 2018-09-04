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

import io.gravitee.management.service.impl.RoleServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleService_CreateOrUpdateSystemRolesTest {

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;

    @Mock
    private AuditService auditService;

    private static int[] mgmtAdminPermissions = new int[]{
            1015,
            1215,
            1315,
            1415,
            1515,
            1615,
            1715,
            1815,
            1915,
            2015,
            2115,
            2215
    };

    @Test
    public void shouldCreateSystemRole() throws TechnicalException {
        when(mockRoleRepository.findById(any(), any())).thenReturn(empty());

        roleService.createOrUpdateSystemRoles();

        verify(mockRoleRepository, times(5)).findById(any(), anyString());
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(5)).create(any());
    }

    @Test
    public void shouldUpdateSystemRole() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(new int[]{20,21});
        when(mockRoleRepository.findById(RoleScope.MANAGEMENT, "ADMIN")).thenReturn(of(mgmtAdminRole));
        when(mockRoleRepository.findById(RoleScope.PORTAL, "ADMIN")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.API, "PRIMARY_OWNER")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.APPLICATION, "PRIMARY_OWNER")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.GROUP, "ADMIN")).thenReturn(empty());

        roleService.createOrUpdateSystemRoles();

        verify(mockRoleRepository, times(5)).findById(any(), anyString());
        verify(mockRoleRepository, times(1)).update(argThat(new ArgumentMatcher<Role>() {
            @Override
            public boolean matches(Object o) {
                return ((Role) o).getScope().equals(RoleScope.MANAGEMENT) &&
                        Arrays.stream(((Role) o).getPermissions()).reduce(Math::addExact).orElse(0)
                                == Arrays.stream(mgmtAdminPermissions).reduce(Math::addExact).orElse(0);
            }
        }));
        verify(mockRoleRepository, times(4)).create(argThat(new ArgumentMatcher<Role>() {
            @Override
            public boolean matches(Object o) {
                return ((Role) o).getScope().equals(RoleScope.API)
                        || ((Role) o).getScope().equals(RoleScope.APPLICATION)
                        || ((Role) o).getScope().equals(RoleScope.PORTAL)
                        || ((Role) o).getScope().equals(RoleScope.GROUP);
            }
        }));
    }

    @Test
    public void shouldDoNothing() throws TechnicalException {

        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(mgmtAdminPermissions);
        when(mockRoleRepository.findById(RoleScope.MANAGEMENT, "ADMIN")).thenReturn(of(mgmtAdminRole));
        when(mockRoleRepository.findById(RoleScope.PORTAL, "ADMIN")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.API, "PRIMARY_OWNER")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.APPLICATION, "PRIMARY_OWNER")).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.GROUP, "ADMIN")).thenReturn(empty());

        roleService.createOrUpdateSystemRoles();

        verify(mockRoleRepository, times(5)).findById(any(), anyString());
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(4)).create(argThat(new ArgumentMatcher<Role>() {
            @Override
            public boolean matches(Object o) {
                return ((Role) o).getScope().equals(RoleScope.API)
                        || ((Role) o).getScope().equals(RoleScope.APPLICATION)
                        || ((Role) o).getScope().equals(RoleScope.PORTAL)
                        || ((Role) o).getScope().equals(RoleScope.GROUP);
            }
        }));
    }
}
