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
package io.gravitee.repository.analytics.engine.api.query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registry of the {@code entrypoint-id} values observability knows about, each with the scope it
 * was given. The default entrypoint scoping of the observability signals is declared here and
 * nowhere else:
 * <ul>
 *   <li>the analytics engine ({@code FilterAdapter})</li>
 *   <li>Gamma analytics and dashboards ({@code AnalyticsRequestPipeline})</li>
 *   <li>Gamma logs ({@code SearchObservabilityLogsUseCase})</li>
 * </ul>
 * The legacy v4 analytics adapters under {@code repository-elasticsearch/v4/analytics/adapter} keep
 * their own lists on purpose: they serve the Console's v4 analytics endpoints, and widening them is a
 * behaviour change owned by OBS-69, not by this registry.
 *
 * <p>The set is declared by hand rather than derived from the installed entrypoint plugins, because
 * the scope is a dashboard decision, not a plugin property: {@code mcp} carries the same metadata as
 * {@code http-proxy} yet is not counted yet, and {@code http-get} / {@code http-post} are Message
 * entrypoints counted on purpose. A derived set would also fail silently — the gateway and the
 * Management API load separate plugin trees, an entrypoint missing on one side would simply vanish
 * from the totals, and uninstalling a plugin would erase its historical traffic — while
 * {@code FilterAdapter} runs inside the Elasticsearch repository plugin and cannot see the plugin
 * registry at all.
 *
 * <p>Two tests hold the registry to the product instead: {@code ObservabilityEntrypointsTest} checks
 * the artifacts the distribution pom bundles (fast, runs whenever this module changes), and
 * {@code ObservabilityEntrypointsDistributionTest} in the distribution reactor reads the id every
 * bundled plugin declares in its {@code plugin.properties} (runs whenever the distribution changes,
 * i.e. when a new entrypoint reaches the product).
 */
public enum ObservabilityEntrypoints {
    /*
     * Declaration order is the order the ids reach Elasticsearch. Kept stable so query bodies stay
     * comparable across versions.
     */
    HTTP_GET("http-get", "gravitee-entrypoint-http-get", Scope.HTTP),
    HTTP_POST("http-post", "gravitee-entrypoint-http-post", Scope.HTTP),
    HTTP_PROXY("http-proxy", "gravitee-apim-plugin-entrypoint-http-proxy", Scope.HTTP),
    LLM_PROXY("llm-proxy", "gravitee-entrypoint-llm-proxy", Scope.HTTP),
    MCP_PROXY("mcp-proxy", "gravitee-entrypoint-mcp-proxy", Scope.HTTP),
    A2A_PROXY("a2a-proxy", "gravitee-entrypoint-a2a-proxy", Scope.HTTP),

    NATIVE_KAFKA("native-kafka", "gravitee-entrypoint-native-kafka", Scope.LOGS_ONLY),

    /** Reported by Edge agents, which ship no entrypoint plugin of their own. */
    EDGE("edge", null, Scope.DEDICATED_FAMILY),
    /** The plugin id differs from the artifact name: {@code plugin.properties} of gravitee-entrypoint-authz 1.2.0 says {@code authzen}. */
    AUTHZ("authzen", "gravitee-entrypoint-authz", Scope.DEDICATED_FAMILY),

    MCP("mcp", "gravitee-entrypoint-mcp-tool-server", Scope.PENDING),
    MCP_STUDIO("mcp-studio", "gravitee-entrypoint-mcp-studio", Scope.PENDING),
    AGENT_TO_AGENT("agent-to-agent", "gravitee-entrypoint-agent-to-agent", Scope.PENDING),

    SSE("sse", "gravitee-entrypoint-sse", Scope.EXCLUDED),
    WEBHOOK("webhook", "gravitee-entrypoint-webhook", Scope.EXCLUDED),
    WEBSOCKET("websocket", "gravitee-entrypoint-websocket", Scope.EXCLUDED),
    TCP_PROXY("tcp-proxy", "gravitee-apim-plugin-entrypoint-tcp-proxy", Scope.EXCLUDED);

    /**
     * What observability does with an entrypoint's traffic. Every entrypoint must pick one, so a
     * new one cannot reach the product without someone stating the intent.
     */
    public enum Scope {
        /** Counted by the HTTP request scope: the analytics engine, Gamma analytics and Gamma logs. */
        HTTP,

        /**
         * Served by Gamma logs but not by analytics. Native connections have their own documents and
         * their own dashboard tiles; adding them to the analytics default would change every
         * environment-wide total. The divergence is deliberate and tracked by OBS-18.
         */
        LOGS_ONLY,

        /**
         * Selected by a query family of its own — the Edge and Authz families each scope themselves —
         * never by the default entrypoint predicate.
         */
        DEDICATED_FAMILY,

        /**
         * Carries traffic observability should count but does not yet. Listed so the gap is visible
         * rather than accidental; OBS-18 promotes these to {@link #HTTP}.
         */
        PENDING,

        /** Message and TCP APIs, outside what the observability signals cover today. */
        EXCLUDED,
    }

    /** Entrypoints counted by the analytics engine, Gamma analytics and Gamma logs alike. */
    public static final List<String> HTTP_SCOPE_IDS = idsWithScope(Scope.HTTP);

    /** {@link #HTTP_SCOPE_IDS} plus the entrypoints only the logs signal serves. */
    public static final List<String> LOGS_SCOPE_IDS = Stream.concat(
        HTTP_SCOPE_IDS.stream(),
        idsWithScope(Scope.LOGS_ONLY).stream()
    ).toList();

    private final String id;
    private final String pluginArtifactId;
    private final Scope scope;

    ObservabilityEntrypoints(String id, String pluginArtifactId, Scope scope) {
        this.id = id;
        this.pluginArtifactId = pluginArtifactId;
        this.scope = scope;
    }

    /** The {@code entrypoint-id} value carried by the reported documents. */
    public String id() {
        return id;
    }

    /** The plugin the distribution bundles this entrypoint as, absent when it ships no plugin. */
    public Optional<String> pluginArtifactId() {
        return Optional.ofNullable(pluginArtifactId);
    }

    private static List<String> idsWithScope(Scope scope) {
        return Arrays.stream(values())
            .filter(entrypoint -> entrypoint.scope == scope)
            .map(ObservabilityEntrypoints::id)
            .toList();
    }
}
