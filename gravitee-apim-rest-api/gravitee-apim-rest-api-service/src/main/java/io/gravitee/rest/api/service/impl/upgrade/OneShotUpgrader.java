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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
public abstract class OneShotUpgrader implements Upgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneShotUpgrader.class);

    @Autowired
    private InstallationService installationService;

    private final String installationStatusKey;

    protected abstract void processOneShotUpgrade() throws Exception;

    public OneShotUpgrader(String installationStatusKey) {
        super();
        this.installationStatusKey = installationStatusKey;
    }

    @Override
    public final Completable upgrade() {
        InstallationEntity installation = installationService.getOrInitialize();

        if (isStatus(installation, SUCCESS)) {
            // This upgrader has already been executed successfully in the past.
            // Register the state so that after next restart, this one will not be considered anymore
            LOGGER.debug("Skipping {} execution cause it has already been successfully executed", this.getClass().getSimpleName());

            return Completable.complete();
        }

        // We are assuming that the Management API has been upgraded and so restarted. So there should be no more
        // upgraders in running state at this stage.
        return Completable.create(
            new CompletableOnSubscribe() {
                @Override
                public void subscribe(@NotNull CompletableEmitter emitter) throws Exception {
                    try {
                        LOGGER.info("Starting {} execution", this.getClass().getSimpleName());
                        processOneShotUpgrade();
                        emitter.onComplete();
                    } catch (Throwable e) {
                        LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
                        emitter.onError(e);
                    }
                }
            }
        );
    }

    private boolean isStatus(InstallationEntity installation, UpgradeStatus status) {
        return status.toString().equals(installation.getAdditionalInformation().get(installationStatusKey));
    }
}
