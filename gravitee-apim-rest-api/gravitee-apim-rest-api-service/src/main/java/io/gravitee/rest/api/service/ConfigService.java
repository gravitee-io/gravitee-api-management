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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.PortalConfigEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ConfigService {
    boolean portalLoginForced(ExecutionContext executionContext);

    PortalSettingsEntity getPortalSettings(ExecutionContext executionContext);

    void save(ExecutionContext executionContext, PortalSettingsEntity portalSettingsEntity);

    ConsoleSettingsEntity getConsoleSettings(ExecutionContext executionContext);

    void save(ExecutionContext executionContext, ConsoleSettingsEntity consoleSettingsEntity);

    ConsoleConfigEntity getConsoleConfig(ExecutionContext executionContext);

    PortalConfigEntity getPortalConfig(ExecutionContext executionContext);
}
