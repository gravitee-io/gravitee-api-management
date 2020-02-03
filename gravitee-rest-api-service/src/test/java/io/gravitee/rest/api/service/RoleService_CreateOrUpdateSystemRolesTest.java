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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.impl.RoleServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleService_CreateOrUpdateSystemRolesTest {

    private static final String REFERENCE_ID = "DEFAULT";
    private static final RoleReferenceType REFERENCE_TYPE = RoleReferenceType.ORGANIZATION;
    
    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;
    @Mock
    private AuditService auditService;

    private static int[] mgmtAdminPermissions = new int[] {
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
            2215,
            2315,
            2415,
            2515,
            2615,
            2715,
            2815
    };

    @Test
    public void shouldCreateSystemRole() throws TechnicalException {
        when(mockRoleRepository.findById(any(), any(), any(), any())).thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5)).findById(any(), anyString(), anyString(), any());
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(5)).create(any());
    }

    @Test
    public void shouldUpdateSystemRole() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(new int[]{20,21});
        when(mockRoleRepository.findById(RoleScope.MANAGEMENT, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(of(mgmtAdminRole));
        when(mockRoleRepository.findById(RoleScope.PORTAL, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.API, "PRIMARY_OWNER", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.APPLICATION, "PRIMARY_OWNER", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5)).findById(any(), anyString(), anyString(), any());
        verify(mockRoleRepository, times(1)).update(argThat(o -> o.getScope().equals(RoleScope.MANAGEMENT) &&
                Arrays.stream(o.getPermissions()).reduce(Math::addExact).orElse(0)
                        == Arrays.stream(mgmtAdminPermissions).reduce(Math::addExact).orElse(0)));
        verify(mockRoleRepository, times(4)).create(argThat(o -> o.getScope().equals(RoleScope.API)
                || o.getScope().equals(RoleScope.APPLICATION)
                || o.getScope().equals(RoleScope.PORTAL)
                || o.getScope().equals(RoleScope.GROUP)));
    }

    @Test
    public void shouldDoNothing() throws TechnicalException {

        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(mgmtAdminPermissions);
        when(mockRoleRepository.findById(RoleScope.MANAGEMENT, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(of(mgmtAdminRole));
        when(mockRoleRepository.findById(RoleScope.PORTAL, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.API, "PRIMARY_OWNER", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.APPLICATION, "PRIMARY_OWNER", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());
        when(mockRoleRepository.findById(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE)).thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5)).findById(any(), anyString(), anyString(), any());
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(4)).create(argThat(o -> o.getScope().equals(RoleScope.API)
                || o.getScope().equals(RoleScope.APPLICATION)
                || o.getScope().equals(RoleScope.PORTAL)
                || o.getScope().equals(RoleScope.GROUP)));
    }
}
