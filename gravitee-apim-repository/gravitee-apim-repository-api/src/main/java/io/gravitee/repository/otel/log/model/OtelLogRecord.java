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
package io.gravitee.repository.otel.log.model;

import java.time.Instant;
import java.util.Map;

/**
 * Single OTel log record, the unified data shape behind both span-event logs and gateway-captured payload
 * logs. Both are stored as OTel {@code LogRecord} documents in the backend (the ES collector's logs data
 * stream, or Loki) — the record carries no kind discriminator because the OTel attribute model already
 * does it: span events carry {@code event.name} in {@link #attributes()} (per the OTel logs data model
 * "Event" convention) and typically no {@link #body()}; payload logs ({@code gravitee-reporter-otel}) carry
 * the captured payload in {@link #body()} and no {@code event.name}. Consumers that need the slice
 * partition on {@code attributes().containsKey("event.name")}.
 *
 * @param traceId           parent trace id (32-character lowercase hex). Required.
 * @param spanId            parent span id (16-character lowercase hex). Required for stitching — entries
 *                          with a null {@code spanId} are dropped at the repository boundary.
 * @param timestamp         record timestamp.
 * @param severity          OTel severity text (e.g. {@code INFO}, {@code ERROR}). Often {@code null} for
 *                          span events.
 * @param body              record body. Used by payload logs to carry the captured request/response
 *                          payload; usually {@code null} for span events.
 * @param attributes        OTel record attributes, flattened to a string→string map (nested attribute
 *                          keys dot-joined: {@code http.status_code}, {@code event.name}, …). For span
 *                          events {@code event.name} is populated here.
 * @param resourceAttributes producer's OTel resource attributes (e.g. {@code gravitee.module},
 *                          {@code gravitee.env.id}), flattened the same way.
 *
 * @author GraviteeSource Team
 */
public record OtelLogRecord(
    String traceId,
    String spanId,
    Instant timestamp,
    String severity,
    String body,
    Map<String, String> attributes,
    Map<String, String> resourceAttributes
) {}
