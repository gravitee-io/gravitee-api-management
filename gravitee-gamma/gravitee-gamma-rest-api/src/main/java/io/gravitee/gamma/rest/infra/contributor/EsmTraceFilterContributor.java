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
package io.gravitee.gamma.rest.infra.contributor;

import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import java.util.List;

/**
 * Trace filters for the ESM module — native Kafka APIs. The cross-module set
 * ({@link CommonTraceFilterContributor}) is HTTP-shaped and says nothing about a Kafka trace: these
 * are the dimensions a Kafka Service is actually debugged along.
 *
 * <h2>Why this lives in the host and not in the ESM plugin</h2>
 * The SPI javadoc says per-module contributors live in their own module's codebase, and that is the
 * target. It does not work yet: gamma modules are loaded through
 * {@code GammaModulePluginHandler} in <b>isolated plugin classloaders</b>, while
 * {@link io.gravitee.gamma.rest.infra.adapter.SpiTraceFilterRegistry} runs
 * {@code ServiceLoader.load(...)} against the rest-api classpath — a contributor shipped inside the
 * ESM plugin jar is never discovered. (The analytics/logs {@code FilterContributor} carries the same
 * caveat: "runtime discovery of module contributions across the isolated gamma plugin classloaders
 * is NOT handled here".) Move this class into {@code gravitee-gamma-module-esm} once the plugin
 * handler exposes a registration hook.
 *
 * <h2>Attribute mapping</h2>
 * Every filter here maps to a span attribute the native Kafka reactor really emits — see
 * {@code KafkaTracingHelper} in {@code gravitee-reactor-native-kafka}, and
 * {@link io.gravitee.gamma.rest.core.tracing.use_case.SearchTraceFilterTranslator} for the mapping
 * itself. Discovery and search MUST stay in sync: a filter advertised here that the translator
 * cannot route would come back as a 400 the moment a user picks it. That is why {@code STATUS},
 * {@code DURATION_NANOS} and friends are still absent — they need top-level-field rendering, not an
 * attribute term.
 *
 * <h2>Every filter here is written on EVERY per-request span — deliberately</h2>
 * {@code ElasticsearchTracingRepository.buildQuery} ANDs attribute filters inside one
 * {@code bool.filter} evaluated <b>against a single span document</b>, then aggregates the matching
 * spans by {@code trace_id}. Two filters that are not carried by one and the same span therefore
 * never match together, however sensible the question. "Same span kind" is not enough: the
 * admission rule is that the attribute is on every per-request span, which holds for
 * {@code kafka.api.key} (set per request by {@code KafkaTracing#startPerRequestSpan}) and for
 * {@code messaging.client.id} ({@code KafkaSpanAttributeExtractor#fromHeader}, no per-api-key
 * branching). Three otherwise obvious candidates fail it:
 * <ul>
 *   <li><b>Consumer group</b> ({@code messaging.consumer.group.name}) — {@code fromRequest} sets it
 *       for {@code JOIN_GROUP}, {@code HEARTBEAT}, {@code OFFSET_COMMIT}, {@code LEAVE_GROUP} and
 *       {@code SYNC_GROUP} only; every other operation falls through to {@code Map.of()}. Pairing it
 *       with "operation = FETCH" — the most natural way to ask what a group is doing — would answer
 *       zero traces even when such a trace exists.</li>
 *   <li><b>Failure origin</b> ({@code gravitee.error.origin}) — written on the root "Kafka
 *       connection" span only ({@code KafkaApiReactor#endRootSpan}). Pairing it with any filter
 *       below ("why did my fetches fail") would answer zero traces even when such a trace exists.
 *       It needs trace-scoped evaluation — resolve matching {@code trace_id}s per filter and
 *       intersect — not a per-document AND.</li>
 *   <li><b>Topic</b> ({@code messaging.destination.name}) — {@code KafkaSpanAttributeExtractor}
 *       joins every topic of a batched request with commas and falls back to {@code "id:<uuid>"}
 *       when the request carries topic ids (fetch v12+, what a modern client sends), so an exact
 *       term silently misses those spans. Ships when the reactor emits topics as a repeated
 *       attribute.</li>
 * </ul>
 *
 * <p><b>Known caveat on the rows returned.</b> The search aggregates by {@code trace_id} over the
 * <i>matching spans only</i>, and picks each row's summary from that subset. Filtering on any of
 * these therefore returns rows whose operation, duration and span count describe the matched
 * per-request span rather than the connection. The cross-module HTTP filters do not show this
 * because {@code http.*} sits on the root span.
 *
 * <p>Types are chosen for what the value endpoint can serve today: {@code ENUM} carries its own
 * values (no round trip), {@code STRING} renders a free-text input. A {@code KEYWORD} would make the
 * UI call {@code /filters/{name}/values}, which the MVP answers with
 * {@code tracing.filter.value_listing_not_supported}.
 *
 * @author GraviteeSource Team
 */
public class EsmTraceFilterContributor implements TraceFilterContributor {

    /**
     * Matches {@code gravitee.module} as emitted by {@code KafkaApiReactorFactory} (MODULE = "esm"),
     * and the {@code module} query param the ESM console sends.
     */
    @Override
    public String moduleId() {
        return "esm";
    }

    @Override
    public List<TraceFilterSpec> getFilters() {
        return ESM_FILTERS;
    }

    /**
     * Kafka protocol requests worth filtering on. Deliberately not the full {@code ApiKeys} enum
     * (~70 values, most of them never seen through the gateway) — this is a picker, not a reference.
     * {@code SASL_HANDSHAKE} / {@code SASL_AUTHENTICATE} are absent on purpose: per-request spans are
     * created after authentication ({@code KafkaApiReactorPolicyChains#executeRequest}), which gets
     * its own {@code Authentication} span, so no span ever carries {@code kafka.api.key=SASL_*}.
     */
    private static final List<String> KAFKA_API_KEYS = List.of(
        "PRODUCE",
        "FETCH",
        "METADATA",
        "API_VERSIONS",
        "FIND_COORDINATOR",
        "JOIN_GROUP",
        "SYNC_GROUP",
        "HEARTBEAT",
        "LEAVE_GROUP",
        "OFFSET_COMMIT",
        "OFFSET_FETCH",
        "LIST_OFFSETS",
        "CREATE_TOPICS",
        "DELETE_TOPICS",
        "DESCRIBE_GROUPS",
        "LIST_GROUPS"
    );

    private static final List<TraceFilterSpec> ESM_FILTERS = List.of(
        new TraceFilterSpec("KAFKA_API_KEY", "Kafka operation", FilterType.ENUM, List.of(FilterOperator.EQ), KAFKA_API_KEYS, null),
        new TraceFilterSpec("KAFKA_CLIENT_ID", "Kafka client id", FilterType.STRING, List.of(FilterOperator.EQ), null, null)
    );
}
