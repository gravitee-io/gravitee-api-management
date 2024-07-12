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

import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultEnvironmentFlowsRolesUpgraderTest {

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    DefaultEnvironmentFlowsRolesUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new DefaultEnvironmentFlowsRolesUpgrader(roleService, organizationRepository);
    }

    @Test
    public void upgrade_add_default_environment_flows_roles() throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id("DEFAULT").build()));

        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), any(), eq("DEFAULT")))
            .thenAnswer(args -> {
                if (args.getArgument(1).equals(DEFAULT_ROLE_ENVIRONMENT_USER.getName())) {
                    return Optional.of(
                        RoleEntity.builder().name(DEFAULT_ROLE_ENVIRONMENT_USER.getName()).permissions(new HashMap<>(Map.of())).build()
                    );
                }

                if (args.getArgument(1).equals(ROLE_ENVIRONMENT_API_PUBLISHER.getName())) {
                    return Optional.of(
                        RoleEntity.builder().name(ROLE_ENVIRONMENT_API_PUBLISHER.getName()).permissions(new HashMap<>(Map.of())).build()
                    );
                }
                throw new TechnicalException("error");
            });

        when(
            roleService.update(
                any(),
                argThat(role -> {
                    if (
                        role.getName().equals(DEFAULT_ROLE_ENVIRONMENT_USER.getName()) &&
                        role.getPermissions().containsKey(EnvironmentPermission.ENVIRONMENT_FLOWS.getName())
                    ) {
                        return true;
                    }
                    return (
                        role.getName().equals(ROLE_ENVIRONMENT_API_PUBLISHER.getName()) &&
                        role.getPermissions().containsKey(EnvironmentPermission.ENVIRONMENT_FLOWS.getName())
                    );
                })
            )
        )
            .thenReturn(RoleEntity.builder().build());

        upgrader.upgrade();

        verify(roleService, times(2)).update(any(), any());
    }

    @Test
    public void do_nothing_if_role_not_found() throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id("DEFAULT").build()));

        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), any(), eq("DEFAULT")))
            .thenAnswer(args -> {
                if (args.getArgument(1).equals(DEFAULT_ROLE_ENVIRONMENT_USER.getName())) {
                    return Optional.empty();
                }

                if (args.getArgument(1).equals(ROLE_ENVIRONMENT_API_PUBLISHER.getName())) {
                    return Optional.empty();
                }
                throw new TechnicalException("error");
            });

        upgrader.upgrade();

        verify(roleService, times(0)).update(any(), any());
    }
}
