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
import io.gravitee.common.data.domain.Page;
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

    public Output execute(Input input) {
        var pageable = input.pageable != null ? input.pageable : new PageableImpl(1, 10);

        return new SearchClusterUseCase.Output(
            clusterQueryService.searchByEnvironmentId(input.environmentId, pageable, generateSortable(input.sortBy))
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

    @Builder
    public record Input(String environmentId, Pageable pageable, String sortBy) {}

    public record Output(Page<Cluster> pageResult) {}
}
