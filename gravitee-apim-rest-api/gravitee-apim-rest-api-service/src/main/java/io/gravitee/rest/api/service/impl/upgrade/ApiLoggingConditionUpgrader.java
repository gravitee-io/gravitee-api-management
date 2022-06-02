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
import static io.gravitee.rest.api.service.impl.upgrade.UpgradeStatus.*;
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
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class ApiLoggingConditionUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiLoggingConditionUpgrader.class);

    @Autowired
    private InstallationService installationService;

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

    @Override
    public boolean upgrade() {
        InstallationEntity installation = installationService.getOrInitialize();
        if (isStatus(installation, SUCCESS)) {
            LOGGER.info("Skipping {} execution because it has already been successfully executed", this.getClass().getSimpleName());
            return false;
        }
        if (isStatus(installation, RUNNING)) {
            LOGGER.warn("Skipping {} execution because it is already running", this.getClass().getSimpleName());
            return false;
        }

        try {
            LOGGER.info("Starting {} execution", this.getClass().getSimpleName());
            setExecutionStatus(installation, RUNNING);
            for (Environment environment : environmentRepository.findAll()) {
                fixApis(environment.getId());
            }

            setExecutionStatus(installation, SUCCESS);
        } catch (Throwable e) {
            LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
            setExecutionStatus(installation, FAILURE);
            return false;
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

        LOGGER.info("Finishing {} execution", this.getClass().getSimpleName());

        return true;
    }

    protected void fixApis(String environmentId) throws Exception {
        for (Api api : apiRepository.search(new ApiCriteria.Builder().environmentId(environmentId).build())) {
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
                    fixLoggingCondition(environmentId, api, apiDefinition, condition);
                }
            }
        }
    }

    protected void fixLoggingCondition(String environmentId, Api api, io.gravitee.definition.model.Api apiDefinition, String condition)
        throws Exception {
        apiDefinition.getProxy().getLogging().setCondition("{" + condition + "}");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        ApiDeploymentEntity apiDeploymentEntity = null;

        if (apiService.isSynchronized(api.getId())) {
            apiDeploymentEntity = new ApiDeploymentEntity();
            apiDeploymentEntity.setDeploymentLabel("Auto-deployed - 3.15.7 upgrade");
        }

        updateApi(environmentId, api, apiDeploymentEntity);
    }

    @Override
    public int getOrder() {
        return 550;
    }

    private void setExecutionStatus(InstallationEntity installation, UpgradeStatus status) {
        installation.getAdditionalInformation().put(InstallationService.API_LOGGING_CONDITION_UPGRADER, status.toString());
        installationService.setAdditionalInformation(installation.getAdditionalInformation());
    }

    private boolean isStatus(InstallationEntity installation, UpgradeStatus status) {
        return status.toString().equals(installation.getAdditionalInformation().get(InstallationService.API_LOGGING_CONDITION_UPGRADER));
    }

    private void updateApi(String environmentId, Api api, ApiDeploymentEntity apiDeploymentEntity) throws Exception {
        api.setUpdatedAt(new Date());
        api.setDeployedAt(api.getUpdatedAt());
        api = apiRepository.update(api);

        if (apiDeploymentEntity != null) {
            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), api.getId());

            // Clear useless field for history
            api.setPicture(null);

            addDeploymentLabelToProperties(api.getId(), properties, apiDeploymentEntity);

            // And create event
            eventService.create(singleton(environmentId), PUBLISH_API, objectMapper.writeValueAsString(api), properties);

            apisFixedAndDeployed.add(api.getId());
        } else {
            apisFixed.add(api.getId());
        }
    }

    private void addDeploymentLabelToProperties(String apiId, Map<String, String> properties, ApiDeploymentEntity apiDeploymentEntity) {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(PUBLISH_API));

        final Optional<EventEntity> optEvent = eventService.search(query).stream().max(comparing(EventEntity::getCreatedAt));

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
