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
package io.gravitee.plugin.gamma.internal;

import com.fasterxml.jackson.annotation.JsonValue;
import io.gravitee.plugin.core.api.PluginManifest;
import java.util.Map;
import lombok.CustomLog;

/**
 * Declared with the {@code scope} property of the module manifest. The console serves account modules outside the
 * environments.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public enum GammaModuleScope {
    ACCOUNT("account"),
    ENVIRONMENT("environment");

    public static final String MANIFEST_PROPERTY = "scope";

    @JsonValue
    private final String value;

    GammaModuleScope(String value) {
        this.value = value;
    }

    public static GammaModuleScope of(PluginManifest manifest) {
        Map<String, String> properties = manifest.properties();
        String declared = properties == null ? null : properties.get(MANIFEST_PROPERTY);
        if (declared == null) {
            return ENVIRONMENT;
        }
        for (GammaModuleScope scope : values()) {
            if (scope.value.equalsIgnoreCase(declared.trim())) {
                return scope;
            }
        }
        log.warn("Gamma module '{}' declares an unknown scope '{}', treating it as an environment module", manifest.id(), declared);
        return ENVIRONMENT;
    }
}
