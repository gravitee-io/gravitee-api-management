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
package io.gravitee.rest.api.model.alert;

import static io.gravitee.rest.api.model.alert.ThresholdType.PERCENT_RATE;
import static java.util.Collections.singletonList;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(enumAsRef = true)
public enum MetricType {
    QUOTA("Quota", "Quota percent", singletonList(PERCENT_RATE));

    private String description;
    private String eventProperty;
    private List<ThresholdType> thresholds;

    MetricType(String description, String eventProperty, List<ThresholdType> thresholds) {
        this.description = description;
        this.eventProperty = eventProperty;
        this.thresholds = thresholds;
    }

    public String description() {
        return description;
    }

    public String eventProperty() {
        return eventProperty;
    }

    public List<ThresholdType> thresholds() {
        return thresholds;
    }
}
