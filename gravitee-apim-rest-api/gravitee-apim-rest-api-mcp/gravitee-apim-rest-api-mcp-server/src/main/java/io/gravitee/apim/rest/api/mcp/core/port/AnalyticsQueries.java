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
package io.gravitee.apim.rest.api.mcp.core.port;

import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.time.Instant;

/**
 * The analytics engine, in its own request and response records, restricted to the APIs the caller can see.
 */
public interface AnalyticsQueries {
    MeasuresResponse measures(AuditInfo caller, MeasuresRequest request);

    FacetsResponse facets(AuditInfo caller, FacetsRequest request);

    TimeSeriesResponse timeSeries(AuditInfo caller, TimeSeriesRequest request);

    FilterValuesPage filterValues(AuditInfo caller, String filterName, Instant from, Instant to, int perPage, String query);
}
