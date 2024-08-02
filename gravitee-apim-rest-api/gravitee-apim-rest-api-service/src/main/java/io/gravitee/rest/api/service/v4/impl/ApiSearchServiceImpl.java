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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component("ApiSearchServiceImplV4")
public class ApiSearchServiceImpl extends AbstractService implements ApiSearchService {

    private final ApiRepository apiRepository;
    private final ApiMapper apiMapper;
    private final GenericApiMapper genericApiMapper;
    private final PrimaryOwnerService primaryOwnerService;
    private final CategoryService categoryService;
    private final SearchEngineService searchEngineService;
    private final ApiAuthorizationService apiAuthorizationService;
    private final IntegrationRepository integrationRepository;

    public ApiSearchServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final GenericApiMapper genericApiMapper,
        @Lazy final PrimaryOwnerService primaryOwnerService,
        @Lazy final CategoryService categoryService,
        final SearchEngineService searchEngineService,
        @Lazy final ApiAuthorizationService apiAuthorizationService,
        @Lazy final IntegrationRepository integrationRepository
    ) {
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.genericApiMapper = genericApiMapper;
        this.primaryOwnerService = primaryOwnerService;
        this.categoryService = categoryService;
        this.searchEngineService = searchEngineService;
        this.apiAuthorizationService = apiAuthorizationService;
        this.integrationRepository = integrationRepository;
    }

    @Override
    public ApiEntity findById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findV4RepositoryApiById(executionContext, apiId);
        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), api.getId());
        return apiMapper.toEntity(executionContext, api, primaryOwner, true);
    }

    @Override
    public GenericApiEntity findGenericById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findRepositoryApiById(executionContext, apiId);
        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), api.getId());
        GenericApiEntity genericApi = genericApiMapper.toGenericApi(executionContext, api, primaryOwner);
        return enrichFederatedApi(genericApi);
    }

    @Override
    public Optional<String> findIdByEnvironmentIdAndCrossId(final String environment, final String crossId) {
        try {
            return apiRepository.findIdByEnvironmentIdAndCrossId(environment, crossId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding API by environment " + environment + " and crossId " + crossId,
                e
            );
        }
    }

    @Override
    public boolean exists(final String apiId) {
        try {
            return apiRepository.existById(apiId);
        } catch (final TechnicalException te) {
            final String msg = "An error occurs while checking if the API exists: " + apiId;
            log.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    @Override
    public Set<GenericApiEntity> findGenericByEnvironmentAndIdIn(final ExecutionContext executionContext, final Set<String> apiIds) {
        if (apiIds.isEmpty()) {
            return Set.of();
        }
        ApiCriteria criteria = new ApiCriteria.Builder().ids(apiIds).environmentId(executionContext.getEnvironmentId()).build();
        List<Api> apisFound = apiRepository.search(criteria, ApiFieldFilter.allFields());
        return toGenericApis(executionContext, apisFound);
    }

    @Override
    public Set<GenericApiEntity> findAllGenericByEnvironment(final ExecutionContext executionContext) {
        ApiCriteria criteria = new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build();
        List<Api> apisFound = apiRepository.search(criteria, ApiFieldFilter.allFields());
        return toGenericApis(executionContext, apisFound);
    }

    @Override
    public Api findRepositoryApiById(final ExecutionContext executionContext, final String apiId) {
        try {
            log.debug("Find API by ID: {}", apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);

            if (executionContext.hasEnvironmentId()) {
                optApi = optApi.filter(result -> result.getEnvironmentId().equals(executionContext.getEnvironmentId()));
            }

            return optApi.orElseThrow(() -> new ApiNotFoundException(apiId));
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    @Override
    public Api findV4RepositoryApiById(final ExecutionContext executionContext, final String apiId) {
        Api api = this.findRepositoryApiById(executionContext, apiId);

        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new IllegalArgumentException(
                String.format("Api found doesn't support v%s definition model.", DefinitionVersion.V4.getLabel())
            );
        }

        return api;
    }

    @Override
    public Set<GenericApiEntity> search(ExecutionContext executionContext, final ApiQuery query) {
        return this.search(executionContext, query, false);
    }

    @Override
    public Set<GenericApiEntity> search(ExecutionContext executionContext, final ApiQuery query, final boolean excludeDefinitionV4) {
        log.debug("Search APIs by {}", query);

        Optional<Collection<String>> optionalTargetIds = this.searchInDefinition(executionContext, query, excludeDefinitionV4);

        if (optionalTargetIds.isPresent()) {
            Collection<String> targetIds = optionalTargetIds.get();
            if (targetIds.isEmpty()) {
                return Collections.emptySet();
            }
            query.setIds(targetIds);
        }

        ApiCriteria.Builder queryToCriteria = queryToCriteria(executionContext, query);

        if (excludeDefinitionV4) {
            List<DefinitionVersion> allowedDefinitionVersion = new ArrayList<>();
            allowedDefinitionVersion.add(null);
            allowedDefinitionVersion.add(DefinitionVersion.V1);
            allowedDefinitionVersion.add(DefinitionVersion.V2);
            queryToCriteria.definitionVersion(allowedDefinitionVersion);
        }
        List<Api> apis = apiRepository.search(queryToCriteria.build(), null, ApiFieldFilter.allFields()).collect(toList());
        return this.toGenericApis(executionContext, apis);
    }

    @Override
    public Page<GenericApiEntity> search(
        final ExecutionContext executionContext,
        final String userId,
        final boolean isAdmin,
        final QueryBuilder<ApiEntity> apiQueryBuilder,
        final Pageable pageable,
        final boolean mapToFullGenericApiEntity,
        final boolean manageOnly
    ) {
        // Step 1: find apiIds from lucene indexer from 'query' parameter without any pagination and sorting
        var apiQuery = apiQueryBuilder.build();
        SearchResult apiIdsResult = searchEngineService.search(GraviteeContext.getExecutionContext(), apiQuery);

        if (!apiIdsResult.hasResults() && apiIdsResult.getDocuments().isEmpty()) {
            return new Page<>(List.of(), 0, 0, 0);
        }

        List<String> apiIds = new LinkedList<>(apiIdsResult.getDocuments());

        // Step 2: if user is not admin, get list of apiIds in their scope and join with Lucene results
        if (!isAdmin) {
            Set<String> userApiIds = apiAuthorizationService.findApiIdsByUserId(
                GraviteeContext.getExecutionContext(),
                userId,
                null,
                manageOnly
            );

            // User has no associated apis
            if (userApiIds.isEmpty()) {
                return new Page<>(List.of(), 0, 0, 0);
            }

            apiIds.retainAll(userApiIds);
            if (apiIds.isEmpty()) {
                return new Page<>(List.of(), 0, 0, 0);
            }
        }

        // Step 3: create apiId page subset add filters to ApiCriteria to be used by the repository search
        List<String> apiIdPageSubset = this.getApiIdPageSubset(apiIds, pageable);

        ApiCriteria.Builder apiCriteriaBuilder = new ApiCriteria.Builder()
            .environmentId(GraviteeContext.getExecutionContext().getEnvironmentId())
            .ids(apiIdPageSubset);

        if (Objects.nonNull(apiQuery.getFilters()) && apiQuery.getFilters().containsKey(FIELD_DEFINITION_VERSION)) {
            apiCriteriaBuilder.definitionVersion(
                List.of(DefinitionVersion.valueOfLabel((String) apiQuery.getFilters().get(FIELD_DEFINITION_VERSION)))
            );
        }

        // Step 4: get APIs from repository by ids and add primary owner
        List<Api> apis = apiRepository.search(apiCriteriaBuilder.build(), ApiFieldFilter.allFields());

        if (apis.isEmpty()) {
            return new Page<>(List.of(), 0, 0, 0);
        }

        Map<String, PrimaryOwnerEntity> primaryOwners = primaryOwnerService.getPrimaryOwners(
            executionContext,
            apis.stream().map(Api::getId).toList()
        );

        Comparator<String> orderingComparator = Comparator.comparingInt(apiIdPageSubset::indexOf);

        // Step 5: Map the page ID subset to GenericApis
        Stream<GenericApiEntity> apisStream;

        if (mapToFullGenericApiEntity) {
            apisStream =
                apis
                    .stream()
                    .map(api -> enrichFederatedApi(genericApiMapper.toGenericApi(executionContext, api, primaryOwners.get(api.getId()))));
        } else {
            // Map to simple GenericApiEntity
            apisStream = apis.stream().map(api -> enrichFederatedApi(genericApiMapper.toGenericApi(api, primaryOwners.get(api.getId()))));
        }

        // Step 6: Sort by subset order and add Page information
        return apisStream
            .sorted((o1, o2) -> orderingComparator.compare(o1.getId(), o2.getId()))
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    apiEntityList -> new Page<>(apiEntityList, pageable.getPageNumber(), apis.size(), apiIds.size())
                )
            );
    }

    private List<String> getApiIdPageSubset(Collection<String> ids, Pageable pageable) {
        int numberOfItems = ids.size();
        int numberOfItemPerPage = pageable.getPageSize();

        if (numberOfItemPerPage == 0 || numberOfItems <= 0) {
            return new ArrayList<>();
        }

        int currentPage = pageable.getPageNumber();

        int startIndex = (currentPage - 1) * numberOfItemPerPage;

        if (startIndex >= numberOfItems || currentPage < 1) {
            throw new PaginationInvalidException();
        }
        return ids.stream().skip(startIndex).limit(numberOfItemPerPage).collect(toList());
    }

    /**
     * This method use ApiQuery to search in indexer for fields in api definition
     *
     * @param executionContext
     * @param apiQuery
     * @param excludeV4
     * @return Optional<List < String>> an optional list of api ids and Optional.empty()
     * if ApiQuery doesn't contain fields stores in the api definition.
     */
    private Optional<Collection<String>> searchInDefinition(ExecutionContext executionContext, ApiQuery apiQuery, final boolean excludeV4) {
        if (apiQuery == null) {
            return Optional.empty();
        }
        QueryBuilder<GenericApiEntity> searchEngineQueryBuilder = convert(apiQuery);
        if (excludeV4) {
            searchEngineQueryBuilder.setExcludedFilters(Map.of(FIELD_DEFINITION_VERSION, List.of(DefinitionVersion.V4.getLabel())));
        }
        Query<GenericApiEntity> searchEngineQuery = searchEngineQueryBuilder.build();
        if (isBlank(searchEngineQuery.getQuery())) {
            return Optional.empty();
        }

        SearchResult matchApis = searchEngineService.search(executionContext, searchEngineQuery);
        return Optional.of(matchApis.getDocuments());
    }

    private QueryBuilder<GenericApiEntity> convert(ApiQuery query) {
        QueryBuilder<GenericApiEntity> searchEngineQuery = QueryBuilder.create(GenericApiEntity.class);
        if (query.getIds() != null && !query.getIds().isEmpty()) {
            Map<String, Object> filters = new HashMap<>();
            filters.put("api", query.getIds());
            searchEngineQuery.setFilters(filters);
        }

        if (!isBlank(query.getContextPath())) {
            searchEngineQuery.addExplicitFilter("paths", query.getContextPath());
        }
        if (!isBlank(query.getTag())) {
            searchEngineQuery.addExplicitFilter("tag", query.getTag());
        }
        return searchEngineQuery;
    }

    private ApiCriteria.Builder queryToCriteria(final ExecutionContext executionContext, final ApiQuery query) {
        final ApiCriteria.Builder builder = new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId());
        if (query == null) {
            return builder;
        }
        builder.label(query.getLabel()).name(query.getName()).version(query.getVersion());

        if (!isBlank(query.getCategory())) {
            builder.category(categoryService.findById(query.getCategory(), executionContext.getEnvironmentId()).getId());
        }
        if (query.getGroups() != null && !query.getGroups().isEmpty()) {
            builder.groups(query.getGroups());
        }
        if (!isBlank(query.getState())) {
            builder.state(LifecycleState.valueOf(query.getState()));
        }
        if (query.getVisibility() != null) {
            builder.visibility(Visibility.valueOf(query.getVisibility().name()));
        }
        if (query.getLifecycleStates() != null) {
            builder.lifecycleStates(
                query
                    .getLifecycleStates()
                    .stream()
                    .map(apiLifecycleState -> ApiLifecycleState.valueOf(apiLifecycleState.name()))
                    .collect(toList())
            );
        }
        if (query.getIds() != null && !query.getIds().isEmpty()) {
            builder.ids(query.getIds());
        }
        if (query.getCrossId() != null && !query.getCrossId().isEmpty()) {
            builder.crossId(query.getCrossId());
        }

        return builder;
    }

    @Override
    public Collection<String> searchIds(
        final ExecutionContext executionContext,
        final String query,
        final Map<String, Object> filters,
        final Sortable sortable
    ) {
        return searchIds(executionContext, query, filters, sortable, false);
    }

    @Override
    public Collection<String> searchIds(
        final ExecutionContext executionContext,
        final String query,
        final Map<String, Object> filters,
        final Sortable sortable,
        final boolean excludeV4Definition
    ) {
        QueryBuilder<GenericApiEntity> searchEngineQueryBuilder = QueryBuilder
            .create(GenericApiEntity.class)
            .setSort(sortable)
            .setFilters(filters);
        if (excludeV4Definition) {
            searchEngineQueryBuilder.setExcludedFilters(Map.of(FIELD_DEFINITION_VERSION, List.of(DefinitionVersion.V4.getLabel())));
        }
        if (!isBlank(query)) {
            searchEngineQueryBuilder.setQuery(query);
        }
        SearchResult searchResult = searchEngineService.search(executionContext, searchEngineQueryBuilder.build());
        return searchResult.getDocuments();
    }

    private Set<GenericApiEntity> toGenericApis(final ExecutionContext executionContext, final List<Api> apis) {
        if (apis == null || apis.isEmpty()) {
            return Set.of();
        }
        //find primary owners usernames of each apis
        final List<String> apiIds = apis.stream().map(Api::getId).collect(toList());
        Map<String, PrimaryOwnerEntity> primaryOwners = primaryOwnerService.getPrimaryOwners(executionContext, apiIds);
        Set<String> apiWithoutPo = apiIds.stream().filter(apiId -> !primaryOwners.containsKey(apiId)).collect(toSet());
        Stream<Api> streamApis = apis.stream();
        if (!apiWithoutPo.isEmpty()) {
            String apisAsString = String.join(" / ", apiWithoutPo);
            log.error("{} apis has no identified primary owners in this list {}.", apiWithoutPo.size(), apisAsString);
            streamApis = streamApis.filter(api -> !apiIds.contains(api.getId()));
        }
        return streamApis
            .map(publicApi -> genericApiMapper.toGenericApi(executionContext, publicApi, primaryOwners.get(publicApi.getId())))
            .collect(Collectors.toSet());
    }

    private GenericApiEntity enrichFederatedApi(GenericApiEntity genericApi) {
        try {
            if (genericApi instanceof FederatedApiEntity fede) {
                integrationRepository
                    .findById(fede.getOriginContext().integrationId())
                    .map(integration ->
                        new FederatedApiEntity.OriginContextView(fede.getOriginContext(), integration.getProvider(), integration.getName())
                    )
                    .ifPresent(fede::setOriginContext);
            }
        } catch (TechnicalException e) {
            log.error("Impossible to find integrations", e);
        }
        return genericApi;
    }
}
