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
package io.gravitee.apim.core.cluster.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import lombok.AllArgsConstructor;

/**
 * Decides how much of a cluster configuration a user may see. The configuration holds the
 * credentials used to reach the cluster, so only users allowed to edit it get it in full.
 */
@DomainService
@AllArgsConstructor
public class ClusterConfigurationAccessDomainService {

    private final PermissionDomainService permissionDomainService;

    /**
     * Cluster as returned by single-cluster operations: full configuration for users who can edit
     * it, configuration without credentials for users who can only read it, no configuration
     * otherwise. The given cluster is never mutated.
     */
    public Cluster visibleTo(Cluster cluster, String organizationId, String userId) {
        if (can(cluster, organizationId, userId, RolePermissionAction.UPDATE)) {
            return cluster;
        }
        return forReader(cluster, organizationId, userId);
    }

    /**
     * Cluster as returned in a list: never with credentials, and without configuration for users
     * who cannot read it. The given cluster is never mutated.
     */
    public Cluster visibleInListTo(Cluster cluster, String organizationId, String userId) {
        return forReader(cluster, organizationId, userId);
    }

    private Cluster forReader(Cluster cluster, String organizationId, String userId) {
        if (can(cluster, organizationId, userId, RolePermissionAction.READ)) {
            return cluster.withoutCredentials();
        }
        return cluster.toBuilder().configuration(null).build();
    }

    private boolean can(Cluster cluster, String organizationId, String userId, RolePermissionAction action) {
        return permissionDomainService.hasPermission(organizationId, userId, RolePermission.CLUSTER_CONFIGURATION, cluster.getId(), action);
    }
}
