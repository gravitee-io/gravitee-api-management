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
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApiLoggingConditionUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiLoggingConditionUpgrader.class);

    @Autowired
    private ApiRepository apiRepository;

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

    public ApiLoggingConditionUpgrader() {
        super(InstallationService.API_LOGGING_CONDITION_UPGRADER);
    }

    @Override
    protected void processOneShotUpgrade() throws Exception {
        for (Environment environment : environmentRepository.findAll()) {
            fixApis(new ExecutionContext(environment));
        }

        if (!apisFixedAndDeployed.isEmpty()) {
            LOGGER.info(
                "{} has updated and deployed {} API with the following identifiers: {}",
                this.getClass().getSimpleName(),
                apisFixedAndDeployed.size(),
                apisFixedAndDeployed
            );
        }

        if (!apisFixed.isEmpty()) {
            LOGGER.warn(
                "{} has updated {} API with the following identifiers: {}",
                this.getClass().getSimpleName(),
                apisFixed.size(),
                apisFixed
            );
            LOGGER.warn("They need to be redeployed manually to apply the patch.");
        }
    }

    protected void fixApis(ExecutionContext executionContext) throws Exception {
        for (Api api : apiRepository.search(new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build())) {
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
        }
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
    public int order() {
        return 550;
    }

    private void updateApi(ExecutionContext executionContext, Api api, ApiDeploymentEntity apiDeploymentEntity) throws Exception {
        api.setUpdatedAt(new Date());
        api.setDeployedAt(api.getUpdatedAt());
        api = apiRepository.update(api);

        if (apiDeploymentEntity != null) {
            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), api.getId());

            // Clear useless field for history
            api.setPicture(null);

            addDeploymentLabelToProperties(executionContext, api.getId(), properties, apiDeploymentEntity);

            // And create event
            eventService.create(
                executionContext,
                singleton(executionContext.getEnvironmentId()),
                PUBLISH_API,
                objectMapper.writeValueAsString(api),
                properties
            );

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
}
