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
package io.gravitee.reporter.elasticsearch.mapping.es6;

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.mapping.PerTypeIndexPreparer;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.functions.Function;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ES6IndexPreparer extends PerTypeIndexPreparer {

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

            logger.debug("Trying to put template mapping for type[{}] name[{}]", typeName, templateName);

            Map<String, Object> data = getTemplateData();
            data.put("indexName", configuration.getIndexName() + '-' + typeName);

            final String template = freeMarkerComponent.generateFromTemplate("/es6x/mapping/index-template-" + typeName + ".ftl", data);

            return client.putTemplate(templateName, template);
        };
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
