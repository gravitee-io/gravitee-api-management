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
package io.gravitee.apim.infra.crud_service.api;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ApiCrudServiceImpl implements ApiCrudService {

    private static final Logger logger = LoggerFactory.getLogger(ApiCrudServiceImpl.class);
    private final ApiRepository apiRepository;

    public ApiCrudServiceImpl(@Lazy ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public Api get(String id) {
        try {
            var foundApi = apiRepository.findById(id);
            if (foundApi.isPresent()) {
                return ApiAdapter.INSTANCE.toCoreModel(foundApi.get());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Api by id {}", id, e);
        }
        throw new ApiNotFoundException(id);
    }

    @Override
    public Optional<Api> findById(String id) {
        try {
            logger.debug("Find an Api by id : {}", id);
            return apiRepository.findById(id).map(ApiAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException(String.format("An error occurs while trying to find an api by id: %s", id), ex);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            return apiRepository.existById(id);
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Api by id {}", id, e);
        }
        return false;
    }

    @Override
    public Api create(Api api) {
        try {
            return ApiAdapter.INSTANCE.toCoreModel(apiRepository.create(ApiAdapter.INSTANCE.toRepository(api)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to create the api: " + api.getId(), e);
        }
    }

    @Override
    public Api update(Api api) {
        try {
            return ApiAdapter.INSTANCE.toCoreModel(apiRepository.update(ApiAdapter.INSTANCE.toRepository(api)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to update the api: " + api.getId(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            apiRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to delete the api: " + id, e);
        }
    }
}
