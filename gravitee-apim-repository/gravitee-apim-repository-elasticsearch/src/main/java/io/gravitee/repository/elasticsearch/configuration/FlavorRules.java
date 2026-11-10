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
package io.gravitee.repository.elasticsearch.configuration;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

/**
 * What each {@link Flavor} accepts from the repository configuration.
 */
public final class FlavorRules {

    public static final String DATA_STREAM_INDEX_MODE = "data_stream";
    private static final String INDEX_MODE_SETTING = "analytics.elasticsearch.index_mode";
    private static final List<String> INDEX_MODES_MANAGED_BY_SERVERLESS = List.of("ilm", "daily");

    private FlavorRules() {}

    /**
     * @return why the configuration cannot work with the flavor, naming the offending setting, or empty when it can
     */
    public static Optional<String> violation(Flavor flavor, RepositoryConfiguration configuration) {
        final Optional<String> indexMode = configuration.getConfiguredIndexMode();
        if (flavor == Flavor.CLASSIC) {
            return indexMode
                .filter(DATA_STREAM_INDEX_MODE::equalsIgnoreCase)
                .map(mode ->
                    format(
                        "'%s' is set to '%s', which is only supported on Elastic Cloud Serverless: set it to 'daily' or 'ilm'.",
                        INDEX_MODE_SETTING,
                        mode
                    )
                );
        }
        return indexMode
            .filter(mode -> INDEX_MODES_MANAGED_BY_SERVERLESS.stream().anyMatch(mode::equalsIgnoreCase))
            .map(mode ->
                format(
                    "'%s' is set to '%s', which Elastic Cloud Serverless does not support as it manages indices itself: remove it or set it to '%s'.",
                    INDEX_MODE_SETTING,
                    mode,
                    DATA_STREAM_INDEX_MODE
                )
            );
    }
}
