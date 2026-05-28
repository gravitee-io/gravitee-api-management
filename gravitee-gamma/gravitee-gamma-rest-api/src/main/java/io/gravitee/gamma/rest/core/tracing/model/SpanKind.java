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

/**
 * OTel-aligned span kind. The tracing repository SPI doesn't expose this as a first-class field yet
 * (tracked separately) — the gamma adapter currently looks for {@code attributes['span.kind']} and
 * defaults to {@link #INTERNAL} when absent, which is the OTel-documented default.
 *
 * @author GraviteeSource Team
 */
public enum SpanKind {
    INTERNAL,
    SERVER,
    CLIENT,
    PRODUCER,
    CONSUMER;

    /**
     * Maps a span-kind attribute value to the enum. Accepts the proto-enum form
     * ({@code SPAN_KIND_SERVER}), short form ({@code Server}), and uppercase short form
     * ({@code SERVER}). Falls back to {@link #INTERNAL} for missing / unknown values rather than
     * throwing — same degradation policy as {@link SpanStatus#fromAttribute(String)}.
     */
    public static SpanKind fromAttribute(String raw) {
        if (raw == null) {
            return INTERNAL;
        }
        return switch (raw.toUpperCase()) {
            case "SERVER", "SPAN_KIND_SERVER" -> SERVER;
            case "CLIENT", "SPAN_KIND_CLIENT" -> CLIENT;
            case "PRODUCER", "SPAN_KIND_PRODUCER" -> PRODUCER;
            case "CONSUMER", "SPAN_KIND_CONSUMER" -> CONSUMER;
            default -> INTERNAL;
        };
    }
}
