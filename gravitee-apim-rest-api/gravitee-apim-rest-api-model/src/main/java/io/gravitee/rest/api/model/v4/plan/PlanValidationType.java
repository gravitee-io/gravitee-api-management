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
@RequiredArgsConstructor
@Getter
@Schema(enumAsRef = true, name = "PlanValidationTypeV4")
public enum PlanValidationType {
    /**
     * Subscription is automatically validated without any human action.
     */
    AUTO("auto"),

    /**
     * Subscription requires a human validation.
     */
    MANUAL("manual");

    private static final Map<String, PlanValidationType> maps = Map.of(AUTO.label, AUTO, MANUAL.label, MANUAL);

    @JsonValue
    private final String label;

    public static PlanValidationType valueOfLabel(final String label) {
        if (label != null) {
            PlanValidationType planValidationType = maps.get(label);
            if (planValidationType == null) {
                try {
                    planValidationType = valueOf(label);
                } catch (IllegalArgumentException e) {
                    // Ignore this and return null
                }
            }
            return planValidationType;
        }
        return null;
    }
}
