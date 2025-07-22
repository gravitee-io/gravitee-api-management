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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiTagService;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiTagServiceImpl implements ApiTagService {

    private final ApiRepository apiRepository;
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper;
    private final ApiNotificationService apiNotificationService;
    private final AuditService auditService;

    public ApiTagServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final EnvironmentService environmentService,
        final ObjectMapper objectMapper,
        final ApiNotificationService apiNotificationService,
        final AuditService auditService
    ) {
        this.apiRepository = apiRepository;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
        this.apiNotificationService = apiNotificationService;
        this.auditService = auditService;
    }

    @Override
    public void deleteTagFromAPIs(ExecutionContext executionContext, final String tagId) {
        environmentService
            .findByOrganization(executionContext.getOrganizationId())
            .stream()
            .map(ExecutionContext::new)
            .flatMap(env ->
                apiRepository.search(
                    new ApiCriteria.Builder().environmentId(env.getEnvironmentId()).build(),
                    null,
                    ApiFieldFilter.allFields()
                )
            )
            .forEach(api -> removeTag(executionContext, api, tagId));
    }

    private void removeTag(ExecutionContext executionContext, Api api, String tagId) throws TechnicalManagementException {
        // Skip if API is federated or has origin Kubernetes
        if (
            api.getDefinitionVersion() == DefinitionVersion.FEDERATED ||
            api.getDefinitionVersion() == DefinitionVersion.FEDERATED_AGENT ||
            Api.ORIGIN_KUBERNETES.equals(api.getOrigin())
        ) {
            log.debug("Skipping tag removal for API: {}", api.getId());
            return;
        }
        log.debug(
            "Starting tag removal process for API: {}, tag: '{}', definition version: {}",
            api.getId(),
            tagId,
            api.getDefinitionVersion()
        );
        try {
            Api previousApi = new Api(api);
            Api updated = null;
            if (api.getDefinitionVersion() != DefinitionVersion.V4) {
                final io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                    api.getDefinition(),
                    io.gravitee.definition.model.Api.class
                );
                if (apiDefinition.getTags().remove(tagId)) {
                    api.setDefinition(objectMapper.writeValueAsString(apiDefinition));
                    updated = apiRepository.update(api);
                }
            } else {
                final io.gravitee.definition.model.v4.AbstractApi apiDefinition = objectMapper.readValue(
                    api.getDefinition(),
                    io.gravitee.definition.model.v4.AbstractApi.class
                );
                if (apiDefinition.getTags().remove(tagId)) {
                    api.setDefinition(objectMapper.writeValueAsString(apiDefinition));
                    updated = apiRepository.update(api);
                    log.debug("API '{}' updated successfully after removing tag '{}'", api.getId(), tagId);
                }
            }
            if (updated != null) {
                apiNotificationService.triggerUpdateNotification(executionContext, api);
                auditService.createApiAuditLog(
                    executionContext,
                    api.getId(),
                    Collections.emptyMap(),
                    API_UPDATED,
                    api.getUpdatedAt(),
                    previousApi,
                    updated
                );
                log.debug("API update notification and audit log triggered for API: {}", api.getId());
            }
        } catch (Exception ex) {
            throw new TechnicalManagementException("An error occurs while removing tag from API: " + api.getId(), ex);
        }
    }
}
