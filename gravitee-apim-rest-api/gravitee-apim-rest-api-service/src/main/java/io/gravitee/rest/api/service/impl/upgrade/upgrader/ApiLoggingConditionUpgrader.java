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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiLoggingConditionUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiService apiService;

    @Autowired
    private EventService eventService;

    private List<String> apisFixedAndDeployed = new ArrayList<>();

    private List<String> apisFixed = new ArrayList<>();

    @Override
    public boolean upgrade() {
        try {
            for (Environment environment : environmentRepository.findAll()) {
                fixApis(new ExecutionContext(environment));
            }

            if (!apisFixedAndDeployed.isEmpty()) {
                log.info(
                    "{} has updated and deployed {} API with the following identifiers: {}",
                    this.getClass().getSimpleName(),
                    apisFixedAndDeployed.size(),
                    apisFixedAndDeployed
                );
            }

            if (!apisFixed.isEmpty()) {
                log.warn(
                    "{} has updated {} API with the following identifiers: {}",
                    this.getClass().getSimpleName(),
                    apisFixed.size(),
                    apisFixed
                );
                log.warn("They need to be redeployed manually to apply the patch.");
            }
        } catch (Exception e) {
            log.error("failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    protected void fixApis(ExecutionContext executionContext) {
        apiRepository
            .search(
                getDefaultApiCriteriaBuilder().environmentId(executionContext.getEnvironmentId()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
            .forEach(api -> {
                try {
                    io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                        api.getDefinition(),
                        io.gravitee.definition.model.Api.class
                    );

                    if (
                        apiDefinition.getProxy() != null &&
                        apiDefinition.getProxy().getLogging() != null &&
                        apiDefinition.getProxy().getLogging().getCondition() != null
                    ) {
                        String condition = apiDefinition.getProxy().getLogging().getCondition().trim();
                        if (condition.contains("#") && !condition.startsWith("{") && !condition.endsWith("}")) {
                            fixLoggingCondition(executionContext, api, apiDefinition, condition);
                        }
                    }
                } catch (Exception e) {
                    log.error("Unable to fix logging condition for API {}", api.getId(), e);
                    throw new RuntimeException(e);
                }
            });
    }

    protected void fixLoggingCondition(
        ExecutionContext executionContext,
        Api api,
        io.gravitee.definition.model.Api apiDefinition,
        String condition
    ) throws Exception {
        apiDefinition.getProxy().getLogging().setCondition("{" + condition + "}");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        ApiDeploymentEntity apiDeploymentEntity = null;

        if (apiService.isSynchronized(executionContext, api.getId())) {
            apiDeploymentEntity = new ApiDeploymentEntity();
            apiDeploymentEntity.setDeploymentLabel("Auto-deployed - 3.15.7 upgrade");
        }

        updateApi(executionContext, api, apiDeploymentEntity);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_LOGGING_CONDITION_UPGRADER;
    }

    private void updateApi(ExecutionContext executionContext, Api api, ApiDeploymentEntity apiDeploymentEntity) throws Exception {
        api.setUpdatedAt(new Date());
        api.setDeployedAt(api.getUpdatedAt());
        api = apiRepository.update(api);

        if (apiDeploymentEntity != null) {
            Map<String, String> properties = new HashMap<>();

            // Clear useless field for history
            api.setPicture(null);

            addDeploymentLabelToProperties(executionContext, api.getId(), properties, apiDeploymentEntity);

            // And create event
            eventService.createApiEvent(executionContext, singleton(executionContext.getEnvironmentId()), PUBLISH_API, api, properties);

            apisFixedAndDeployed.add(api.getId());
        } else {
            apisFixed.add(api.getId());
        }
    }

    private void addDeploymentLabelToProperties(
        ExecutionContext executionContext,
        String apiId,
        Map<String, String> properties,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(PUBLISH_API));
        if (executionContext.hasEnvironmentId()) {
            query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        }
        final Optional<EventEntity> optEvent = eventService
            .search(executionContext, query)
            .stream()
            .max(comparing(EventEntity::getCreatedAt));

        String lastDeployNumber = optEvent.isPresent()
            ? optEvent.get().getProperties().getOrDefault(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0")
            : "0";
        String newDeployNumber = Long.toString(Long.parseLong(lastDeployNumber) + 1);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), newDeployNumber);

        if (apiDeploymentEntity != null && StringUtils.isNotEmpty(apiDeploymentEntity.getDeploymentLabel())) {
            properties.put(Event.EventProperties.DEPLOYMENT_LABEL.getValue(), apiDeploymentEntity.getDeploymentLabel());
        }
    }

    private ApiCriteria.Builder getDefaultApiCriteriaBuilder() {
        // By default in this service, we do not care for V4 APIs.
        List<DefinitionVersion> allowedDefinitionVersion = new ArrayList<>();
        allowedDefinitionVersion.add(null);
        allowedDefinitionVersion.add(DefinitionVersion.V1);
        allowedDefinitionVersion.add(DefinitionVersion.V2);

        return new ApiCriteria.Builder().definitionVersion(allowedDefinitionVersion);
    }
}
