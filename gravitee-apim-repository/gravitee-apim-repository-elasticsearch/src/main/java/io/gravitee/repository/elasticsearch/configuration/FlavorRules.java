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

import java.util.Optional;

/**
 * What each {@link Flavor} accepts from the repository configuration.
 */
public final class FlavorRules {

    private static final String INDEX_MODE_SETTING = "analytics.elasticsearch.index_mode";

    private FlavorRules() {}

    /**
     * @return what the flavor does not take from the configuration, naming the setting, or empty when it takes all of it
     */
    public static Optional<String> violation(Flavor flavor, RepositoryConfiguration configuration) {
        if (flavor == Flavor.CLASSIC) {
            return Optional.empty();
        }
        return configuration
            .getConfiguredIndexMode()
            .filter(mode -> IndexMode.of(mode) != IndexMode.DATA_STREAM)
            .map(mode ->
                format(
                    "'%s' is set to '%s', which is ignored on Elastic Cloud Serverless: analytics are read from data streams. Remove it or set it to '%s'.",
                    INDEX_MODE_SETTING,
                    mode,
                    IndexMode.DATA_STREAM.value()
                )
            );
    }

    /**
     * @return the index mode analytics are read with: always data streams on Serverless, which manages indices itself
     */
    public static IndexMode indexMode(Flavor flavor, RepositoryConfiguration configuration) {
        if (flavor == Flavor.SERVERLESS) {
            return IndexMode.DATA_STREAM;
        }
        return configuration.getConfiguredIndexMode().map(IndexMode::of).orElse(IndexMode.DAILY);
    }
}
