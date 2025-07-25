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
package io.gravitee.rest.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiService {
    ApiEntity findById(ExecutionContext executionContext, String apiId);

    Optional<ApiEntity> findByEnvironmentIdAndCrossId(String environment, String crossId);

    Page<ApiEntity> findByUser(
        ExecutionContext executionContext,
        String userId,
        ApiQuery apiQuery,
        Sortable sortable,
        Pageable pageable,
        boolean manageOnly
    );

    Set<ApiEntity> findByUser(ExecutionContext executionContext, String userId, ApiQuery apiQuery, boolean manageOnly);

    ApiEntity create(ExecutionContext executionContext, NewApiEntity api, String userId);
    ApiEntity createFromSwagger(
        ExecutionContext executionContext,
        SwaggerApiEntity api,
        String userId,
        ImportSwaggerDescriptorEntity swaggerDescriptor
    );
    ApiEntity createWithApiDefinition(ExecutionContext executionContext, UpdateApiEntity api, String userId, JsonNode apiDefinition);

    ApiEntity update(ExecutionContext executionContext, String apiId, UpdateApiEntity api);
    ApiEntity update(ExecutionContext executionContext, String apiId, UpdateApiEntity api, boolean checkPlans);
    ApiEntity update(ExecutionContext executionContext, String apiId, UpdateApiEntity api, boolean checkPlans, boolean updatePlansAndFlows);

    ApiEntity updateFromSwagger(
        ExecutionContext executionContext,
        String apiId,
        SwaggerApiEntity swaggerApiEntity,
        ImportSwaggerDescriptorEntity swaggerDescriptor
    );

    void delete(ExecutionContext executionContext, String apiId, boolean closePlans);

    ApiEntity start(ExecutionContext executionContext, String apiId, String userId);

    ApiEntity stop(ExecutionContext executionContext, String apiId, String userId);

    /**
     * Check if the API is "out of sync" or not. In this case, user is able to deploy it.
     * API is in "out of sync" state if:
     * - API definition has been updated and is different from the currently deployed API
     * - A plan has been updated for the API
     *
     * @param executionContext
     * @param apiId
     * @return
     */
    boolean isSynchronized(ExecutionContext executionContext, String apiId);

    ApiEntity deploy(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        EventType eventType,
        ApiDeploymentEntity apiDeploymentEntity
    );

    ApiEntity rollback(ExecutionContext executionContext, String apiId, RollbackApiEntity api);

    String exportAsJson(ExecutionContext executionContext, String apiId, String exportVersion, String... filteredFields);

    InlinePictureEntity getPicture(ExecutionContext executionContext, String apiId);

    ApiEntity importPathMappingsFromPage(
        ExecutionContext executionContext,
        ApiEntity apiEntity,
        String page,
        DefinitionVersion definitionVersion
    );

    Page<ApiEntity> search(ExecutionContext executionContext, ApiQuery query, Sortable sortable, Pageable pageable);

    Collection<ApiEntity> search(ExecutionContext executionContext, ApiQuery query);

    Page<String> searchIds(ExecutionContext executionContext, ApiQuery query, Pageable pageable, Sortable sortable);

    Collection<String> searchIds(ExecutionContext executionContext, String query, Map<String, Object> filters, Sortable sortable)
        throws TechnicalException;

    Page<ApiEntity> search(
        ExecutionContext executionContext,
        String query,
        Map<String, Object> filters,
        Sortable sortable,
        Pageable pageable
    );

    Collection<ApiEntity> search(ExecutionContext executionContext, String query, Map<String, Object> filters) throws TechnicalException;

    List<ApiHeaderEntity> getPortalHeaders(ExecutionContext executionContext, String apiId);

    ApiEntity askForReview(ExecutionContext executionContext, String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity acceptReview(ExecutionContext executionContext, String apiId, String userId, ReviewEntity reviewEntity);
    ApiEntity rejectReview(ExecutionContext executionContext, String apiId, String userId, ReviewEntity reviewEntity);

    InlinePictureEntity getBackground(ExecutionContext executionContext, String apiId);

    ApiEntity migrate(ExecutionContext executionContext, String api);

    boolean hasHealthCheckEnabled(ApiEntity api, boolean mustBeEnabledOnAllEndpoints);

    void checkPolicyConfigurations(Map<String, List<Rule>> paths, List<Flow> flows, Set<PlanEntity> plans);

    long countByCategoryForUser(ExecutionContext executionContext, String categoryId, String userId);

    Map<String, Object> findByIdAsMap(String api) throws TechnicalException;
}
