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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public ApiSearchServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final GenericApiMapper genericApiMapper,
        @Lazy final PrimaryOwnerService primaryOwnerService,
        @Lazy final CategoryService categoryService
    ) {
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.genericApiMapper = genericApiMapper;
        this.primaryOwnerService = primaryOwnerService;
        this.categoryService = categoryService;
    }

    @Override
    public ApiEntity findById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findApiById(executionContext, apiId, true);
        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());
        return apiMapper.toEntity(executionContext, api, primaryOwner, null, true);
    }

    @Override
    public GenericApiEntity findGenericById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findApiById(executionContext, apiId, false);
        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());
        final List<CategoryEntity> categories = categoryService.findAll(executionContext.getEnvironmentId());
        return genericApiMapper.toGenericApi(executionContext, api, primaryOwner, categories);
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
            return Collections.emptySet();
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
        return this.findApiById(executionContext, apiId, true);
    }

    private Api findApiById(final ExecutionContext executionContext, final String apiId, boolean throwWhenNotV4) {
        try {
            log.debug("Find API by ID: {}", apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);

            if (executionContext.hasEnvironmentId()) {
                optApi = optApi.filter(result -> result.getEnvironmentId().equals(executionContext.getEnvironmentId()));
            }

            final Api api = optApi.orElseThrow(() -> new ApiNotFoundException(apiId));

            if (throwWhenNotV4 && api.getDefinitionVersion() != DefinitionVersion.V4) {
                throw new IllegalArgumentException(
                    String.format("Api found doesn't support v%s definition model.", DefinitionVersion.V4.getLabel())
                );
            }

            return api;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    private Set<GenericApiEntity> toGenericApis(final ExecutionContext executionContext, final List<Api> apis) {
        if (apis == null || apis.isEmpty()) {
            return Collections.emptySet();
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
        final List<CategoryEntity> categories = categoryService.findAll(executionContext.getEnvironmentId());

        return streamApis
            .map(publicApi -> genericApiMapper.toGenericApi(executionContext, publicApi, primaryOwners.get(publicApi.getId()), categories))
            .collect(Collectors.toSet());
    }
}
