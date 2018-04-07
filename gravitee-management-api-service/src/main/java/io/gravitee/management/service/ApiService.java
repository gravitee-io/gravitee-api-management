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
import io.gravitee.repository.exceptions.TechnicalException;

import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiService {

    ApiEntity findById(String apiId);

    Set<ApiEntity> findAll();

    Set<ApiEntity> findByUser(String userId);

    Set<ApiEntity> findByGroup(String groupId);

    Set<ApiEntity> findByVisibility(Visibility visibility);

    ApiEntity create(NewApiEntity api, String userId);

    ApiEntity update(String apiId, UpdateApiEntity api);

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

    String exportAsJson(String apiId, String role, String... filteredFields);

    ApiEntity createOrUpdateWithDefinition(ApiEntity apiEntity, String apiDefinition, String userId);

    InlinePictureEntity getPicture(String apiId);

    void deleteViewFromAPIs(String viewId);

    void deleteTagFromAPIs(String tagId);

    void checkContextPath(String newContextPath) throws TechnicalException;

    ApiModelEntity findByIdForTemplates(String apiId);

    boolean exists(String apiId);

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

        return updateApiEntity;
    }

}
