/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elasticsearch.mapping.es7;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.mapping.PerTypeIndexPreparer;
import io.reactivex.*;
import io.reactivex.functions.Function;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ES7IndexPreparer extends PerTypeIndexPreparer {

    /**
     * Configuration of pipelineConfiguration
     */
    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    @Override
    public Completable prepare() {
        return indexMapping().andThen(pipeline());
    }

    @Override
    protected Function<Type, CompletableSource> indexTypeMapper() {
        return type -> {
            final String typeName = type.getType();
            final String templateName = configuration.getIndexName() + '-' + typeName;
            final String aliasName = configuration.getIndexName() + '-' + typeName;

            logger.debug("Trying to put template mapping for type[{}] name[{}]", typeName, templateName);

            Map<String, Object> data = getTemplateData();
            data.put("indexName", configuration.getIndexName() + '-' + typeName);

            final String template = freeMarkerComponent.generateFromTemplate("/es7x/mapping/index-template-" + typeName + ".ftl", data);

            final Completable templateCreationCompletable = client.putTemplate(templateName, template);
            if (configuration.isIlmManagedIndex()) {
                return templateCreationCompletable.andThen(ensureAlias(aliasName));
            }
            return templateCreationCompletable;
        };
    }

    private Completable ensureAlias(String aliasName) {
        final String aliasTemplate = freeMarkerComponent.generateFromTemplate(
            "/es7x/alias/alias.ftl",
            Collections.singletonMap("aliasName", aliasName)
        );

        return client
            .getAlias(aliasName)
            .switchIfEmpty(client.createIndexWithAlias(aliasName + "-000001", aliasTemplate).toMaybe())
            .ignoreElement();
    }

    private Completable pipeline() {
        String pipelineTemplate = pipelineConfiguration.createPipeline();

        if (pipelineTemplate != null && pipelineConfiguration.getPipelineName() != null) {
            return client
                .putPipeline(pipelineConfiguration.getPipelineName(), pipelineTemplate)
                .doOnComplete(() -> pipelineConfiguration.valid());
        }

        return Completable.complete();
    }
}
