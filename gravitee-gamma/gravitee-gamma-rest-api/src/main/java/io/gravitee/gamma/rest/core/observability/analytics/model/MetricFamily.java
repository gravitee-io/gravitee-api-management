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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * A group of metrics as the widget editor lists them, in display order.
 *
 * @author GraviteeSource Team
 */
public enum MetricFamily {
    GATEWAY("Gateway", null),
    LLM("LLM", ApiType.LLM),
    MESSAGE("Message", ApiType.MESSAGE),
    EDGE("Edge", ApiType.EDGE),
    NATIVE("Kafka Service", ApiType.NATIVE),
    AUTHZ("AuthZ", ApiType.AUTHZ);

    private final String label;
    private final ApiType ownKind;

    MetricFamily(String label, ApiType ownKind) {
        this.label = label;
        this.ownKind = ownKind;
    }

    public String label() {
        return label;
    }

    /**
     * The family of the one API kind a metric exists for. A metric shared by several kinds is a gateway
     * metric: the HTTP ones exist for every kind served over HTTP, LLM included.
     */
    public static MetricFamily of(Collection<ApiType> apiTypes) {
        var kinds = Set.copyOf(apiTypes);
        if (kinds.size() != 1) {
            return GATEWAY;
        }
        var kind = kinds.iterator().next();
        return Arrays.stream(values())
            .filter(family -> family.ownKind == kind)
            .findFirst()
            .orElse(GATEWAY);
    }
}
