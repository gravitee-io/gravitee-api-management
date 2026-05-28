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
 * OTel span event — a timestamped marker attached to a span. The name comes from {@code event.name} in
 * the OTel logs data model; attributes carry the event payload (e.g. {@code gravitee.policy.id} on
 * {@code gravitee.policy.pre}). Stitched onto its parent {@link Span} by spanId.
 *
 * @author GraviteeSource Team
 */
public record SpanEvent(String spanId, String name, Instant timestamp, Map<String, String> attributes) {}
