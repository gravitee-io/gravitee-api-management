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

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ApiQueryServiceInMemory implements ApiQueryService, InMemoryAlternative<Api> {

    private static final Comparator<Api> MOST_RECENTLY_UPDATED_FIRST = Comparator.comparing(Api::getUpdatedAt).reversed();

    private final List<Api> storage;

    public ApiQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ApiQueryServiceInMemory(ApiCrudServiceInMemory apiCrudServiceInMemory) {
        storage = apiCrudServiceInMemory.storage;
    }

    @Override
    public Page<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = this.storage()
            .stream()
            .filter(api -> {
                var matchesIntegrationId =
                    isNull(apiCriteria) ||
                    isNull(apiCriteria.getIntegrationId()) ||
                    Objects.equals(((OriginContext.Integration) api.getOriginContext()).integrationId(), apiCriteria.getIntegrationId());
                var matchesApiId = isNull(apiCriteria) || isNull(apiCriteria.getIds()) || apiCriteria.getIds().contains(api.getId());
                var matchesEnvironmentId =
                    isNull(apiCriteria) ||
                    isNull(apiCriteria.getEnvironmentId()) ||
                    apiCriteria.getEnvironmentId().equals(api.getEnvironmentId());
                var matchesLifecycleState =
                    isNull(apiCriteria) ||
                    isNull(apiCriteria.getLifecycleStates()) ||
                    apiCriteria.getLifecycleStates().contains(api.getApiLifecycleState());
                var matchesGroups =
                    isNull(apiCriteria) ||
                    isNull(apiCriteria.getGroups()) ||
                    apiCriteria.getGroups().isEmpty() ||
                    (api.getGroups() != null && !Collections.disjoint(api.getGroups(), apiCriteria.getGroups()));
                return (matchesIntegrationId && matchesApiId && matchesLifecycleState && matchesEnvironmentId && matchesGroups);
            })
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    /**
     * WARNING: this implementation doesn't actually filter the API present in the storage. Instead, it will return all applications from storage.
     * Except for the integrationId where filtering is implemented.
     */
    @Override
    public Stream<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
        if (
            apiCriteria != null &&
            (apiCriteria.getIntegrationId() != null ||
                apiCriteria.getIds() != null ||
                apiCriteria.getDefinitionVersion() != null ||
                apiCriteria.getEnvironmentId() != null ||
                (apiCriteria.getGroups() != null && !apiCriteria.getGroups().isEmpty()))
        ) {
            return this.storage()
                .stream()
                .filter(api -> {
                    var matchesIntegrationId =
                        isNull(apiCriteria.getIntegrationId()) ||
                        Objects.equals(
                            ((OriginContext.Integration) api.getOriginContext()).integrationId(),
                            apiCriteria.getIntegrationId()
                        );
                    var matchesApiId = isNull(apiCriteria.getIds()) || apiCriteria.getIds().contains(api.getId());
                    var matchesLifecycleState =
                        isNull(apiCriteria.getLifecycleStates()) || apiCriteria.getLifecycleStates().contains(api.getApiLifecycleState());
                    var matchesApiDefinitionVersion =
                        isNull(apiCriteria.getDefinitionVersion()) ||
                        apiCriteria.getDefinitionVersion().contains(api.getDefinitionVersion());
                    var matchesEnvironmentId =
                        isNull(apiCriteria.getEnvironmentId()) || apiCriteria.getEnvironmentId().equals(api.getEnvironmentId());
                    var matchesGroups =
                        isNull(apiCriteria.getGroups()) ||
                        apiCriteria.getGroups().isEmpty() ||
                        (api.getGroups() != null && !Collections.disjoint(api.getGroups(), apiCriteria.getGroups()));
                    return (
                        matchesIntegrationId &&
                        matchesApiId &&
                        matchesLifecycleState &&
                        matchesApiDefinitionVersion &&
                        matchesEnvironmentId &&
                        matchesGroups
                    );
                });
        }
        return this.storage().stream();
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) {
        return storage
            .stream()
            .filter(api -> environmentId.equals(api.getEnvironmentId()) && Objects.equals(api.getCrossId(), crossId))
            .findFirst();
    }

    @Override
    public Page<Api> findByIntegrationId(String integrationId, Pageable pageable) {
        return pageOf(apisOwnedBy(integrationId).sorted(MOST_RECENTLY_UPDATED_FIRST).toList(), pageable);
    }

    @Override
    public Page<Api> searchByIntegrationId(
        String integrationId,
        List<DefinitionVersion> definitionVersions,
        String query,
        Pageable pageable
    ) {
        var matches = apisOwnedBy(integrationId)
            .filter(api -> matchesRequestedDefinitionVersion(api, definitionVersions))
            .filter(api -> matchesFreeTextQuery(api, query))
            .sorted(MOST_RECENTLY_UPDATED_FIRST)
            .toList();

        return pageOf(matches, pageable);
    }

    private Stream<Api> apisOwnedBy(String integrationId) {
        return storage
            .stream()
            .filter(api -> api.getOriginContext() instanceof OriginContext.Integration inte && integrationId.equals(inte.integrationId()));
    }

    private static boolean matchesRequestedDefinitionVersion(Api api, List<DefinitionVersion> definitionVersions) {
        if (definitionVersions == null || definitionVersions.isEmpty()) {
            return true;
        }
        return definitionVersions.contains(Objects.requireNonNullElse(api.getDefinitionVersion(), DefinitionVersion.V2));
    }

    private static boolean matchesFreeTextQuery(Api api, String query) {
        if (isBlank(query)) {
            return true;
        }
        var searchedText = query.trim().toLowerCase();
        return freeTextSearchableValuesOf(api).anyMatch(value -> value.toLowerCase().contains(searchedText));
    }

    private static Stream<String> freeTextSearchableValuesOf(Api api) {
        var providerOrganization = api.getApiDefinitionValue() instanceof FederatedAgent agent && agent.getProvider() != null
            ? agent.getProvider().organization()
            : null;
        return Stream.of(api.getName(), api.getDescription(), providerOrganization).filter(Objects::nonNull);
    }

    private static Page<Api> pageOf(List<Api> matches, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public void initWith(List<Api> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Api> storage() {
        return Collections.unmodifiableList(storage);
    }
}
