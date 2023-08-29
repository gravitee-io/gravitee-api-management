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
package io.gravitee.reporter.elasticsearch.indexer.es5;

import io.gravitee.node.api.Node;
import io.gravitee.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.name.IndexNameGenerator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ES5BulkIndexer extends BulkIndexer {

    protected ES5BulkIndexer(
        ReporterConfiguration configuration,
        PipelineConfiguration pipelineConfiguration,
        IndexNameGenerator indexNameGenerator,
        Node node
    ) {
        super(configuration, pipelineConfiguration, indexNameGenerator, node);
    }

    @Override
    protected FormatterFactoryConfiguration formatterFactoryConfiguration() {
        return FormatterFactoryConfiguration.builder().elasticSearchVersion(5).build();
    }
}
