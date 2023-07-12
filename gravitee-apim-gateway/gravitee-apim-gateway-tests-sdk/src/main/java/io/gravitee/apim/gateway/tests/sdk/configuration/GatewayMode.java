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
package io.gravitee.apim.gateway.tests.sdk.configuration;

import io.gravitee.definition.model.ExecutionMode;
import lombok.Getter;

@Getter
public enum GatewayMode {
    JUPITER(true, ExecutionMode.JUPITER),
    V3(false, ExecutionMode.V3),
    COMPATIBILITY(true, ExecutionMode.V3);

    private final Boolean jupiterEnabled;
    private final ExecutionMode executionMode;

    GatewayMode(Boolean jupiterEnabled, ExecutionMode executionMode) {
        this.jupiterEnabled = jupiterEnabled;
        this.executionMode = executionMode;
    }
}
