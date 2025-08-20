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
package io.gravitee.apim.core.cluster.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@UseCase
public class SearchClusterUseCase {

    private final ClusterQueryService clusterQueryService;
    private final MembershipQueryService membershipQueryService;

    @Builder
    public record Input(String environmentId, Pageable pageable, String sortBy, boolean isAdmin, String userId) {}

    public record Output(Page<Cluster> pageResult) {}

    public Output execute(Input input) {
        ClusterCriteria.ClusterCriteriaBuilder criteriaBuilder = ClusterCriteria.builder().environmentId(input.environmentId);

        if (!input.isAdmin) {
            var clustersIdsUserCanRead = membershipQueryService.findClustersIdsThatUserBelongsTo(input.userId);
            criteriaBuilder.ids(clustersIdsUserCanRead);
        }

        var pageable = Optional.ofNullable(input.pageable).orElse(new PageableImpl(1, 10));

        return new SearchClusterUseCase.Output(
            clusterQueryService.search(criteriaBuilder.build(), pageable, generateSortable(input.sortBy))
        );
    }

    private Optional<Sortable> generateSortable(String sortBy) {
        if (sortBy == null) {
            return Optional.empty();
        }

        boolean isAscending = !sortBy.startsWith("-");
        String field = isAscending ? sortBy : sortBy.substring(1);
        return Optional.of(new SortableImpl(field, isAscending));
    }
}
