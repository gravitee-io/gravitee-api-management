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

import static io.gravitee.repository.management.model.MetadataReferenceType.API;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiMetadataServiceImpl extends AbstractReferenceMetadataService implements ApiMetadataService {

    @Autowired
    private ApiService apiService;

    @Autowired
    private SearchEngineService searchEngineService;

    @Override
    public List<ApiMetadataEntity> findAllByApi(final String apiId) {
        final List<ReferenceMetadataEntity> allMetadata = findAllByReference(API, apiId, true);
        return allMetadata.stream().map(m -> convert(m, apiId)).collect(toList());
    }

    @Override
    public ApiMetadataEntity findByIdAndApi(final String metadataId, final String apiId) {
        return convert(findByIdAndReference(metadataId, API, apiId, true), apiId);
    }

    @Override
    public void delete(final String metadataId, final String apiId) {
        delete(metadataId, API, apiId);
    }

    @Override
    public void deleteAllByApi(String apiId) {
        final List<ReferenceMetadataEntity> allMetadata = findAllByReference(API, apiId, false);
        allMetadata.stream().forEach(referenceMetadataEntity -> delete(referenceMetadataEntity.getKey(), API, apiId));
    }

    @Override
    public List<ApiMetadataEntity> create(List<ApiMetadataEntity> apiMetadata, String apiId) {
        if (apiMetadata == null || apiMetadata.isEmpty()) {
            return emptyList();
        }

        return apiMetadata
            .stream()
            .map(
                data -> {
                    NewApiMetadataEntity newMD = new NewApiMetadataEntity();
                    newMD.setFormat(data.getFormat());
                    newMD.setName(data.getName());
                    newMD.setValue(data.getValue());
                    newMD.setApiId(apiId);
                    return newMD;
                }
            )
            .map(this::create)
            .collect(toList());
    }

    @Override
    public ApiMetadataEntity create(final NewApiMetadataEntity metadataEntity) {
        return convert(
            create(metadataEntity, API, metadataEntity.getApiId(), true, GraviteeContext.getCurrentEnvironment()),
            metadataEntity.getApiId()
        );
    }

    @Override
    public ApiMetadataEntity update(final UpdateApiMetadataEntity metadataEntity) {
        ApiMetadataEntity apiMetadataEntity = convert(
            update(metadataEntity, API, metadataEntity.getApiId(), true, GraviteeContext.getCurrentEnvironment()),
            metadataEntity.getApiId()
        );
        ApiEntity apiEntity = apiService.fetchMetadataForApi(apiService.findById(apiMetadataEntity.getApiId()));
        searchEngineService.index(apiEntity, false);
        return apiMetadataEntity;
    }

    @Override
    protected void checkReferenceMetadataFormat(
        MetadataFormat format,
        String value,
        MetadataReferenceType referenceType,
        String referenceId,
        final String environmentId
    ) {
        final ApiEntity apiEntity = apiService.findById(referenceId);
        metadataService.checkMetadataFormat(format, value, referenceType, apiEntity);
    }

    private ApiMetadataEntity convert(ReferenceMetadataEntity m, String apiId) {
        final ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setKey(m.getKey());
        apiMetadataEntity.setName(m.getName());
        apiMetadataEntity.setFormat(m.getFormat());
        apiMetadataEntity.setValue(m.getValue());
        apiMetadataEntity.setDefaultValue(m.getDefaultValue());
        apiMetadataEntity.setApiId(apiId);
        return apiMetadataEntity;
    }
}
