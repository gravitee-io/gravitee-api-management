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
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.ClusterSearchCriteria;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Counts the environment's clusters by lifecycle state — the numbers behind the console's cluster
 * stat-strip (Total / Deployed / Pending / Undeployed). Kept independent of the search text and the
 * lifecycle facet: it always reports the whole (type-scoped, membership-visible) inventory so the
 * strip stays correct even when the list is paginated and filtered server-side.
 *
 * <p>Reuses the existing {@code search} + {@code lifecycleState} filter (one count query per bucket)
 * rather than a bespoke aggregation, so there is nothing new to keep in sync in the repositories.
 */
@AllArgsConstructor
@UseCase
public class CountClustersByLifecycleStateUseCase {

    private final ClusterQueryService clusterQueryService;
    private final MembershipQueryService membershipQueryService;

    @Builder
    public record Input(String environmentId, ClusterType type, boolean isAdmin, String userId) {}

    public record Output(long total, long deployed, long pending, long undeployed) {}

    public Output execute(Input input) {
        List<String> readableIds = null;
        if (!input.isAdmin()) {
            var clustersIdsUserCanRead = membershipQueryService.findClustersIdsThatUserBelongsTo(input.userId());
            if (clustersIdsUserCanRead.isEmpty()) {
                // The user is a member of no cluster — nothing to count.
                return new Output(0, 0, 0, 0);
            }
            readableIds = clustersIdsUserCanRead;
        }

        return new Output(
            count(input, readableIds, null),
            count(input, readableIds, List.of(ClusterLifecycleState.DEPLOYED.name())),
            count(input, readableIds, List.of(ClusterLifecycleState.PENDING.name())),
            count(input, readableIds, List.of(ClusterLifecycleState.UNDEPLOYED.name()))
        );
    }

    private long count(Input input, List<String> ids, List<String> lifecycleStates) {
        var criteria = ClusterSearchCriteria.builder()
            .environmentId(input.environmentId())
            .type(input.type())
            .ids(ids)
            .lifecycleStates(lifecycleStates)
            .build();
        // pageSize 1 — only the total count is read, the page content is ignored
        return clusterQueryService.search(criteria, new PageableImpl(1, 1), Optional.empty()).getTotalElements();
    }
}
