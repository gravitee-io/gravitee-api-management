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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import io.gravitee.gamma.rest.core.observability.catalog.exception.InvalidMetricDefinitionException;

/**
 * The fields a caller sets on a record. A write replaces all of them: an absent label or unit clears it, an absent
 * {@code exposed} means exposed.
 *
 * @author GraviteeSource Team
 */
public record MetricDefinitionChanges(String label, String description, String unit, boolean exposed) {
    public static final int MAX_LABEL_LENGTH = 64;
    public static final int MAX_DESCRIPTION_LENGTH = 512;

    /** Trims and checks what the caller sent; blank text reads as absent. */
    public static MetricDefinitionChanges of(String label, String description, String unit, Boolean exposed) {
        return new MetricDefinitionChanges(
            bounded(label, MAX_LABEL_LENGTH, "label"),
            bounded(description, MAX_DESCRIPTION_LENGTH, "description"),
            AdditionalMetricUnit.validate(unit),
            exposed == null || exposed
        );
    }

    private static String bounded(String text, int maxLength, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String stripped = text.strip();
        if (stripped.length() > maxLength) {
            throw InvalidMetricDefinitionException.tooLong(field, maxLength);
        }
        return stripped;
    }
}
