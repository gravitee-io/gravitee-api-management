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
package io.gravitee.apim.reporter.elasticsearch.mapping.datastream;

import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.elasticsearch.version.Version;

/**
 * How a cluster manages the lifecycle of data stream backing indices.
 *
 * @param policyPropertyName the index setting attaching an ILM policy, or null on OpenSearch: ISM ignores its own setting, and
 * an ISM policy reaches a data stream through its {@code ism_template}
 * @param dataRetentionSupported whether the cluster has a data stream lifecycle, which carries a {@code data_retention}
 */
public record DataStreamLifecycle(String policyPropertyName, boolean dataRetentionSupported) {
    public static DataStreamLifecycle of(Version version, ReporterConfiguration configuration) {
        if (version.isOpenSearch()) {
            return openSearch();
        }
        return elasticsearch(configuration.getIndexLifecyclePolicyPropertyName(), version.canUseDataStreamLifecycle());
    }

    public static DataStreamLifecycle elasticsearch(String policyPropertyName, boolean dataRetentionSupported) {
        return new DataStreamLifecycle(policyPropertyName, dataRetentionSupported);
    }

    public static DataStreamLifecycle openSearch() {
        return new DataStreamLifecycle(null, false);
    }
}
