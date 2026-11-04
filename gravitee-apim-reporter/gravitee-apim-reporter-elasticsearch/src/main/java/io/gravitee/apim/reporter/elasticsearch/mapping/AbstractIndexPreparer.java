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
package io.gravitee.apim.reporter.elasticsearch.mapping;

import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.utils.Type;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.functions.Function;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public abstract class AbstractIndexPreparer implements IndexPreparer {

    protected final ReporterConfiguration configuration;
    protected final PipelineConfiguration pipelineConfiguration;

    protected final FreeMarkerComponent freeMarkerComponent;

    protected final Client client;

    @Getter
    protected final String templatePathPrefix;

    private static final String TEMPLATE_PATH = "/mapping/index-template-";
    private static final String FTL_EXTENSION = ".ftl";

    protected Completable indexMapping() {
        return Completable.merge(Flowable.fromArray(Type.TYPES).map(indexTypeMapper()));
    }

    /**
     * Index mapping for a single {@link Type}.
     */
    protected Function<Type, CompletableSource> indexTypeMapper() {
        return type -> {
            final String typeName = type.getType();
            boolean dataStream = type.isDataStream();
            final String templateName = configuration.getIndexName() + '-' + typeName;
            final String aliasName = configuration.getIndexName() + '-' + typeName;

            log.debug("Trying to put template mapping for type[{}] name[{}]", typeName, templateName);
            final String template = generateIndexTemplate(type);

            final Completable templateCreationCompletable = (useOldClient(dataStream)
                    ? client.putTemplate(templateName, template)
                    : client.putIndexTemplate(templateName, template)).onErrorResumeNext(throwable ->
                Completable.error(new IndexPreparationException("the index template [" + templateName + "]", throwable))
            );

            if (configuration.isIlmManagedIndex() && !dataStream) {
                return templateCreationCompletable.andThen(ensureAlias(aliasName));
            }
            return templateCreationCompletable;
        };
    }

    /**
     * The pipeline only enriches documents (geoip, user agent): when it cannot be created, documents are indexed without it.
     */
    protected Completable pipeline() {
        final String pipelineName = pipelineConfiguration.getPipelineName();
        String pipelineTemplate = pipelineConfiguration.createPipeline();

        if (pipelineTemplate != null && pipelineName != null) {
            return client
                .putPipeline(pipelineName, pipelineTemplate)
                .doOnComplete(pipelineConfiguration::valid)
                .onErrorComplete(throwable -> {
                    log.warn(
                        "Unable to create the ingest pipeline [{}]: documents are indexed without the ingest pipeline, so they carry no geoip or user agent fields. Cause: {}",
                        pipelineName,
                        throwable.getMessage()
                    );
                    return true;
                });
        }

        log.warn(
            "No ingest plugin is configured in 'reporters.elasticsearch.pipeline.plugins.ingest': documents are indexed without the ingest pipeline [{}]",
            pipelineName
        );
        return Completable.complete();
    }

    public String generateIndexTemplate(Type type) {
        final String typeName = type.getType();
        Map<String, Object> data = getTemplateData();
        data.put("indexName", configuration.getIndexName() + '-' + typeName);

        return freeMarkerComponent.generateFromTemplate(templatePathPrefix + TEMPLATE_PATH + typeName + FTL_EXTENSION, data);
    }

    protected Map<String, Object> getTemplateData() {
        final Map<String, Object> data = new HashMap<>();

        data.put("indexName", this.configuration.getIndexName());
        data.put("numberOfShards", this.configuration.getNumberOfShards());
        data.put("numberOfReplicas", this.configuration.getNumberOfReplicas());
        data.put("refreshInterval", this.configuration.getRefreshInterval());
        data.put("indexBody", this.configuration.isIndexBody());
        data.put("indexLifecyclePolicyPropertyName", this.configuration.getIndexLifecyclePolicyPropertyName());
        data.put("indexLifecycleRolloverAliasPropertyName", this.configuration.getIndexLifecycleRolloverAliasPropertyName());
        data.put("indexLifecyclePolicyHealth", this.configuration.getIndexLifecyclePolicyHealth());
        data.put("indexLifecyclePolicyMonitor", this.configuration.getIndexLifecyclePolicyMonitor());
        data.put("indexLifecyclePolicyRequest", this.configuration.getIndexLifecyclePolicyRequest());
        data.put("indexLifecyclePolicyLog", this.configuration.getIndexLifecyclePolicyLog());
        data.put("indexLifecyclePolicyEventMetrics", this.configuration.getIndexLifecyclePolicyEventMetrics());
        data.put("extendedRequestMappingTemplate", this.configuration.getExtendedRequestMappingTemplate());
        data.put("extendedSettingsTemplate", this.configuration.getExtendedSettingsTemplate());
        return data;
    }

    protected Completable ensureAlias(String aliasName) {
        final String aliasTemplate = freeMarkerComponent.generateFromTemplate(
            "/common/alias/alias.ftl",
            Collections.singletonMap("aliasName", aliasName)
        );

        final String initialIndexName = aliasName + "-000001";
        return client
            .getAlias(aliasName)
            .switchIfEmpty(
                client
                    .createIndexWithAlias(initialIndexName, aliasTemplate)
                    .onErrorResumeNext(throwable ->
                        isBadRequest(throwable) ? aliasCreatedMeanwhile(aliasName, throwable) : Completable.error(throwable)
                    )
                    .toMaybe()
            )
            .ignoreElement()
            .onErrorResumeNext(throwable ->
                Completable.error(
                    new IndexPreparationException(
                        "the alias [" + aliasName + "] and its initial index [" + initialIndexName + "]",
                        throwable
                    )
                )
            );
    }

    /**
     * Gateways starting together all find the alias missing: the slower ones get a 400 resource_already_exists_exception
     * while the alias they need is in place.
     */
    private Completable aliasCreatedMeanwhile(String aliasName, Throwable creationError) {
        return client
            .getAlias(aliasName)
            .doOnSuccess(alias -> log.info("Alias [{}] was created meanwhile, probably by another gateway", aliasName))
            .switchIfEmpty(Maybe.error(creationError))
            .ignoreElement();
    }

    private static boolean isBadRequest(Throwable throwable) {
        return (
            throwable instanceof ElasticsearchException elasticsearchException &&
            Integer.valueOf(HttpStatusCode.BAD_REQUEST_400).equals(elasticsearchException.getStatusCode())
        );
    }

    protected boolean useOldClient(boolean dataStream) {
        return false;
    }

    public Completable prepare() {
        return indexMapping().andThen(pipeline());
    }
}
