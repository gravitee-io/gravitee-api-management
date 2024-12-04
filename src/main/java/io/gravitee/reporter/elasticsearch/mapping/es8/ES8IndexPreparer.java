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
package io.gravitee.reporter.elasticsearch.mapping.es8;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.functions.Function;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ES8IndexPreparer extends AbstractIndexPreparer {

    public ES8IndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client
    ) {
        super(configuration, pipelineConfiguration, freeMarkerComponent, client);
    }

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

            log.debug("Trying to put template mapping for type[{}] name[{}]", typeName, templateName);

            Map<String, Object> data = getTemplateData();
            data.put("indexName", configuration.getIndexName() + '-' + typeName);

            final String template = freeMarkerComponent.generateFromTemplate("/es8x/mapping/index-template-" + typeName + ".ftl", data);

            final Completable templateCreationCompletable = client.putIndexTemplate(templateName, template);
            if (configuration.isIlmManagedIndex()) {
                return templateCreationCompletable.andThen(ensureAlias(aliasName));
            }
            return templateCreationCompletable;
        };
    }

    private Completable ensureAlias(String aliasName) {
        final String aliasTemplate = freeMarkerComponent.generateFromTemplate(
            "/es8x/alias/alias.ftl",
            Collections.singletonMap("aliasName", aliasName)
        );

        return client
            .getAlias(aliasName)
            .switchIfEmpty(client.createIndexWithAlias(aliasName + "-000001", aliasTemplate).toMaybe())
            .ignoreElement();
    }
}
