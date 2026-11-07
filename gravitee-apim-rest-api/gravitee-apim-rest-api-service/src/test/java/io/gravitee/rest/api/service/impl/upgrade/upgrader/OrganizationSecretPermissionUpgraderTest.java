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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationSecretPermissionUpgraderTest {

    @InjectMocks
    OrganizationSecretPermissionUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    void should_refresh_the_system_roles_of_each_organization() throws TechnicalException, UpgraderException {
        Organization orgA = organization("org-a");
        Organization orgB = organization("org-b");
        when(organizationRepository.findAll()).thenReturn(Set.of(orgA, orgB));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService).createOrUpdateSystemRoles(new ExecutionContext(orgA), "org-a");
        verify(roleService).createOrUpdateSystemRoles(new ExecutionContext(orgB), "org-b");
    }

    @Test
    void should_fail_when_the_organizations_cannot_be_read() throws TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new TechnicalException("db error"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
        verify(roleService, never()).createOrUpdateSystemRoles(any(), any());
    }

    @Test
    void should_run_after_the_existing_permission_upgraders() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.ORGANIZATION_SECRET_PERMISSION_UPGRADER);
    }

    private static Organization organization(String id) {
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(id);
        return organization;
    }
}
