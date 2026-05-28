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
package io.gravitee.gamma.rest.core.tracing.model;

import java.time.Instant;
import java.util.Map;

/**
 * Gateway-captured payload log — what {@code gravitee-reporter-otel} writes when the gateway captures a
 * request / response body. Discriminated from {@link SpanEvent} in the same OTel logs data stream by the
 * absence of {@code event.name} on the source record. Stitched onto its parent {@link Span} by spanId.
 *
 * @author GraviteeSource Team
 */
public record PayloadLog(String spanId, Instant timestamp, String severity, String body, Map<String, String> attributes) {}
