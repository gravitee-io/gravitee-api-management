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
package io.gravitee.repository.noop.tracing;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;

/**
 * No-op {@link TracingRepository} for the {@code OTEL_TRACES} scope. Wired when an operator sets
 * {@code repositories.otel-traces.type=none} — typical for deployments that haven't enabled the trace
 * explorer yet, or that don't want to point tracing at a backend. Returns empty results so consumers
 * (gamma APIM trace resource, future use cases) can rely on the SPI being present without each
 * implementing their own NoOp fallback.
 *
 * @author GraviteeSource Team
 */
public class NoOpTracingRepository implements TracingRepository {

    @Override
    public Single<List<Trace>> searchTraces(QueryContext queryContext, TraceSearchCriteria criteria) {
        return Single.just(List.of());
    }

    @Override
    public Maybe<Trace> getTrace(QueryContext queryContext, String traceId, Map<String, String> resourceAttributeFilters) {
        return Maybe.empty();
    }
}
