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

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.elasticsearch.version.Version;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * What the data stream index mode needs from a classic cluster, and what it reports about the lifecycle of the data streams.
 */
public final class DataStreamRules {

    private DataStreamRules() {}

    /**
     * @return why the cluster cannot write the data streams as configured, naming the offending setting, or empty when it can
     */
    public static Optional<String> violation(Version version, ReporterConfiguration configuration) {
        if (!version.canUseDataStreams()) {
            return Optional.of(
                format(
                    "'%s' is set to '%s', which needs Elasticsearch 7.9 or later: the cluster runs %s. Set it to 'daily' or 'ilm', or upgrade the cluster.",
                    ReporterConfiguration.INDEX_MODE_SETTING,
                    ReporterConfiguration.DATA_STREAM_INDEX_MODE,
                    version.getNumber()
                )
            );
        }
        if (version.isOpenSearch()) {
            return openSearchViolation(configuration);
        }
        final Optional<String> bothLifecycles = Arrays.stream(LifecycleGroup.values())
            .filter(group -> configuration.lifecyclePolicy(group).isPresent() && configuration.dataRetention(group).isPresent())
            .findFirst()
            .map(group -> bothLifecycles(configuration, group));
        if (bothLifecycles.isPresent() || version.canUseDataStreamLifecycle()) {
            return bothLifecycles;
        }
        return Arrays.stream(LifecycleGroup.values())
            .filter(group -> configuration.dataRetention(group).isPresent())
            .findFirst()
            .map(group -> unsupportedRetention(version, group));
    }

    /**
     * ISM ignores the policy_id index setting and OpenSearch has no data stream lifecycle: neither setting would apply.
     */
    private static Optional<String> openSearchViolation(ReporterConfiguration configuration) {
        for (LifecycleGroup group : LifecycleGroup.values()) {
            final String setting;
            if (configuration.lifecyclePolicy(group).isPresent()) {
                setting = group.policySetting();
            } else if (configuration.dataRetention(group).isPresent()) {
                setting = group.retentionSetting();
            } else {
                continue;
            }
            return Optional.of(
                format(
                    "'%s' is set, which OpenSearch does not apply to data streams: remove it and give the ISM policy an 'ism_template' whose 'index_patterns' match the data streams %s.",
                    setting,
                    dataStreams(configuration, List.of(group))
                )
            );
        }
        return Optional.empty();
    }

    private static String bothLifecycles(ReporterConfiguration configuration, LifecycleGroup group) {
        return format(
            "'%s' and '%s' are both set, while the data streams %s take one lifecycle: keep either the ILM policy or the retention.",
            group.policySetting(),
            group.retentionSetting(),
            dataStreams(configuration, List.of(group))
        );
    }

    private static String unsupportedRetention(Version version, LifecycleGroup group) {
        return format(
            "'%s' is set, which needs the data stream lifecycle of Elasticsearch 8.14 or later: the cluster runs %s. Remove it and set '%s' to an ILM policy instead.",
            group.retentionSetting(),
            version.getNumber(),
            group.policySetting()
        );
    }

    private static List<String> dataStreams(ReporterConfiguration configuration, List<LifecycleGroup> groups) {
        return groups
            .stream()
            .flatMap(group -> Arrays.stream(Type.values()).filter(type -> LifecycleGroup.of(type) == group))
            .map(type -> configuration.getIndexName() + '-' + type.getType())
            .toList();
    }

    /**
     * @return the lifecycle settings the cluster will not apply as the operator may expect; call it once {@link #violation} is empty
     */
    public static List<String> warnings(Version version, ReporterConfiguration configuration) {
        if (version.isOpenSearch()) {
            return List.of(
                format(
                    "The reporter attaches no lifecycle to the data streams on OpenSearch: their documents are never deleted unless an ISM policy has an 'ism_template' whose 'index_patterns' match the data streams %s.",
                    dataStreams(configuration, List.of(LifecycleGroup.values()))
                )
            );
        }
        final List<LifecycleGroup> keptForever = Arrays.stream(LifecycleGroup.values())
            .filter(group -> configuration.lifecyclePolicy(group).isEmpty() && configuration.dataRetention(group).isEmpty())
            .toList();
        if (keptForever.isEmpty()) {
            return List.of();
        }
        return List.of(
            format(
                "No lifecycle policy%s is set for the data streams %s: their documents are never deleted. Set '%s<key>'%s for the keys %s.",
                version.canUseDataStreamLifecycle() ? " nor retention" : "",
                dataStreams(configuration, keptForever),
                LifecycleGroup.POLICY_SETTING_PREFIX,
                version.canUseDataStreamLifecycle() ? format(" or '%s<key>'", LifecycleGroup.RETENTION_SETTING_PREFIX) : "",
                keptForever.stream().map(LifecycleGroup::key).toList()
            )
        );
    }
}
