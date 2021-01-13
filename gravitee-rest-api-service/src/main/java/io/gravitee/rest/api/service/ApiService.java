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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiService {

    ApiEntity findById(String apiId);

    Set<ApiEntity> findAll();

    Set<ApiEntity> findAllLight();

    Set<ApiEntity> findByUser(String userId, ApiQuery apiQuery, boolean portal);

    Set<ApiEntity> findPublishedByUser(String userId);
    Set<ApiEntity> findPublishedByUser(String userId, ApiQuery apiQuery);

    Set<ApiEntity> findByVisibility(Visibility visibility);

    ApiEntity create(NewApiEntity api, String userId);
    ApiEntity createFromSwagger(SwaggerApiEntity api, String userId, ImportSwaggerDescriptorEntity swaggerDescriptor);

    ApiEntity update(String apiId, UpdateApiEntity api);
    ApiEntity updateFromSwagger(String apiId, SwaggerApiEntity swaggerApiEntity, ImportSwaggerDescriptorEntity swaggerDescriptor);

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

    ApiEntity deploy(String apiId, String userId, EventType eventType, ApiDeploymentEntity apiDeploymentEntity);

    ApiEntity rollback(String apiId, UpdateApiEntity api);

    String exportAsJson(String apiId, String exportVersion, String... filteredFields);

    ApiEntity createWithImportedDefinition(ApiEntity apiEntity, String apiDefinitionOrURL, String userId);

    ApiEntity updateWithImportedDefinition(ApiEntity apiEntity, String apiDefinitionOrURL, String userId);

    InlinePictureEntity getPicture(String apiId);

    void deleteCategoryFromAPIs(String categoryId);

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
        updateApiEntity.setBackground(apiEntity.getBackground());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setVisibility(apiEntity.getVisibility());
        updateApiEntity.setLabels(apiEntity.getLabels());
        updateApiEntity.setPathMappings(apiEntity.getPathMappings());
        updateApiEntity.setLifecycleState(apiEntity.getLifecycleState());
        updateApiEntity.setPlans(apiEntity.getPlans());
        updateApiEntity.setFlows(apiEntity.getFlows());
        updateApiEntity.setGraviteeDefinitionVersion(apiEntity.getGraviteeDefinitionVersion());
        updateApiEntity.setFlowMode(apiEntity.getFlowMode());

        return updateApiEntity;
    }

    Collection<ApiEntity> search(ApiQuery query);

    Collection<String> searchIds(ApiQuery query);

    Collection<ApiEntity> search(String query, Map<String, Object> filters) throws TechnicalException;

    List<ApiHeaderEntity> getPortalHeaders(String apiId);

    ApiEntity askForReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity acceptReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity rejectReview(String apiId, String userId, ReviewEntity reviewEntity);

    ApiEntity duplicate(String apiId, DuplicateApiEntity duplicateApiEntity);

    InlinePictureEntity getBackground(String apiId);

    ApiEntity migrate(String api);
    String getConfigurationSchema();
}
