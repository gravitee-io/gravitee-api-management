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
import java.util.Set;

/**
 * Registry of the {@code entrypoint-id} values observability knows about, each with the scope it was
 * given. The Elasticsearch adapters derive the default scope of the observability signals from it and
 * nowhere else: the analytics engine ({@code FilterAdapter}) leaves out {@link #ANALYTICS_EXCLUDED_IDS},
 * and the logs query builder ({@code SearchMetricsQueryAdapter}) leaves out {@link #LOGS_EXCLUDED_IDS}
 * when Gamma asks for the default scope.
 *
 * <p>A default states what it leaves out, never what it covers. An entrypoint nobody has declared here is
 * therefore counted rather than silently dropped, and the Management API warns once at startup about every
 * installed entrypoint without a decision — the two halves of the same fail-open bargain.
 *
 * <p>Requests without an entrypoint id: a document written for a request refused before an entrypoint
 * connector was selected carried no {@code entrypoint-id} until the gateway started attributing those
 * requests at report time. Such documents belong to the default scope only; an explicit {@code ENTRYPOINT}
 * filter is exact and reaches them through the synthetic {@link #NO_ENTRYPOINT_VALUE} value.
 *
 * <p>The legacy v4 analytics adapters under {@code repository-elasticsearch/v4/analytics/adapter} keep
 * their own lists on purpose: they serve the Console's v4 analytics endpoints, and widening them is a
 * behaviour change owned by OBS-69, not by this registry.
 *
 * <p>The scope is declared by hand rather than derived from plugin metadata, because the plugin SPI cannot
 * partition it: {@code http-get} and {@code sse} are both Message-API entrypoints on the HTTP listener, one
 * counted as request traffic and one not, and {@code mcp} declares the same {@code PROXY} support as
 * {@code http-proxy}. A derived set would also fail silently — the gateway and the Management API load
 * separate plugin trees, an entrypoint missing on one side would simply vanish from the totals, and
 * uninstalling a plugin would erase its historical traffic — while {@code FilterAdapter} runs inside the
 * Elasticsearch repository plugin and cannot see the plugin registry at all.
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
    /** The plugin id differs from the artifact name: {@code plugin.properties} of gravitee-entrypoint-mcp-tool-server says {@code mcp}. */
    MCP("mcp", "gravitee-entrypoint-mcp-tool-server", Scope.HTTP),
    MCP_STUDIO("mcp-studio", "gravitee-entrypoint-mcp-studio", Scope.HTTP),
    AGENT_TO_AGENT("agent-to-agent", "gravitee-entrypoint-agent-to-agent", Scope.HTTP),

    NATIVE_KAFKA("native-kafka", "gravitee-entrypoint-native-kafka", Scope.LOGS_ONLY),

    /** Reported by Edge agents, which ship no entrypoint plugin of their own. */
    EDGE("edge", null, Scope.DEDICATED_FAMILY),
    /** The plugin id differs from the artifact name: {@code plugin.properties} of gravitee-entrypoint-authz 1.2.0 says {@code authzen}. */
    AUTHZ("authzen", "gravitee-entrypoint-authz", Scope.DEDICATED_FAMILY),

    /*
     * The async entrypoints of Message APIs. LOGS_ONLY rather than EXCLUDED since the logs signal
     * serves Message APIs: their connection documents are HTTP connection documents like any other,
     * and leaving them out returned an empty page with no error.
     */
    SSE("sse", "gravitee-entrypoint-sse", Scope.LOGS_ONLY),
    WEBHOOK("webhook", "gravitee-entrypoint-webhook", Scope.LOGS_ONLY),
    WEBSOCKET("websocket", "gravitee-entrypoint-websocket", Scope.LOGS_ONLY),

    TCP_PROXY("tcp-proxy", "gravitee-apim-plugin-entrypoint-tcp-proxy", Scope.EXCLUDED);

    /**
     * What observability does with an entrypoint's traffic. Every entrypoint must pick one, so a
     * new one cannot reach the product without someone stating the intent.
     */
    public enum Scope {
        /**
         * Counted by the HTTP request scope: the analytics engine, Gamma analytics and Gamma logs. The
         * criterion is the shape of the traffic, not the API kind — {@code http-get} and {@code http-post}
         * are Message-API entrypoints serving request-shaped calls.
         */
        HTTP,

        /**
         * Served by Gamma logs but not by analytics. Native connections have their own documents and
         * their own dashboard tiles; adding them to the analytics default would change every
         * environment-wide total. The divergence is deliberate.
         *
         * <p>The subscription entrypoints sit here for the same reason, one signal at a time: their
         * connections are not requests and their duration would distort every latency aggregate, which
         * is an argument about analytics — so they stay out of {@link #HTTP}. It is not an argument
         * about logs, where a connection is exactly what the screen lists, and where excluding them
         * meant a Message API served over SSE answered an empty page with no error.
         */
        LOGS_ONLY,

        /**
         * Selected by a query family of its own — the Edge and Authz families each scope themselves —
         * never by the default entrypoint predicate.
         */
        DEDICATED_FAMILY,

        /**
         * Outside what the observability signals cover at all: TCP. Being a Message-API entrypoint is
         * not the criterion — see {@link #HTTP} and {@link #LOGS_ONLY}.
         */
        EXCLUDED,
    }

    /** Entrypoints the analytics default leaves out: everything not counted as HTTP request traffic. */
    public static final List<String> ANALYTICS_EXCLUDED_IDS = idsWithScope(Scope.LOGS_ONLY, Scope.DEDICATED_FAMILY, Scope.EXCLUDED);

    /** Entrypoints the logs default leaves out. Narrower than analytics: the logs screen lists connections. */
    public static final List<String> LOGS_EXCLUDED_IDS = idsWithScope(Scope.DEDICATED_FAMILY, Scope.EXCLUDED);

    /**
     * Synthetic {@code ENTRYPOINT} filter value standing for documents written without an
     * {@code entrypoint-id}. Offered by the values endpoint, translated to a field-missing clause.
     */
    public static final String NO_ENTRYPOINT_VALUE = "(none)";

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

    /** Whether an {@code entrypoint-id} has a scope decision recorded here. */
    public static boolean declares(String id) {
        return Arrays.stream(values()).anyMatch(entrypoint -> entrypoint.id.equals(id));
    }

    private static List<String> idsWithScope(Scope... scopes) {
        var selected = Set.of(scopes);
        return Arrays.stream(values())
            .filter(entrypoint -> selected.contains(entrypoint.scope))
            .map(ObservabilityEntrypoints::id)
            .toList();
    }
}
