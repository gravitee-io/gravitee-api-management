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
package inmemory;

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupHistoryQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SharedPolicyGroupHistoryQueryServiceInMemory
    implements SharedPolicyGroupHistoryQueryService, InMemoryAlternative<SharedPolicyGroup> {

    final ArrayList<SharedPolicyGroup> storage = new ArrayList<>();

    @Override
    public Stream<SharedPolicyGroup> streamLatestBySharedPolicyGroupId(String environmentId) {
        return storage.stream().filter(spg -> spg.getEnvironmentId().equals(environmentId));
    }

    @Override
    public Page<SharedPolicyGroup> search(String environmentId, String sharedPolicyGroupId, Pageable pageable, Sortable sortable) {
        // Search, sort, and paginate the shared policy groups from storage
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();
        var sortableField = sortable.getField();
        var isAscending = sortable.isAscOrder();

        var stream = storage
            .stream()
            .filter(spg -> spg.getEnvironmentId().equals(environmentId))
            .filter(spg -> sharedPolicyGroupId == null || spg.getId().equals(sharedPolicyGroupId));

        stream =
            switch (sortableField) {
                case "version" -> stream.sorted(Comparator.comparing(SharedPolicyGroup::getVersion));
                case "deployedAt" -> stream.sorted(Comparator.comparing(SharedPolicyGroup::getDeployedAt));
                default -> stream.sorted(Comparator.comparing(SharedPolicyGroup::getUpdatedAt));
            };

        var matches = stream.collect(Collectors.toList());

        if (!isAscending) {
            Collections.reverse(matches);
        }

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public Optional<SharedPolicyGroup> getLatestBySharedPolicyGroupId(String environmentId, String sharedPolicyGroupId) {
        return storage
            .stream()
            .filter(spg -> spg.getEnvironmentId().equals(environmentId))
            .filter(spg -> spg.getId().equals(sharedPolicyGroupId))
            .max(Comparator.comparing(SharedPolicyGroup::getUpdatedAt));
    }

    @Override
    public void initWith(List<SharedPolicyGroup> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<SharedPolicyGroup> storage() {
        return Collections.unmodifiableList(storage);
    }
}
