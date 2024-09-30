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

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpgraderOrder {

    private UpgraderOrder() {}

    public static final int INSTALLATION_KEY_STATUS_UPGRADER = 0;
    public static final int DEFAULT_ENVIRONMENT_UPGRADER = 100;
    public static final int DEFAULT_ORGANIZATION_UPGRADER = 100;
    public static final int ORPHAN_CATEGORY_UPGRADER = 100;
    public static final int DEFAULT_ROLES_UPGRADER = 120;
    public static final int API_PRIMARY_OWNER_REMOVAL_UPGRADER = 140;
    public static final int ALERTS_ENVIRONMENT_UPGRADER = 500;
    public static final int APPLICATION_API_KEY_MODE_UPGRADER = 500;
    public static final int COMMAND_ORGANIZATION_UPGRADER = 500;
    public static final int PLANS_DATA_FIX_UPGRADER = 500;
    public static final int API_KEY_SUBSCRIPTIONS_UPGRADER = 501;
    public static final int CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER = 502;
    public static final int API_LOGGING_CONDITION_UPGRADER = 550;
    public static final int PLANS_FLOWS_DEFINITION_UPGRADER = 550;
    public static final int EVENTS_LATEST_UPGRADER = 600;
    public static final int EXECUTION_MODE_UPGRADER = 610;
    public static final int API_V4_CATEGORIES_UPGRADER = 701;
    public static final int USER_TOKEN_PERMISSION_UPGRADER = 702;
}
