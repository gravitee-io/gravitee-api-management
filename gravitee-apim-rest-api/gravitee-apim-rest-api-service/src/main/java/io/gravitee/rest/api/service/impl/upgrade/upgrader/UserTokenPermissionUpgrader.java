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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserTokenPermissionUpgrader implements Upgrader {

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public UserTokenPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.USER_TOKEN_PERMISSION_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    roleService
                        .findByScope(RoleScope.ORGANIZATION, organization.getId())
                        .stream()
                        .filter(role -> !SystemRole.ADMIN.name().equalsIgnoreCase(role.getName()))
                        .forEach(role -> processRolePermissions(role, organization));
                });
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to upgrade user token permissions", e);
            return false;
        }
        return true;
    }

    private void processRolePermissions(RoleEntity role, Organization organization) {
        Set<Character> userTokenPermission = new HashSet<>();
        Map<String, char[]> permissions = role.getPermissions();

        if (permissions != null) {
            char[] userPermission = permissions.get(OrganizationPermission.USER.getName());
            if (userPermission != null) {
                for (char permission : userPermission) {
                    switch (permission) {
                        case 'R' -> userTokenPermission.addAll(Set.of('R', 'C', 'D'));
                        case 'C' -> userTokenPermission.add('C');
                        case 'D' -> userTokenPermission.add('D');
                        case 'U' -> userTokenPermission.add('U');
                    }
                }
            }

            permissions.put(OrganizationPermission.USER_TOKEN.getName(), toCharArray(userTokenPermission));
            role.setPermissions(permissions);
            roleService.update(new ExecutionContext(organization), convert(role));
        }
    }

    private char[] toCharArray(Set<Character> charSet) {
        char[] charArray = new char[charSet.size()];
        int i = 0;
        for (Character ch : charSet) {
            charArray[i++] = ch;
        }
        return charArray;
    }

    private UpdateRoleEntity convert(RoleEntity role) {
        UpdateRoleEntity updateRoleEntity = new UpdateRoleEntity();
        updateRoleEntity.setId(role.getId());
        updateRoleEntity.setName(role.getName());
        updateRoleEntity.setDescription(role.getDescription());
        updateRoleEntity.setDefaultRole(role.isDefaultRole());
        updateRoleEntity.setPermissions(role.getPermissions());
        updateRoleEntity.setScope(role.getScope());
        return updateRoleEntity;
    }
}
