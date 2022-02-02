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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiService {
    ApiEntity findById(String apiId);

    Optional<ApiEntity> findByEnvironmentIdAndCrossId(String environment, String crossId);

    Set<ApiEntity> findAllByEnvironment(String environment);

    Set<ApiEntity> findByEnvironmentAndIdIn(String environment, Set<String> ids);

    default Set<ApiEntity> findAllLightByEnvironment(String environmentId) {
        return findAllLightByEnvironment(environmentId, true);
    }

    Set<ApiEntity> findAllLightByEnvironment(String environmentId, boolean excludeDefinition);

    Set<ApiEntity> findAllLight();

    Page<ApiEntity> findByUser(String userId, ApiQuery apiQuery, Sortable sortable, Pageable pageable, boolean portal);

    Set<ApiEntity> findByUser(String userId, ApiQuery apiQuery, boolean portal);

    Page<ApiEntity> findPublishedByUser(String userId, ApiQuery apiQuery, Sortable sortable, Pageable pageable);

    Set<ApiEntity> findPublishedByUser(String userId);

    default Set<String> findIdsByUser(String userId, ApiQuery apiQuery, boolean portal) {
        return findIdsByUser(userId, apiQuery, null, portal);
    }

    Set<String> findIdsByUser(String userId, ApiQuery apiQuery, Sortable sortable, boolean portal);

    Set<ApiEntity> findPublishedByUser(String userId, ApiQuery apiQuery);

    Set<String> findPublishedIdsByUser(String userId, ApiQuery apiQuery);

    default Set<String> findPublishedIdsByUser(String userId) {
        return findPublishedIdsByUser(userId, null);
    }

    Set<ApiEntity> findByVisibility(Visibility visibility);

    ApiEntity create(NewApiEntity api, String userId);
    ApiEntity createFromSwagger(SwaggerApiEntity api, String userId, ImportSwaggerDescriptorEntity swaggerDescriptor);
    ApiEntity createWithApiDefinition(UpdateApiEntity api, String userId, JsonNode apiDefinition);

    ApiEntity update(String apiId, UpdateApiEntity api);
    ApiEntity update(String apiId, UpdateApiEntity api, boolean checkPlans);

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

    ApiEntity rollback(String apiId, RollbackApiEntity api);

    String exportAsJson(String apiId, String exportVersion, String... filteredFields);

    InlinePictureEntity getPicture(String apiId);

    void deleteCategoryFromAPIs(String categoryId);

    void deleteTagFromAPIs(String tagId);

    ApiModelEntity findByIdForTemplates(String apiId, boolean decodeTemplate);

    default ApiModelEntity findByIdForTemplates(String apiId) {
        return findByIdForTemplates(apiId, false);
    }

    boolean exists(String apiId);

    ApiEntity importPathMappingsFromPage(ApiEntity apiEntity, String page, DefinitionVersion definitionVersion);

    Set<CategoryEntity> listCategories(Collection<String> apis, String environment);

    Page<ApiEntity> search(ApiQuery query, Sortable sortable, Pageable pageable);

    Collection<ApiEntity> search(ApiQuery query);

    Collection<String> searchIds(ApiQuery query);

    default Collection<String> searchIds(String query, Map<String, Object> filters) throws TechnicalException {
        return searchIds(query, filters, null);
    }

    Collection<String> searchIds(String query, Map<String, Object> filters, Sortable sortable) throws TechnicalException;

    Page<ApiEntity> search(String query, Map<String, Object> filters, Sortable sortable, Pageable pageable);

    Collection<ApiEntity> search(String query, Map<String, Object> filters) throws TechnicalException;

    List<ApiHeaderEntity> getPortalHeaders(String apiId);

    ApiEntity askForReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity acceptReview(String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity rejectReview(String apiId, String userId, ReviewEntity reviewEntity);

    InlinePictureEntity getBackground(String apiId);

    ApiEntity migrate(String api);

    boolean hasHealthCheckEnabled(ApiEntity api, boolean mustBeEnabledOnAllEndpoints);

    ApiEntity fetchMetadataForApi(ApiEntity apiEntity);

    PrimaryOwnerEntity getPrimaryOwner(String apiId) throws TechnicalManagementException;

    void addGroup(String api, String group);
    void removeGroup(String api, String group);

    void checkPolicyConfigurations(Map<String, List<Rule>> paths, List<Flow> flows, List<Plan> plans);

    Map<String, Long> countPublishedByUserGroupedByCategories(String userId);

    void calculateEntrypoints(String environment, ApiEntity api);
}
