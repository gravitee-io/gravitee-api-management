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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;

/**
 * Puts one data stream template per type, named and matching exactly {@code <prefix>-<type>}, on every classic cluster:
 * the same mappings as the classic templates, from {@code common/mapping/}, and no rollover alias.
 */
@CustomLog
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

    /**
     * An alias named after a data stream, as the 'ilm' index mode leaves, takes the documents: Elasticsearch writes them to the
     * indices of the alias and never creates the data stream.
     */
    @Override
    public Completable prepare() {
        return aliasesNamedAfterTheDataStreams().flatMapCompletable(aliases ->
            aliases.isEmpty() ? super.prepare() : Completable.error(new IllegalStateException(aliasConflict(aliases)))
        );
    }

    private Single<List<String>> aliasesNamedAfterTheDataStreams() {
        return Flowable.fromArray(Type.TYPES)
            .map(type -> configuration.getIndexName() + '-' + type.getType())
            .concatMapMaybe(name ->
                client
                    .getAlias(name)
                    .map(alias -> name)
                    .onErrorResumeNext(error -> aliasUnreadable(name, error))
            )
            .toList();
    }

    private Maybe<String> aliasUnreadable(String name, Throwable error) {
        log.warn("Unable to check that no alias is named [{}] like its data stream: {}", name, error.getMessage());
        return Maybe.empty();
    }

    private static String aliasConflict(List<String> aliases) {
        return String.format(
            "'reporters.elasticsearch.index_mode' is 'data_stream', but the aliases %s have the names of its data streams, " +
                "most likely left by the 'ilm' index mode: Elasticsearch would write the documents to their indices instead. " +
                "Once no gateway writes in 'ilm' mode, set 'index.lifecycle.indexing_complete' to true on the write index of " +
                "each alias and remove the aliases, or set 'reporters.elasticsearch.index' to another prefix",
            aliases
        );
    }

    @Override
    public String generateIndexTemplate(Type type) {
        final Map<String, Object> data = getTemplateData();
        data.put("indexName", configuration.getIndexName() + '-' + type.getType());
        data.put("mapping", "/common/mapping/" + type.getType() + "-mapping.ftl");
        data.put("bodyAnalysis", TYPES_WITH_BODIES.contains(type));
        data.put("extendedSettings", !TYPES_WITHOUT_EXTENDED_SETTINGS.contains(type));
        Optional.ofNullable(lifecycle.policyPropertyName())
            .flatMap(property -> configuration.lifecyclePolicy(type))
            .ifPresent(name -> {
                data.put("lifecyclePolicyPropertyName", lifecycle.policyPropertyName());
                data.put("lifecyclePolicy", name);
            });
        if (lifecycle.dataRetentionSupported()) {
            configuration.dataRetention(type).ifPresent(retention -> data.put("dataRetention", retention));
        }
        return freeMarkerComponent.generateFromTemplate(TEMPLATE, data);
    }
}
