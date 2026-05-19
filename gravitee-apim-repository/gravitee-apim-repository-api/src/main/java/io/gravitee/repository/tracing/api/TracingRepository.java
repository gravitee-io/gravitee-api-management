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
package io.gravitee.repository.tracing.api;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;

/**
 * Backend-agnostic port for querying stored traces. Implementations adapt this contract to a specific tracing backend
 * (Grafana Tempo today; Jaeger / direct OTLP could follow).
 * <p>
 * {@link QueryContext} carries the caller's org/env tenancy and is used to resolve tenant-aware indices / headers
 * (e.g. {@code X-Scope-OrgID} on Tempo, the {@code {orgId}} placeholder substituted into the Elasticsearch index
 * template). All other query scoping — environment, module, region, anything attached to the producer's OTel
 * resource — flows through {@link TraceSearchCriteria#resourceAttributeFilters()} (for {@code searchTraces}) or
 * the equivalent map argument on {@link #getTrace(QueryContext, String, Map)}. Implementations apply the map
 * uniformly; they have no built-in knowledge of which keys belong on the resource.
 *
 * @author GraviteeSource Team
 */
public interface TracingRepository {
    /**
     * List traces matching the given filter. The returned {@link Trace} entries carry summary fields only;
     * {@code spans} is always empty — call {@link #getTrace(QueryContext, String, Map)} to fetch a trace's spans.
     *
     * @param queryContext caller's org/env, used to resolve tenant-aware indices/headers
     * @param criteria filter (attribute filters, resource-attribute filters, time window, limit)
     * @return list of matching traces; emits an empty list when no trace matches
     */
    Single<List<Trace>> searchTraces(QueryContext queryContext, TraceSearchCriteria criteria);

    /**
     * Fetch a single trace by id, including its full span tree.
     *
     * @param queryContext caller's org/env, used to resolve tenant-aware indices/headers
     * @param traceId the trace identifier
     * @param resourceAttributeFilters resource-attribute scope (env, module, …) the trace must match. Empty map
     *                                 means "no resource scoping" — the caller is responsible for adding
     *                                 isolation filters (env, module) so a known trace id from another scope
     *                                 isn't returned. Same shape as
     *                                 {@link TraceSearchCriteria#resourceAttributeFilters()}.
     * @return the trace; completes empty when the backend has no trace for that id, or when the trace exists
     *         but doesn't satisfy the resource filter
     */
    Maybe<Trace> getTrace(QueryContext queryContext, String traceId, Map<String, String> resourceAttributeFilters);
}
