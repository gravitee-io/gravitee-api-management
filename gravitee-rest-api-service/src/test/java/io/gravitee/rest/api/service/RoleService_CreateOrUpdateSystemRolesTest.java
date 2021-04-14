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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.impl.RoleServiceImpl;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    private static int[] envtAdminPermissions = new int[] {
        1015,
        1215,
        1315,
        1415,
        1515,
        1715,
        1815,
        1915,
        2015,
        2215,
        2315,
        2415,
        2515,
        2615,
        2715,
        2815,
        2915,
        3015,
        3115,
        3215,
        3315,
        3415,
        3515,
        3615,
    };

    @Test
    public void shouldCreateSystemRole() throws TechnicalException {
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any())).thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(5)).create(any());
    }

    @Test
    public void shouldUpdateSystemRole() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(new int[] { 20, 21 });
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ENVIRONMENT,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(of(mgmtAdminRole));
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ORGANIZATION,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.APPLICATION,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, times(1))
            .update(
                argThat(
                    o ->
                        o.getScope().equals(RoleScope.ENVIRONMENT) &&
                        Arrays.stream(o.getPermissions()).reduce(Math::addExact).orElse(0) ==
                        Arrays.stream(envtAdminPermissions).reduce(Math::addExact).orElse(0)
                )
            );
        verify(mockRoleRepository, times(4))
            .create(
                argThat(
                    o ->
                        o.getScope().equals(RoleScope.API) ||
                        o.getScope().equals(RoleScope.APPLICATION) ||
                        o.getScope().equals(RoleScope.ORGANIZATION) ||
                        o.getScope().equals(RoleScope.PLATFORM) ||
                        o.getScope().equals(RoleScope.GROUP)
                )
            );
    }

    @Test
    public void shouldDoNothing() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(envtAdminPermissions);
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ENVIRONMENT,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(of(mgmtAdminRole));
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.APPLICATION,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(empty());

        roleService.createOrUpdateSystemRoles(REFERENCE_ID);

        verify(mockRoleRepository, times(5))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(4))
            .create(
                argThat(
                    o ->
                        o.getScope().equals(RoleScope.API) ||
                        o.getScope().equals(RoleScope.APPLICATION) ||
                        o.getScope().equals(RoleScope.ORGANIZATION) ||
                        o.getScope().equals(RoleScope.PLATFORM) ||
                        o.getScope().equals(RoleScope.GROUP)
                )
            );
    }
}
