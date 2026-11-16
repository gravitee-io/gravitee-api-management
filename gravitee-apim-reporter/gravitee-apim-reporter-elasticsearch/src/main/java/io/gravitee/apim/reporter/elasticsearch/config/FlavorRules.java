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
package io.gravitee.apim.reporter.elasticsearch.config;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * What each {@link Flavor} accepts from the reporter configuration, and how it fills in what is left unset.
 */
public final class FlavorRules {

    private static final List<String> INDEX_MODES_MANAGED_BY_SERVERLESS = List.of("ilm", "daily");

    private FlavorRules() {}

    /**
     * @return why the configuration cannot work with the flavor, naming the offending setting, or empty when it can
     */
    public static Optional<String> violation(Flavor flavor, ReporterConfiguration configuration) {
        final Optional<String> indexMode = configuration.getConfiguredIndexMode();
        if (flavor == Flavor.CLASSIC) {
            return Optional.empty();
        }
        final Optional<String> managedIndexMode = indexMode.filter(mode ->
            INDEX_MODES_MANAGED_BY_SERVERLESS.stream().anyMatch(mode::equalsIgnoreCase)
        );
        if (managedIndexMode.isPresent()) {
            return Optional.of(
                format(
                    "'%s' is set to '%s', which Elastic Cloud Serverless does not support as it manages indices itself: remove it or set it to '%s'.",
                    ReporterConfiguration.INDEX_MODE_SETTING,
                    managedIndexMode.get(),
                    ReporterConfiguration.DATA_STREAM_INDEX_MODE
                )
            );
        }
        return Arrays.stream(LifecycleGroup.values())
            .filter(group -> configuration.lifecyclePolicy(group).isPresent())
            .findFirst()
            .map(group ->
                format(
                    "'%s' is set, which Elastic Cloud Serverless does not support as it manages data retention itself: remove it.",
                    group.policySetting()
                )
            );
    }

    public static String indexMode(Flavor flavor, ReporterConfiguration configuration) {
        return configuration
            .getConfiguredIndexMode()
            .orElse(flavor == Flavor.SERVERLESS ? ReporterConfiguration.DATA_STREAM_INDEX_MODE : ReporterConfiguration.DEFAULT_INDEX_MODE);
    }

    public static String disabledFeatures(Flavor flavor) {
        return flavor == Flavor.SERVERLESS
            ? "index settings (number_of_shards, number_of_replicas, refresh_interval), lifecycle policies"
            : "none";
    }
}
