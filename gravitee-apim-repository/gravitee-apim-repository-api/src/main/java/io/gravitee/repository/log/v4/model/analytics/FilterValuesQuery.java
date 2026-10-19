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
package io.gravitee.repository.log.v4.model.analytics;

import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * @param trafficScope narrows the calls of {@code apiIds} the values come from, absent when the caller is not
 *                     scoped by API kind
 * @param values       the only values to offer, absent when every value of the field is offered
 * @param apiProductId narrows the calls to those made through one API product, absent when it does not matter
 */
@Builder
public record FilterValuesQuery(
    String esFieldName,
    Long from,
    Long to,
    int size,
    Map<String, Object> afterKey,
    String searchPattern,
    Set<String> apiIds,
    TrafficScope trafficScope,
    Set<String> values,
    String apiProductId
) {}
