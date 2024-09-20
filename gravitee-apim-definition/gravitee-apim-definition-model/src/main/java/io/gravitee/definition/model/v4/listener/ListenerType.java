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
package io.gravitee.definition.model.v4.listener;

import static io.gravitee.definition.model.v4.listener.Listener.HTTP_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.SUBSCRIPTION_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.TCP_LABEL;
import static io.gravitee.definition.model.v4.nativeapi.NativeListener.KAFKA_LABEL;

import com.fasterxml.jackson.annotation.JsonValue;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Getter
@Schema(name = "ListenerTypeV4")
public enum ListenerType {
    HTTP(HTTP_LABEL),
    SUBSCRIPTION(SUBSCRIPTION_LABEL),
    TCP(TCP_LABEL),
    KAFKA(KAFKA_LABEL);

    private static final Map<String, ListenerType> LABELS_MAP = Map.of(
        HTTP.label,
        HTTP,
        SUBSCRIPTION.label,
        SUBSCRIPTION,
        TCP.label,
        TCP,
        KAFKA.label,
        KAFKA
    );

    @JsonValue
    private final String label;

    public static ListenerType fromLabel(final String label) {
        if (label != null) {
            return LABELS_MAP.get(label);
        }
        return null;
    }
}
