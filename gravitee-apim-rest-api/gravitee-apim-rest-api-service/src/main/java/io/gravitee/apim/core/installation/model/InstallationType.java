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
package io.gravitee.apim.core.installation.model;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InstallationType {
    STANDALONE(Labels.STANDALONE),
    MULTI_TENANT(Labels.MULTI_TENANT);

    private static final Map<String, InstallationType> LABELS_MAP = Map.of(STANDALONE.label, STANDALONE, MULTI_TENANT.label, MULTI_TENANT);

    @JsonValue
    private final String label;

    public static InstallationType fromLabel(final String label) {
        if (label != null) {
            return LABELS_MAP.get(label);
        }
        return null;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Labels {

        public static final String STANDALONE = "standalone";
        public static final String MULTI_TENANT = "multi-tenant";
    }
}
