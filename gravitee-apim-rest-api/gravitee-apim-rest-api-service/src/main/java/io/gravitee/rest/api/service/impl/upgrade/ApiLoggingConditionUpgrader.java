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

import static io.gravitee.rest.api.service.impl.upgrade.UpgradeStatus.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
    private ObjectMapper objectMapper;

    @Override
    public boolean upgrade(ExecutionContext executionContext) {
        InstallationEntity installation = installationService.getOrInitialize();
        if (isStatus(installation, SUCCESS)) {
            LOGGER.info("Skipping {} execution cause it has already been successfully executed", this.getClass().getSimpleName());
            return false;
        }
        if (isStatus(installation, RUNNING)) {
            LOGGER.warn("Skipping {} execution cause it's already running", this.getClass().getSimpleName());
            return false;
        }

        try {
            LOGGER.info("Starting {} execution with dry-run", this.getClass().getSimpleName());
            setExecutionStatus(installation, RUNNING);
            fixApis();
            setExecutionStatus(installation, SUCCESS);
        } catch (Throwable e) {
            LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
            setExecutionStatus(installation, FAILURE);
            return false;
        }

        return true;
    }

    protected void fixApis() throws Exception {
        for (Api api : apiRepository.findAll()) {
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
                    fixLoggingCondition(api, apiDefinition, condition);
                }
            }
        }
    }

    protected void fixLoggingCondition(Api api, io.gravitee.definition.model.Api apiDefinition, String condition) throws Exception {
        apiDefinition.getProxy().getLogging().setCondition("{" + condition + "}");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));
        apiRepository.update(api);
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
}
