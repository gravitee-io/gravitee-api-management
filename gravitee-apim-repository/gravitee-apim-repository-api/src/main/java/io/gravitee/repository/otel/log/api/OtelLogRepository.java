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
package io.gravitee.repository.otel.log.api;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Backend-agnostic port for reading OTel log records. Sits alongside
 * {@link io.gravitee.repository.tracing.api.TracingRepository}. The OTel logs data model unifies two
 * shapes that the SPI doesn't split:
 * <ul>
 *   <li>Span events — written by the OTel collector's ES {@code elasticsearch} exporter (in
 *       {@code mapping.mode: otel}) as separate log documents in the logs data stream. Carry
 *       {@code event.name} in their attributes. Only the ES-backed implementation returns them; in the
 *       Tempo + Loki stack span events live inline on the spans and are returned by
 *       {@code TracingRepository.getTrace}, so a Loki-backed implementation simply doesn't surface
 *       any record carrying {@code event.name}.</li>
 *   <li>Payload logs — gateway-captured request/response payloads written by
 *       {@code gravitee-reporter-otel}. Both Elasticsearch and Loki implementations return these.</li>
 * </ul>
 * Consumers that need to partition span events vs payload logs do it on
 * {@code attributes().containsKey("event.name")}.
 *
 * @author GraviteeSource Team
 */
public interface OtelLogRepository {
    /**
     * Fetch OTel log records matching the given criteria — span events and payload logs alike — in
     * {@code @timestamp} ascending order so consumers can render them as a timeline without re-sorting.
     * <p>
     * Returns an empty list when no records match, when the resource filter excludes everything, or when
     * the backing storage isn't reachable (e.g. the logs data stream doesn't exist because the operator
     * didn't enable the logs pipeline). Never errors on "not found" — the gamma consumer is expected to
     * still render the trace's spans even without any associated logs.
     *
     * @param queryContext caller's org/env tenant context, used to resolve the target index / data
     *                     stream / Loki tenant. Mandatory: {@link QueryContext#orgId()} hard-partitions
     *                     so a known trace id from another scope can't leak through.
     * @param criteria     query shape — see {@link OtelLogSearchCriteria}. Every field is optional;
     *                     callers are responsible for adding enough filters (trace id, resource scope,
     *                     time range) that the response stays bounded and tenant-isolated.
     */
    Single<List<OtelLogRecord>> findLogs(QueryContext queryContext, OtelLogSearchCriteria criteria);
}
