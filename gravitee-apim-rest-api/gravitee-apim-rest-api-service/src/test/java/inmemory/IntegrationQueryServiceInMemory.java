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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class IntegrationQueryServiceInMemory implements IntegrationQueryService, InMemoryAlternative<Integration> {

    private final List<Integration> storage;

    public IntegrationQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public IntegrationQueryServiceInMemory(IntegrationCrudServiceInMemory integrationCrudServiceInMemory) {
        storage = integrationCrudServiceInMemory.storage;
    }

    @Override
    public Page<Integration> findByEnvironment(String environmentId, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(integration -> integration.getEnvironmentId().equals(environmentId))
            .sorted(Comparator.comparing(Integration::getUpdatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public Page<Integration> findByEnvironmentAndGroups(String environmentId, Collection<String> groups, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(integration -> integration.getEnvironmentId().equals(environmentId))
            .filter(integration -> stream(integration.getGroups()).anyMatch(groups::contains))
            .sorted(Comparator.comparing(Integration::getUpdatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public void initWith(List<Integration> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Integration> storage() {
        return Collections.unmodifiableList(storage);
    }
}
