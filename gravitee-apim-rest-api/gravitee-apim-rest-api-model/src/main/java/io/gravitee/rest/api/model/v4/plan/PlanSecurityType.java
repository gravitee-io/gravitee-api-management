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
package io.gravitee.rest.api.model.v4.plan;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
@RequiredArgsConstructor
@Getter
public enum PlanSecurityType {
    /**
     * Plan which is using a key_less (ie. public) security authentication type for incoming HTTP requests.
     */
    KEY_LESS("key-less"),

    /**
     * Plan which is using an api-key security authentication type for incoming HTTP requests.
     */
    API_KEY("api-key"),

    /**
     * Plan which is using an OAuth2 security authentication type for incoming HTTP requests.
     */
    OAUTH2("oauth2"),

    /**
     * Plan which is using a JWT security authentication type for incoming HTTP requests.
     */
    JWT("jwt");

    private static final Map<String, PlanSecurityType> maps = Map.of(
        KEY_LESS.label,
        KEY_LESS,
        API_KEY.label,
        API_KEY,
        OAUTH2.label,
        OAUTH2,
        JWT.label,
        JWT
    );

    @JsonValue
    private final String label;

    public static PlanSecurityType fromLabel(final String label) {
        return maps.get(label);
    }
}
