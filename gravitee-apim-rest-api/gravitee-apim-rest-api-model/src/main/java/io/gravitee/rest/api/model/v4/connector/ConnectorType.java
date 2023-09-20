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
package io.gravitee.rest.api.model.v4.connector;

import java.util.Map;
import lombok.Getter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public enum ConnectorType {
    ENDPOINT("endpoint"),
    ENTRYPOINT("entrypoint");

    private static final Map<String, ConnectorType> LABELS_MAP = Map.of(ENDPOINT.label, ENDPOINT, ENTRYPOINT.label, ENTRYPOINT);

    private final String label;

    private ConnectorType(String label) {
        this.label = label;
    }

    public static ConnectorType fromLabel(final String label) {
        if (label != null) {
            return LABELS_MAP.get(label);
        }
        return null;
    }
}
