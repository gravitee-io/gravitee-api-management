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
package io.gravitee.gamma.rest.core.observability.analytics.port.service_provider;

import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;

/**
 * Core-side port onto the analytics engine catalog, in Gamma-native types so the core stays independent of
 * the APIM analytics-engine model.
 *
 * @author GraviteeSource Team
 */
public interface AnalyticsDefinitionPort {
    /** The metrics existing for this API kind, in catalog order. */
    List<MetricDefinition> metricsOf(ApiType apiKind);
}
