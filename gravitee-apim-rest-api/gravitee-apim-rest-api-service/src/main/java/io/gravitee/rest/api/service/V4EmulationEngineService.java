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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.ExecutionMode;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface V4EmulationEngineService {
    /**
     * Return when the V4 Emulation Engine should use by default.
     * <ul>
     * <li>yes: V4 emulation engine will be used for creation and import</li>
     * <li>creation_only: V4 emulation engine will be used only while creating a new api, not for import</li>
     * <li>no: V4 emulation engine won't be used, v3 mode is the default mode</li>
     * </ul>
     *
     * @return default mode set
     */
    DefaultMode defaultMode();

    /**
     * Return the appropriate {@link ExecutionMode} to use regarding the given api defintion and configuration
     *
     * @return the {@link ExecutionMode} to use
     * @param apiDefinition
     */
    ExecutionMode getExecutionModeFor(final JsonNode apiDefinition);

    /**
     * <ul>
     * <li>yes: V4 emulation engine will be used for creation and import</li>
     * <li>creation_only: V4 emulation engine will be used only while creating a new api, not for import</li>
     * <li>no: V4 emulation engine won't be used, v3 mode is the default mode</li>
     * </ul>
     */
    enum DefaultMode {
        YES("yes"),
        CREATION_ONLY("creation_only"),
        NO("no");

        private static final Map<String, DefaultMode> BY_LABEL = Map.of(YES.label, YES, CREATION_ONLY.label, CREATION_ONLY, NO.label, NO);
        private final String label;

        DefaultMode(final String label) {
            this.label = label;
        }

        public static DefaultMode fromLabel(final String label) {
            return BY_LABEL.getOrDefault(label, YES);
        }

        public String getLabel() {
            return label;
        }
    }
}
