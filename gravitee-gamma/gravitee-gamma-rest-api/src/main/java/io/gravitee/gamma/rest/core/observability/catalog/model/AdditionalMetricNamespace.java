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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The namespace of an additional metric key, the second segment of {@code <type>_<namespace>_<name>}, and the API
 * kinds it ties the key to. The segment is a writing convention: the reporter API enforces the type prefix and
 * nothing reads the namespace on the write path, so the catalog reads it here, on declared and discovered keys only.
 * A key a built-in reads is scoped by the built-in's own catalog.
 *
 * <p>The kinds come from a static map of the namespaces Gravitee components write under. The map is interim: OBS-194
 * (the plugin metrics descriptor) has each plugin describe the keys it writes, every descriptor replaces its entries
 * here, and OBS-194 deletes this map once none is left. A namespace the map does not know ties the key to no kind;
 * {@value #CUSTOM} is the user's own, the reserved {@code <type>_custom_<name>} keys of assign-metrics.
 *
 * @author GraviteeSource Team
 */
public final class AdditionalMetricNamespace {

    /** The namespace of a user key, the one {@link AdditionalMetricField#isReserved} reserves. */
    public static final String CUSTOM = "custom";

    private static final Set<ApiType> HTTP_FAMILY = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A);

    /**
     * The API kinds each Gravitee namespace writes for, every entry pinned by {@code AdditionalMetricNamespaceTest}:
     * the LLM endpoint and the PII and semantic-caching policies, the MCP entrypoint and endpoints, the native Kafka
     * reactor, the webhook entrypoint, the Edge gateway, the AuthZ enforcement point, and the gateway core's failover
     * and quota, written on every kind of the HTTP family.
     */
    public static final Map<String, Set<ApiType>> GRAVITEE_API_TYPES = Map.ofEntries(
        Map.entry("llm-proxy", Set.of(ApiType.LLM)),
        Map.entry("pii", Set.of(ApiType.LLM)),
        Map.entry("semantic-caching", Set.of(ApiType.LLM)),
        Map.entry("mcp-proxy", Set.of(ApiType.MCP)),
        Map.entry("native-kafka", Set.of(ApiType.NATIVE)),
        Map.entry("webhook", Set.of(ApiType.MESSAGE)),
        Map.entry("edge", Set.of(ApiType.EDGE)),
        Map.entry("authz", Set.of(ApiType.AUTHZ)),
        Map.entry("failover", HTTP_FAMILY),
        Map.entry("quota", HTTP_FAMILY)
    );

    private AdditionalMetricNamespace() {}

    /**
     * The namespace segment of a key: what follows the type prefix up to the next {@code _}, when a name follows that
     * {@code _}. Empty for a key of one segment, which has no namespace, and for a key with nothing after it.
     */
    public static Optional<String> segmentOf(AdditionalMetricField field) {
        String rawKey = field.rawKey();
        String prefix = field.valueType().prefix();
        if (!rawKey.startsWith(prefix)) {
            return Optional.empty();
        }
        int start = prefix.length();
        int end = rawKey.indexOf('_', start);
        if (end <= start || end + 1 >= rawKey.length()) {
            return Optional.empty();
        }
        return Optional.of(rawKey.substring(start, end));
    }

    /**
     * The namespace served on a catalog entry: a Gravitee namespace the map knows, {@value #CUSTOM} on a user key,
     * nothing otherwise. An unknown namespace is served as none, its segment kept in the derived label: the entry
     * reads as one of the "other fields".
     */
    public static Optional<String> of(AdditionalMetricField field) {
        return segmentOf(field).filter(segment -> segment.equals(CUSTOM) || GRAVITEE_API_TYPES.containsKey(segment));
    }

    /** The API kinds the key's namespace ties it to; none for a user key or a namespace the map does not know. */
    public static Set<ApiType> apiTypesOf(AdditionalMetricField field) {
        return segmentOf(field).map(GRAVITEE_API_TYPES::get).orElse(Set.of());
    }

    /**
     * Whether the namespace names the one API kind the key is listed under, so the derived label leaves it out:
     * "Tokens cached" under LLM. A namespace of several kinds, {@code failover} or {@code quota}, names no group of
     * its own and stays in the label: "Failover count". Adding a kind to a namespace therefore changes the labels of
     * its keys, which is why the pin test fails on any change to the map.
     */
    static boolean namesOneKind(String segment) {
        Set<ApiType> kinds = GRAVITEE_API_TYPES.get(segment);
        return kinds != null && kinds.size() == 1;
    }
}
