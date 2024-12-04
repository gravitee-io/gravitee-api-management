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
package io.gravitee.reporter.elasticsearch.bulk;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.bulk.transformer.BulkFormatterTransformer;
import io.gravitee.reporter.common.formatter.Formatter;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.IndexNameGenerator;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticBulkTransformer extends BulkFormatterTransformer {

    private static final String INDEX_OPTION = "index";
    private static final String PIPELINE_OPTION = "pipeline";
    private final PipelineConfiguration pipelineConfiguration;
    private final IndexNameGenerator indexNameGenerator;

    public ElasticBulkTransformer(
        final Formatter<Reportable> formatter,
        final PipelineConfiguration pipelineConfiguration,
        final IndexNameGenerator indexNameGenerator
    ) {
        super(formatter);
        this.pipelineConfiguration = pipelineConfiguration;
        this.indexNameGenerator = indexNameGenerator;
    }

    @Override
    protected Map<String, Object> buildOptions(final Reportable reportable) {
        return Map.of(INDEX_OPTION, indexNameGenerator.generate(reportable), PIPELINE_OPTION, pipelineConfiguration.getPipeline());
    }
}
