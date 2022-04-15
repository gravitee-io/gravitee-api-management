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

import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

/**
 * An upgrader that will run at APIM startup, only once.
 * It stores his status in 'installations' table, and won't execute once he has already succeeded.
 *
 * Implementations can disable the upgrader by overriding 'isEnabled'.
 * Or run in dry mode, by overriding 'isDryRun'.
 *
 * Implementations have to provide the 'installationStatusKey' in builder,
 * Which is the status property key of this upgrader in the 'installations' collection.
 *
 * @author GraviteeSource Team
 */
public abstract class OneShotUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneShotUpgrader.class);

    @Autowired
    private InstallationService installationService;

    private String installationStatusKey;

    protected abstract void processOneShotUpgrade() throws Exception;

    public OneShotUpgrader(String installationStatusKey) {
        super();
        this.installationStatusKey = installationStatusKey;
    }

    @Override
    public final boolean upgrade(ExecutionContext executionContext) {
        if (!isEnabled()) {
            LOGGER.info("Skipping {} execution cause it's not enabled in configuration", this.getClass().getSimpleName());
            return false;
        }

        InstallationEntity installation = installationService.getOrInitialize();
        if (isDryRun() && isStatus(installation, DRY_SUCCESS)) {
            LOGGER.info(
                "Skipping {} execution cause it has already been successfully executed in dry mode",
                this.getClass().getSimpleName()
            );
            return false;
        }
        if (isStatus(installation, SUCCESS)) {
            LOGGER.info("Skipping {} execution cause it has already been successfully executed", this.getClass().getSimpleName());
            return false;
        }
        if (isStatus(installation, RUNNING)) {
            LOGGER.warn("Skipping {} execution cause it's already running", this.getClass().getSimpleName());
            return false;
        }

        try {
            LOGGER.info("Starting {} execution with dry-run {}", this.getClass().getSimpleName(), isDryRun() ? "enabled" : "disabled");
            setExecutionStatus(installation, RUNNING);
            processOneShotUpgrade();
            setExecutionStatus(installation, isDryRun() ? DRY_SUCCESS : SUCCESS);
        } catch (Throwable e) {
            LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
            setExecutionStatus(installation, FAILURE);
            return false;
        }
        LOGGER.info("Finishing {} execution", this.getClass().getSimpleName());
        return true;
    }

    private void setExecutionStatus(InstallationEntity installation, UpgradeStatus status) {
        installation.getAdditionalInformation().put(installationStatusKey, status.toString());
        installationService.setAdditionalInformation(installation.getAdditionalInformation());
    }

    private boolean isStatus(InstallationEntity installation, UpgradeStatus status) {
        return status.toString().equals(installation.getAdditionalInformation().get(installationStatusKey));
    }

    protected boolean isDryRun() {
        return false;
    }

    protected boolean isEnabled() {
        return true;
    }
}
