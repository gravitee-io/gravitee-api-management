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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.rest.api.service.V4EmulationEngineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class V4EmulationEngineServiceImpl implements V4EmulationEngineService {

    private final DefaultMode defaultMode;

    public V4EmulationEngineServiceImpl(@Value("${api.v2.emulateV4Engine.default:yes}") String defaultMode) {
        this.defaultMode = DefaultMode.fromLabel(defaultMode);
    }

    @Override
    public DefaultMode defaultMode() {
        return defaultMode;
    }

    @Override
    public ExecutionMode getExecutionModeFor(final JsonNode apiDefinition) {
        // When apiDefinition is null, that means it is not an import
        if (((defaultMode == DefaultMode.YES) || (apiDefinition == null && defaultMode == DefaultMode.CREATION_ONLY))) {
            return ExecutionMode.V4_EMULATION_ENGINE;
        }
        return ExecutionMode.V3;
    }
}
