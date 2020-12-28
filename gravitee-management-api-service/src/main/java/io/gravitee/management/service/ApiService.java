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
package io.gravitee.management.service;

import io.gravitee.management.model.*;
import io.gravitee.management.model.api.*;
import io.gravitee.management.model.api.header.ApiHeaderEntity;
import io.gravitee.repository.exceptions.TechnicalException;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiService {

    ApiEntity findById(String apiId);

    Set<ApiEntity> findAll();

    Set<ApiEntity> findAllLight();

    Set<ApiEntity> findByUser(String userId, ApiQuery apiQuery);

    Set<ApiEntity> findByVisibility(Visibility visibility);

    ApiEntity create(NewApiEntity api, String userId);
    ApiEntity create(SwaggerApiEntity api, String userId, ImportSwaggerDescriptorEntity swaggerDescriptor);

    ApiEntity update(String apiId, UpdateApiEntity api);
    ApiEntity update(String apiId, SwaggerApiEntity swaggerApiEntity, ImportSwaggerDescriptorEntity swaggerDescriptor);

    void delete(String apiId);

    ApiEntity start(String apiId, String userId);

    ApiEntity stop(String apiId, String userId);

    /**
     * Check if the API is "out of sync" or not. In this case, user is able to deploy it.
     * API is in "out of sync" state if:
     * - API definition has been updated and is different from the currently deployed API
     * - A plan has been updated for the API
     *
     * @param apiId
     * @return
     */
    boolean isSynchronized(String apiId);

    ApiEntity deploy(String apiId, String userId, EventType eventType);

    ApiEntity rollback(String apiId, UpdateApiEntity api);

    String exportAsJson(String apiId, String exportVersion, String... filteredFields);

    ApiEntity createOrUpdateWithDefinition(ApiEntity apiEntity, String apiDefinitionOrURL, String userId);

    InlinePictureEntity getPicture(String apiId);

    byte[] getDefaultPicture();

    void deleteViewFromAPIs(String viewId);

    void deleteTagFromAPIs(String tagId);

    ApiModelEntity findByIdForTemplates(String apiId, boolean decodeTemplate);

    default ApiModelEntity findByIdForTemplates(String apiId) {
        return findByIdForTemplates(apiId, false);
    }

    boolean exists(String apiId);

    ApiEntity importPathMappingsFromPage(ApiEntity apiEntity, String page);

    static UpdateApiEntity convert(ApiEntity apiEntity) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();

        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setProperties(apiEntity.getProperties());
        updateApiEntity.setDescription(apiEntity.getDescription());
        updateApiEntity.setGroups(apiEntity.getGroups());
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setVisibility(apiEntity.getVisibility());
        updateApiEntity.setLabels(apiEntity.getLabels());
        updateApiEntity.setPathMappings(apiEntity.getPathMappings());
        updateApiEntity.setLifecycleState(apiEntity.getLifecycleState());

        return updateApiEntity;
    }

    Collection<ApiEntity> search(ApiQuery query);

    Collection<ApiEntity> search(String query, Map<String, Object> filters) throws TechnicalException;

    List<ApiHeaderEntity> getPortalHeaders(String apiId);

    ApiEntity askForReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity acceptReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity rejectReview(String apiId, String userId, ReviewEntity reviewEntity);

    ApiEntity duplicate(ApiEntity apiEntity, DuplicateApiEntity duplicateApiEntity);
}
