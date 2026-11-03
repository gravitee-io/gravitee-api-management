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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService.ClusterUse;
import io.gravitee.apim.core.cluster.exception.ClusterBindingNotAllowedException;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Only the members of a registered Kafka cluster, with {@code CLUSTER_DEFINITION[READ]} on it, can bind an API to it: the
 * gateway then uses the cluster's credentials on behalf of that API. Org admins pass. Only the bindings a save adds, or
 * whose connection it changes, are checked: an API keeps the bindings it has, whoever edits it, and dropping one needs no
 * permission. Virtual clusters are not checked.
 */
@DomainService
@RequiredArgsConstructor
public class ClusterBindingPermissionDomainService {

    private final ClusterQueryService clusterQueryService;
    private final PermissionDomainService permissionDomainService;
    private final ObjectMapper objectMapper;

    /**
     * Throws {@link ClusterBindingNotAllowedException} naming the first cluster, by cross id, that {@code uses} binds and
     * {@code previousUses} did not bind with the same connection, when {@code userId} is not a member of it. No-op without
     * a user, as for system calls. Unknown clusters are left to the binding validation.
     */
    public void validate(
        String organizationId,
        String environmentId,
        @Nullable String userId,
        Collection<ClusterUse> previousUses,
        Collection<ClusterUse> uses
    ) {
        if (userId == null) {
            return;
        }
        var previous = new HashSet<>(previousUses);
        var changedClusterCrossIds = uses
            .stream()
            .filter(use -> !previous.contains(use))
            .map(ClusterUse::clusterCrossId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));
        for (String crossId : changedClusterCrossIds) {
            clusterQueryService
                .findByCrossIdAndEnvironmentId(crossId, environmentId)
                .filter(cluster -> !canBind(organizationId, userId, cluster))
                .ifPresent(cluster -> {
                    throw new ClusterBindingNotAllowedException(cluster.getName());
                });
        }
    }

    /** {@link #validate(String, String, String, Collection, Collection)} for a Kafka Native API definition replacing {@code previous}, {@code null} on create. */
    public void validate(
        String organizationId,
        String environmentId,
        @Nullable String userId,
        @Nullable NativeApi previous,
        NativeApi definition
    ) {
        validate(
            organizationId,
            environmentId,
            userId,
            ClusterBoundApisQueryService.clusterUses(previous, objectMapper),
            ClusterBoundApisQueryService.clusterUses(definition, objectMapper)
        );
    }

    /**
     * Whether {@code userId} may bind an API to {@code cluster}. Always for a cluster other than a registered Kafka
     * cluster, or without a user.
     */
    public boolean canBind(String organizationId, @Nullable String userId, Cluster cluster) {
        return (
            userId == null ||
            cluster.getType() != ClusterType.KAFKA_CLUSTER ||
            permissionDomainService.hasPermission(
                organizationId,
                userId,
                RolePermission.CLUSTER_DEFINITION,
                cluster.getId(),
                RolePermissionAction.READ
            )
        );
    }
}
