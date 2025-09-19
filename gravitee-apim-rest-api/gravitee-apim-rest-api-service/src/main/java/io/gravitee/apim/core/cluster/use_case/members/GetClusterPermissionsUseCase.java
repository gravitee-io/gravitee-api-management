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
package io.gravitee.apim.core.cluster.use_case.members;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.ClusterPermission;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class GetClusterPermissionsUseCase {

    private final MembershipDomainService membershipDomainService;

    public record Input(boolean isAuthenticated, boolean isAdmin, String authenticatedUser, String clusterId) {}

    public record Output(Map<String, char[]> permissions) {}

    public Output execute(Input input) {
        Map<String, char[]> permissions = new HashMap<>();
        if (input.isAuthenticated) {
            if (input.isAdmin) {
                final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };
                for (ClusterPermission perm : ClusterPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                final String username = input.authenticatedUser;
                final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
                permissions = membershipDomainService.getUserMemberPermissions(
                    executionContext,
                    MembershipReferenceType.CLUSTER,
                    input.clusterId,
                    username
                );
            }
        }
        return new Output(permissions);
    }
}
