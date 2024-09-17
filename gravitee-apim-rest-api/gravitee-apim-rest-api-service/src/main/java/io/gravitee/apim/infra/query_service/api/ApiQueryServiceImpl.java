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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiQueryServiceImpl extends AbstractService implements ApiQueryService {

    private final ApiRepository apiRepository;

    public ApiQueryServiceImpl(@Lazy final ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public Page<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        return this.apiRepository.search(
                apiCriteria == null ? null : ApiSearchCriteriaAdapter.INSTANCE.toCriteriaForRepository(apiCriteria),
                sortable == null ? null : SortableAdapter.INSTANCE.toSortableForRepository(sortable),
                convert(pageable),
                apiFieldFilter == null ? null : ApiFieldFilterAdapter.INSTANCE.toApiFieldFilterForRepository(apiFieldFilter)
            )
            .map(ApiAdapter.INSTANCE::toCoreModel);
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
