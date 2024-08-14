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
package fixtures.core.model;

import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.model.Role.ReferenceType;
import io.gravitee.rest.api.model.permissions.SystemRole;
import java.time.Instant;
import java.time.ZoneId;

public class RoleFixtures {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    private RoleFixtures() {}

    public static String organizationAdminRoleId(String organizationId) {
        return "org-admin-" + organizationId;
    }

    public static String environmentAdminRoleId(String organizationId) {
        return "env-admin-" + organizationId;
    }

    public static String groupAdminRoleId(String organizationId) {
        return "group-admin-" + organizationId;
    }

    public static String apiPrimaryOwnerRoleId(String organizationId) {
        return "api-po-id-" + organizationId;
    }

    public static String applicationPrimaryOwnerRoleId(String organizationId) {
        return "app-po-id-" + organizationId;
    }

    public static String integrationPrimaryOwnerRoleId(String integrationId) {
        return "int-po-id-" + integrationId;
    }

    public static Role anOrganizationAdminRole(String organizationId) {
        return systemRoleBuilder()
            .id(organizationAdminRoleId(organizationId))
            .name(SystemRole.ADMIN.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.ORGANIZATION)
            .build();
    }

    public static Role anEnvironmentAdminRole(String organizationId) {
        return systemRoleBuilder()
            .id(environmentAdminRoleId(organizationId))
            .name(SystemRole.ADMIN.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.ENVIRONMENT)
            .build();
    }

    public static Role aGroupAdminRole(String organizationId) {
        return systemRoleBuilder()
            .id(groupAdminRoleId(organizationId))
            .name(SystemRole.ADMIN.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.GROUP)
            .build();
    }

    public static Role anApiPrimaryOwnerRole(String organizationId) {
        return systemRoleBuilder()
            .id(apiPrimaryOwnerRoleId(organizationId))
            .name(SystemRole.PRIMARY_OWNER.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.API)
            .build();
    }

    public static Role anApplicationPrimaryOwnerRole(String organizationId) {
        return systemRoleBuilder()
            .id(applicationPrimaryOwnerRoleId(organizationId))
            .name(SystemRole.PRIMARY_OWNER.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.APPLICATION)
            .build();
    }

    public static Role anIntegrationPrimaryOwnerRole(String organizationId) {
        return systemRoleBuilder()
            .id(integrationPrimaryOwnerRoleId(organizationId))
            .name(SystemRole.PRIMARY_OWNER.name())
            .referenceId(organizationId)
            .referenceType(ReferenceType.ORGANIZATION)
            .scope(Role.Scope.INTEGRATION)
            .build();
    }

    private static Role.RoleBuilder systemRoleBuilder() {
        return Role
            .builder()
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .defaultRole(false)
            .system(true);
    }
}
