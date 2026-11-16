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

import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.utils.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Puts one data stream template per type, named and matching exactly {@code <prefix>-<type>}, on every classic cluster:
 * the same mappings as the classic templates, from {@code common/mapping/}, and no rollover alias.
 */
public class DataStreamIndexPreparer extends AbstractIndexPreparer {

    private static final String TEMPLATE = "/data-stream/index-template.ftl";
    private static final Set<Type> TYPES_WITH_BODIES = Set.of(Type.LOG, Type.V4_LOG, Type.V4_MESSAGE_LOG);
    private static final Set<Type> TYPES_WITHOUT_EXTENDED_SETTINGS = Set.of(Type.EVENT_METRICS, Type.DECISIONS);

    private final DataStreamLifecycle lifecycle;

    public DataStreamIndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client,
        final DataStreamLifecycle lifecycle
    ) {
        super(configuration, pipelineConfiguration, freeMarkerComponent, client, "/data-stream");
        this.lifecycle = lifecycle;
    }

    @Override
    public String generateIndexTemplate(Type type) {
        final Map<String, Object> data = getTemplateData();
        data.put("indexName", configuration.getIndexName() + '-' + type.getType());
        data.put("mapping", "/common/mapping/" + type.getType() + "-mapping.ftl");
        data.put("bodyAnalysis", TYPES_WITH_BODIES.contains(type));
        data.put("extendedSettings", !TYPES_WITHOUT_EXTENDED_SETTINGS.contains(type));
        data.put("lifecyclePolicyPropertyName", lifecycle.policyPropertyName());

        final Optional<String> policy = configuration.lifecyclePolicy(type);
        policy.ifPresent(name -> data.put("lifecyclePolicy", name));
        // With both, Elasticsearch applies the ILM policy and ignores the retention.
        if (lifecycle.dataRetentionSupported() && policy.isEmpty()) {
            configuration.dataRetention(type).ifPresent(retention -> data.put("dataRetention", retention));
        }
        return freeMarkerComponent.generateFromTemplate(TEMPLATE, data);
    }
}
