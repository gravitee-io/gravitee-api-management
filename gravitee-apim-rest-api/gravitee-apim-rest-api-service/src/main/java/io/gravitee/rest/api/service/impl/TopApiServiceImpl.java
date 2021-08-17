/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.parameters.Key.PORTAL_TOP_APIS;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.rest.api.model.NewTopApiEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.UpdateTopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.TopApiService;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TopApiServiceImpl extends TransactionalService implements TopApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopApiServiceImpl.class);

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiService apiService;

    @Override
    public List<TopApiEntity> findAll() {
        LOGGER.debug("Find all top APIs");
        final List<ApiEntity> apis = parameterService.findAll(
            PORTAL_TOP_APIS,
            apiId -> apiService.findById(apiId),
            apiService::exists,
            ParameterReferenceType.ENVIRONMENT
        );
        if (!apis.isEmpty()) {
            final List<TopApiEntity> topApis = new ArrayList<>(apis.size());
            for (int i = 0; i < apis.size(); i++) {
                final ApiEntity api = apis.get(i);

                final TopApiEntity topApiEntity = new TopApiEntity();
                topApiEntity.setApi(api.getId());
                topApiEntity.setName(api.getName());
                topApiEntity.setVersion(api.getVersion());
                topApiEntity.setDescription(api.getDescription());
                topApiEntity.setOrder(i);

                topApis.add(topApiEntity);
            }
            return topApis;
        }
        return emptyList();
    }

    @Override
    public List<TopApiEntity> create(final NewTopApiEntity topApi) {
        final List<String> existingTopApis = parameterService.findAll(PORTAL_TOP_APIS, ParameterReferenceType.ENVIRONMENT);
        if (existingTopApis.contains(topApi.getApi())) {
            throw new IllegalArgumentException("The API is already defined on top APIs");
        }
        ArrayList<String> newTopApis = new ArrayList<>(existingTopApis);
        newTopApis.add(topApi.getApi());
        parameterService.save(PORTAL_TOP_APIS, newTopApis, ParameterReferenceType.ENVIRONMENT);
        return findAll();
    }

    @Override
    public List<TopApiEntity> update(final List<UpdateTopApiEntity> topApis) {
        final List<String> existingTopApis = parameterService.findAll(PORTAL_TOP_APIS, ParameterReferenceType.ENVIRONMENT);
        final List<String> updatingTopApis = topApis.stream().map(UpdateTopApiEntity::getApi).collect(toList());
        if (
            existingTopApis.size() != updatingTopApis.size() ||
            !updatingTopApis.containsAll(existingTopApis) ||
            !existingTopApis.containsAll(updatingTopApis)
        ) {
            throw new IllegalArgumentException("Invalid content to update");
        }
        parameterService.save(
            PORTAL_TOP_APIS,
            topApis.stream().sorted(comparing(UpdateTopApiEntity::getOrder)).map(UpdateTopApiEntity::getApi).collect(toList()),
            ParameterReferenceType.ENVIRONMENT
        );
        return findAll();
    }

    @Override
    public void delete(final String apiId) {
        final List<TopApiEntity> topApis = findAll();
        if (!topApis.isEmpty()) {
            topApis.removeIf(topApiEntity -> apiId.equals(topApiEntity.getApi()));
            parameterService.save(
                PORTAL_TOP_APIS,
                topApis.stream().sorted(comparing(TopApiEntity::getOrder)).map(TopApiEntity::getApi).collect(toList()),
                ParameterReferenceType.ENVIRONMENT
            );
        }
    }
}
