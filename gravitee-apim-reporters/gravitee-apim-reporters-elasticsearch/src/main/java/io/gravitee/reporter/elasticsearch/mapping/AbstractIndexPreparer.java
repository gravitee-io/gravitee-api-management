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
package io.gravitee.reporter.elasticsearch.mapping;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractIndexPreparer implements IndexPreparer {

    protected final ReporterConfiguration configuration;
    protected final PipelineConfiguration pipelineConfiguration;

    protected final FreeMarkerComponent freeMarkerComponent;

    protected final Client client;

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

            Map<String, Object> data = getTemplateData();
            data.put("indexName", configuration.getIndexName() + '-' + typeName);

            final String template = freeMarkerComponent.generateFromTemplate(
                templatePathPrefix + TEMPLATE_PATH + typeName + FTL_EXTENSION,
                data
            );

            final Completable templateCreationCompletable = useOldClient(dataStream)
                ? client.putTemplate(templateName, template)
                : client.putIndexTemplate(templateName, template);

            if (configuration.isIlmManagedIndex() && !dataStream) {
                return templateCreationCompletable.andThen(ensureAlias(aliasName));
            }
            return templateCreationCompletable;
        };
    }

    protected Completable pipeline() {
        String pipelineTemplate = pipelineConfiguration.createPipeline();

        if (pipelineTemplate != null && pipelineConfiguration.getPipelineName() != null) {
            return client.putPipeline(pipelineConfiguration.getPipelineName(), pipelineTemplate).doOnComplete(pipelineConfiguration::valid);
        }

        return Completable.complete();
    }

    protected Map<String, Object> getTemplateData() {
        final Map<String, Object> data = new HashMap<>();

        data.put("indexName", this.configuration.getIndexName());
        data.put("numberOfShards", this.configuration.getNumberOfShards());
        data.put("numberOfReplicas", this.configuration.getNumberOfReplicas());
        data.put("refreshInterval", this.configuration.getRefreshInterval());
        data.put("indexLifecyclePolicyPropertyName", this.configuration.getIndexLifecyclePolicyPropertyName());
        data.put("indexLifecycleRolloverAliasPropertyName", this.configuration.getIndexLifecycleRolloverAliasPropertyName());
        data.put("indexLifecyclePolicyHealth", this.configuration.getIndexLifecyclePolicyHealth());
        data.put("indexLifecyclePolicyMonitor", this.configuration.getIndexLifecyclePolicyMonitor());
        data.put("indexLifecyclePolicyRequest", this.configuration.getIndexLifecyclePolicyRequest());
        data.put("indexLifecyclePolicyLog", this.configuration.getIndexLifecyclePolicyLog());
        data.put("extendedRequestMappingTemplate", this.configuration.getExtendedRequestMappingTemplate());
        data.put("extendedSettingsTemplate", this.configuration.getExtendedSettingsTemplate());
        return data;
    }

    protected Completable ensureAlias(String aliasName) {
        final String aliasTemplate = freeMarkerComponent.generateFromTemplate(
            "/common/alias/alias.ftl",
            Collections.singletonMap("aliasName", aliasName)
        );

        return client
            .getAlias(aliasName)
            .switchIfEmpty(client.createIndexWithAlias(aliasName + "-000001", aliasTemplate).toMaybe())
            .ignoreElement();
    }

    protected boolean useOldClient(boolean dataStream) {
        return false;
    }

    public Completable prepare() {
        return indexMapping().andThen(pipeline());
    }
}
