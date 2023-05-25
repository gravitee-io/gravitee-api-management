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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.gravitee.rest.api.service.InstallationService;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class InstallationKeyStatusUpgrader implements Upgrader {

    @Autowired
    private InstallationService installationService;

    @Autowired
    @Lazy
    private UpgraderRepository upgraderRepository;

    protected static final Map<String, String> INSTALLATION_KEY_STATUS = new HashMap<>();

    public InstallationKeyStatusUpgrader() {
        INSTALLATION_KEY_STATUS.put(InstallationService.ORPHAN_CATEGORY_UPGRADER_STATUS, OrphanCategoryUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.DEFAULT_ROLES_UPGRADER_STATUS, DefaultRolesUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.COMMAND_ORGANIZATION_UPGRADER, CommandOrganizationUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(
            InstallationService.APPLICATION_API_KEY_MODE_UPGRADER_STATUS,
            ApplicationApiKeyModeUpgrader.class.getName()
        );
        INSTALLATION_KEY_STATUS.put(InstallationService.ALERTS_ENVIRONMENT_UPGRADER, AlertsEnvironmentUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.PLANS_DATA_UPGRADER_STATUS, PlansDataFixUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS, ApiKeySubscriptionsUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(
            InstallationService.CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS,
            ClientIdInApiKeySubscriptionsUpgrader.class.getName()
        );
        INSTALLATION_KEY_STATUS.put(InstallationService.PLANS_FLOWS_UPGRADER_STATUS, PlansFlowsDefinitionUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.API_LOGGING_CONDITION_UPGRADER, ApiLoggingConditionUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(InstallationService.EVENTS_LATEST_UPGRADER_STATUS, EventsLatestUpgrader.class.getName());
    }

    @Override
    public boolean upgrade() {
        try {
            installationService
                .get()
                .getAdditionalInformation()
                .forEach((k, v) -> {
                    if ("SUCCESS".equals(v)) {
                        String id = INSTALLATION_KEY_STATUS.get(k);
                        if (id == null) {
                            log.error("Can't find upgrader class with installation status key {}", k);
                            return;
                        }
                        UpgradeRecord upgradeRecord = upgraderRepository.findById(id).blockingGet();
                        if (upgradeRecord == null) {
                            upgraderRepository.create(new UpgradeRecord(id, new Date())).blockingGet();
                        }
                    }
                });

            return true;
        } catch (Exception e) {
            log.error("unable to apply upgrader {}", getClass().getSimpleName(), e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.INSTALLATION_KEY_STATUS_UPGRADER;
    }
}
