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
package io.gravitee.definition.model.v4.listener.entrypoint;

import com.fasterxml.jackson.annotation.JsonValue;
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
@Schema(name = "EntrypointQosV4")
public enum Qos {
    NONE("none"),
    AUTO("auto"),
    AT_MOST_ONCE("at-most-once"),
    AT_LEAST_ONCE("at-least-once");

    private static final Map<String, Qos> maps = Map.of(
        NONE.label,
        NONE,
        AUTO.label,
        AUTO,
        AT_MOST_ONCE.label,
        AT_MOST_ONCE,
        AT_LEAST_ONCE.label,
        AT_LEAST_ONCE
    );

    @JsonValue
    private final String label;

    public static Qos fromLabel(final String label) {
        if (label != null) {
            return maps.get(label);
        }
        return null;
    }
}
