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

    static String PLANS_DATA_UPGRADER_STATUS = "PLANS_DATA_UPGRADER_V2_STATUS";
    static String APPLICATION_API_KEY_MODE_UPGRADER_STATUS = "API_KEY_MODE_UPGRADER_STATUS";
    static String API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS = "API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS";
    static String CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS = "CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS";
    static String ORPHAN_CATEGORY_UPGRADER_STATUS = "ORPHAN_CATEGORY_UPGRADER_STATUS";
    static String API_LOGGING_CONDITION_UPGRADER = "API_LOGGING_CONDITION_UPGRADER";
    static String DEFAULT_ROLES_UPGRADER_STATUS = "DEFAULT_ROLES_UPGRADER_STATUS";
    static String ALERTS_ENVIRONMENT_UPGRADER = "ALERTS_ENVIRONMENT_UPGRADER";
    static String COMMAND_ORGANIZATION_UPGRADER = "COMMAND_ORGANIZATION_UPGRADER";
    static String PLANS_FLOWS_UPGRADER_STATUS = "PLANS_FLOWS_UPGRADER_STATUS";
    static String EVENTS_LATEST_UPGRADER_STATUS = "EVENTS_LATEST_UPGRADER_STATUS";
    static String INTEGRATION_ROLES_UPGRADER_STATUS = "EVENTS_LATEST_UPGRADER_STATUS";

    @Autowired
    private InstallationService installationService;

    @Autowired
    @Lazy
    private UpgraderRepository upgraderRepository;

    protected static final Map<String, String> INSTALLATION_KEY_STATUS = new HashMap<>();

    public InstallationKeyStatusUpgrader() {
        INSTALLATION_KEY_STATUS.put(ORPHAN_CATEGORY_UPGRADER_STATUS, OrphanCategoryUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(DEFAULT_ROLES_UPGRADER_STATUS, DefaultRolesUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(COMMAND_ORGANIZATION_UPGRADER, CommandOrganizationUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(APPLICATION_API_KEY_MODE_UPGRADER_STATUS, ApplicationApiKeyModeUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(ALERTS_ENVIRONMENT_UPGRADER, AlertsEnvironmentUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(PLANS_DATA_UPGRADER_STATUS, PlansDataFixUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS, ApiKeySubscriptionsUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(
            CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS,
            ClientIdInApiKeySubscriptionsUpgrader.class.getName()
        );
        INSTALLATION_KEY_STATUS.put(PLANS_FLOWS_UPGRADER_STATUS, PlansFlowsDefinitionUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(API_LOGGING_CONDITION_UPGRADER, ApiLoggingConditionUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(EVENTS_LATEST_UPGRADER_STATUS, EventsLatestUpgrader.class.getName());
        INSTALLATION_KEY_STATUS.put(INTEGRATION_ROLES_UPGRADER_STATUS, IntegrationRolesUpgrader.class.getName());
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
