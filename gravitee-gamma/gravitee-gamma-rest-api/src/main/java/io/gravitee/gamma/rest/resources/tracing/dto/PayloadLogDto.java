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
package io.gravitee.gamma.rest.resources.tracing.dto;

import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import java.util.Map;

/**
 * Payload log in the detail view (gateway-captured request / response). Same omit-{@code spanId} rule
 * as {@link SpanEventDto}. See {@link TraceSummaryDto} for the ms-since-epoch timestamp rationale.
 */
public record PayloadLogDto(long timestampEpochMs, String severity, String body, Map<String, String> attributes) {
    public static PayloadLogDto from(PayloadLog log) {
        return new PayloadLogDto(
            log.timestamp() != null ? log.timestamp().toEpochMilli() : 0L,
            log.severity(),
            log.body(),
            log.attributes()
        );
    }
}
