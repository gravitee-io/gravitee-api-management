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
package io.gravitee.apim.core.log.model;

import java.util.Map;
import lombok.Getter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public enum MessageOperation {
    SUBSCRIBE("subscribe"),
    PUBLISH("publish");

    private static final Map<String, MessageOperation> LABELS_MAP = Map.of(SUBSCRIBE.label, SUBSCRIBE, PUBLISH.label, PUBLISH);

    private final String label;

    MessageOperation(String label) {
        this.label = label;
    }

    public static MessageOperation fromLabel(final String label) {
        if (label != null) {
            return LABELS_MAP.get(label);
        }
        return null;
    }
}
