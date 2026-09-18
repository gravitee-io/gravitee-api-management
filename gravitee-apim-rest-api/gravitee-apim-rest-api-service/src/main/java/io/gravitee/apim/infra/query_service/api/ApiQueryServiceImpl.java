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
package io.gravitee.apim.infra.query_service.api;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.ApiFieldFilterAdapter;
import io.gravitee.apim.infra.adapter.ApiSearchCriteriaAdapter;
import io.gravitee.apim.infra.adapter.SortableAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@CustomLog
@Service
public class ApiQueryServiceImpl extends AbstractService implements ApiQueryService {

    /** Keeps each {@code ids} criteria below the bind parameter limit a relational repository turns it into. */
    private static final int MAX_IDS_PER_REPOSITORY_QUERY = 500;

    private final ApiRepository apiRepository;
    private final ApiDocumentSearcher apiDocumentSearcher;

    public ApiQueryServiceImpl(@Lazy final ApiRepository apiRepository, final ApiDocumentSearcher apiDocumentSearcher) {
        this.apiRepository = apiRepository;
        this.apiDocumentSearcher = apiDocumentSearcher;
    }

    @Override
    public Page<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        return this.apiRepository.search(
            apiCriteria == null ? null : ApiSearchCriteriaAdapter.INSTANCE.toCriteriaForRepository(apiCriteria),
            sortable == null ? null : SortableAdapter.INSTANCE.toSortableForRepository(sortable),
            convert(pageable),
            apiFieldFilter == null ? null : ApiFieldFilterAdapter.INSTANCE.toApiFieldFilterForRepository(apiFieldFilter)
        ).map(ApiAdapter.INSTANCE::toCoreModel);
    }

    @Override
    public Stream<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
        return ApiAdapter.INSTANCE.toCoreModelStream(
            this.apiRepository.search(
                apiCriteria == null ? null : ApiSearchCriteriaAdapter.INSTANCE.toCriteriaForRepository(apiCriteria),
                sortable == null ? null : SortableAdapter.INSTANCE.toSortableForRepository(sortable),
                apiFieldFilter == null ? null : ApiFieldFilterAdapter.INSTANCE.toApiFieldFilterForRepository(apiFieldFilter)
            )
        );
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) {
        try {
            return apiRepository.findByEnvironmentIdAndCrossId(environmentId, crossId).map(ApiAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public Page<Api> findByIntegrationId(String integrationId, Pageable pageable) {
        var searchCriteria = new ApiCriteria.Builder().integrationId(integrationId).build();

        return hydrate(searchCriteria, pageable);
    }

    /**
     * {@code total} is the index's exact hit count and the page is the index's window: an id the index still holds
     * for an api whose row was deleted without a reindex inflates {@code total} and shortens that page. That
     * divergence is accepted here rather than reconciled against the repository, matching {@code
     * AbstractDocumentSearcher#searchWindow}/{@code UserDocumentSearcher#search}.
     */
    @Override
    public Page<Api> searchByIntegrationId(
        String environmentId,
        String integrationId,
        List<DefinitionVersion> definitionVersions,
        String query,
        Pageable pageable
    ) {
        var scopedEnvironmentId = requiredNonBlank(environmentId, "An environment id is required to search the apis of an integration");
        var owningIntegrationId = requiredNonBlank(integrationId, "An integration id is required to search the apis of an integration");
        var narrowedDefinitionVersions = requiredNonNullDefinitionVersions(definitionVersions);
        var matchedPage = searchIndexedApiIdPage(scopedEnvironmentId, owningIntegrationId, narrowedDefinitionVersions, query, pageable);
        var matchedIdsOfThePage = List.copyOf(matchedPage.getDocuments());
        if (matchedIdsOfThePage.isEmpty()) {
            return new Page<>(List.of(), repositoryPageNumberOf(pageable), 0, matchedPage.getHits());
        }

        var content = hydrateWindow(scopedEnvironmentId, matchedIdsOfThePage);

        return new Page<>(content, repositoryPageNumberOf(pageable), content.size(), matchedPage.getHits());
    }

    private SearchResult searchIndexedApiIdPage(
        String environmentId,
        String integrationId,
        List<DefinitionVersion> definitionVersions,
        String query,
        Pageable pageable
    ) {
        try {
            return apiDocumentSearcher.searchByIntegrationId(environmentId, integrationId, definitionVersions, query, pageable);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private List<Api> hydrateWindow(String environmentId, List<String> apiIds) {
        var hydrated = batchesOf(apiIds)
            .flatMap(batch ->
                hydrate(new ApiCriteria.Builder().environmentId(environmentId).ids(batch).build(), null).getContent().stream()
            )
            .toList();

        return inIdOrder(hydrated, apiIds);
    }

    private static String requiredNonBlank(String value, String missingValueMessage) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        throw new IllegalArgumentException(missingValueMessage);
    }

    private static List<DefinitionVersion> requiredNonNullDefinitionVersions(List<DefinitionVersion> definitionVersions) {
        if (definitionVersions != null && definitionVersions.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A definition version to narrow the search of an integration to cannot be null");
        }
        return definitionVersions;
    }

    private static Stream<List<String>> batchesOf(List<String> apiIds) {
        return IntStream.iterate(0, from -> from < apiIds.size(), from -> from + MAX_IDS_PER_REPOSITORY_QUERY).mapToObj(from ->
            apiIds.subList(from, Math.min(from + MAX_IDS_PER_REPOSITORY_QUERY, apiIds.size()))
        );
    }

    private static List<Api> inIdOrder(List<Api> apis, List<String> apiIds) {
        var byId = apis.stream().collect(Collectors.toMap(Api::getId, Function.identity()));

        return apiIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static int repositoryPageNumberOf(Pageable pageable) {
        var repositoryPageable = convert(pageable);
        return repositoryPageable == null ? 0 : repositoryPageable.pageNumber();
    }

    private Page<Api> hydrate(ApiCriteria searchCriteria, Pageable pageable) {
        var sortable = SortableAdapter.INSTANCE.toSortableForRepository(
            Sortable.builder().field("updatedAt").order(Sortable.Order.DESC).build()
        );
        var fieldFilter = new io.gravitee.repository.management.api.search.ApiFieldFilter.Builder()
            .excludeDefinition()
            .excludePicture()
            .build();

        return apiRepository.search(searchCriteria, sortable, convert(pageable), fieldFilter).map(ApiAdapter.INSTANCE::toCoreModel);
    }
}
