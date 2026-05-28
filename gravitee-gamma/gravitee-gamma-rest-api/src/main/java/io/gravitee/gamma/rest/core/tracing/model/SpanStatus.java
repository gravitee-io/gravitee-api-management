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
 * OTel-aligned span status. Lifted out of the raw {@code attributes['otel.status_code']} attribute on
 * the wire so consumers can render error styling without knowing about the underlying attribute key
 * or the {@code Error / STATUS_CODE_ERROR / 2} normalisation done one layer below in the ES adapter.
 *
 * @author GraviteeSource Team
 */
public enum SpanStatus {
    UNSET,
    OK,
    ERROR;

    /**
     * Maps the normalised {@code otel.status_code} attribute value (already in {@code UNSET / OK / ERROR}
     * form by the time the SPI returns it) to the enum. Falls back to {@link #UNSET} for unknown / missing
     * values rather than throwing — backends that don't emit status at all collapse to "unknown" rather
     * than failing the whole detail fetch.
     */
    public static SpanStatus fromAttribute(String raw) {
        if (raw == null) {
            return UNSET;
        }
        return switch (raw) {
            case "OK" -> OK;
            case "ERROR" -> ERROR;
            default -> UNSET;
        };
    }
}
