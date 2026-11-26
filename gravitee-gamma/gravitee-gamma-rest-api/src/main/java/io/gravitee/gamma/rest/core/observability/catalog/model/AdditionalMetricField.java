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
import java.util.Optional;
import java.util.Set;

/**
 * An additional metric field: the key a plugin, the gateway or a policy writes ({@code rawKey}) under one of the
 * reporter's maps, and its value type. Its wire name is {@code additional-metrics.<rawKey>} (type prefix included in
 * the key) or {@code custom.<rawKey>} (assign-metrics values, keywords). Names are read by the platform's parser,
 * through {@link io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort}.
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricField(Root root, String rawKey, AdditionalMetricValueType valueType) {
    public static final String NAME_PREFIX = Root.ADDITIONAL_METRICS.prefix();
    private static final String RESERVED_SEGMENT = AdditionalMetricNamespace.CUSTOM + "_";

    /** A key of the {@code additional-metrics} map. */
    public AdditionalMetricField(String rawKey, AdditionalMetricValueType valueType) {
        this(Root.ADDITIONAL_METRICS, rawKey, valueType);
    }

    public String name() {
        return root.prefix() + rawKey;
    }

    /**
     * The key served as {@code CatalogEntryFlags.rawKey}: an additional key as the {@code additionalMetrics} of a log
     * carry it; a custom key with its {@code custom.} root, so it never reads as the additional key of the same spelling.
     */
    public String servedRawKey() {
        return root == Root.CUSTOM ? name() : rawKey;
    }

    /** {@code additional-metrics.<type>_custom_<name>}: kept for the typed custom metrics of assign-metrics. */
    public boolean isReserved() {
        // The typed family lives in the additional-metrics map (OBS-172); under the custom root that name is a plain key.
        return root == Root.ADDITIONAL_METRICS && rawKey.startsWith(valueType.prefix() + RESERVED_SEGMENT);
    }

    /** Whether a record may give the field a unit: a custom metric takes none, whatever its value type. */
    public boolean takesUnit() {
        return root != Root.CUSTOM;
    }

    /**
     * {@code additional-metrics.<type>_custom.<name>}: parses, but reads another map than the policy's
     * {@code custom.<name>}, so a record of it would be a decoy that never matches the custom metric it looks like.
     */
    public boolean namesCustomMetric() {
        return root == Root.ADDITIONAL_METRICS && rawKey.startsWith(valueType.prefix() + Root.CUSTOM.prefix());
    }

    /** The custom metric a {@link #namesCustomMetric() decoy} looks like: {@code custom.<name>}; a decoy only. */
    public String customMetricLookalike() {
        if (!namesCustomMetric()) {
            throw new IllegalStateException(name() + " does not name a custom metric after its type prefix");
        }
        return Root.CUSTOM.prefix() + rawKey.substring(valueType.prefix().length() + Root.CUSTOM.prefix().length());
    }

    public String derivedLabel() {
        return AdditionalMetricLabel.derive(this);
    }

    /** The namespace the catalog serves for the key; see {@link AdditionalMetricNamespace#of}. */
    public Optional<String> namespace() {
        return AdditionalMetricNamespace.of(this);
    }

    /** The API kinds the key's namespace ties it to; see {@link AdditionalMetricNamespace#apiTypesOf}. */
    public Set<ApiType> apiTypes() {
        return AdditionalMetricNamespace.apiTypesOf(this);
    }

    /** The map of a request document the reporter writes a key under. */
    public enum Root {
        ADDITIONAL_METRICS("additional-metrics."),
        CUSTOM("custom.");

        private final String prefix;

        Root(String prefix) {
            this.prefix = prefix;
        }

        /** The start of the wire name of a key under this root, the {@code .} included. */
        public String prefix() {
            return prefix;
        }
    }
}
