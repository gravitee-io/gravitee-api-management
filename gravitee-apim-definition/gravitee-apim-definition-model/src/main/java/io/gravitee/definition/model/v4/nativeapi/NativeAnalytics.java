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
package io.gravitee.definition.model.v4.nativeapi;

import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NativeAnalytics {

    /** Gates event-metrics reporting. Independent of reporterMetricsEnabled. */
    // TODO: rename to `eventMetricsEnabled` for symmetry with `reporterMetricsEnabled`. Breaking change — persisted in v4 API definitions.
    @Builder.Default
    protected boolean enabled = true;

    /** Gates the connection-metrics reporter on the gateway. Independent of analytics.enabled. */
    @Builder.Default
    protected boolean reporterMetricsEnabled = true;

    /**
     * Which connection lifecycle events are reported, or {@code null} when the API has never been configured.
     *
     * <p>Emphatically <b>not</b> a {@code @Builder.Default}: null has to keep meaning "never configured" so it
     * can resolve to {@link NativeConnectionEvent#LEGACY_DEFAULTS}. Give it a default here and every API
     * deployed before this field existed silently starts reporting DISCONNECTED on upgrade, roughly doubling
     * the documents it writes — and an API whose owner deliberately reports nothing gets it turned back on.
     *
     * <p>Read it through {@link #effectiveConnectionEvents()} rather than directly, so the legacy rule is
     * applied in one place.
     */
    protected Set<NativeConnectionEvent> connectionEvents;

    /** Per-API OpenTelemetry tracing toggle (enabled + verbose). */
    private Tracing tracing;

    /**
     * The events this API actually reports, resolving an unconfigured API to what it already does today.
     *
     * <p>Not named {@code getEffectiveConnectionEvents}: the {@code get} prefix would make Jackson serialize it
     * as a property of the API definition, persisting a derived value that would then outlive the rule that
     * produced it.
     */
    public Set<NativeConnectionEvent> effectiveConnectionEvents() {
        return connectionEvents != null ? connectionEvents : NativeConnectionEvent.LEGACY_DEFAULTS;
    }
}
