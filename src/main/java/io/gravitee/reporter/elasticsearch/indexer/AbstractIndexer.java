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
package io.gravitee.reporter.elasticsearch.indexer;

import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.formatter.Formatter;
import io.gravitee.reporter.common.formatter.FormatterFactory;
import io.gravitee.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.reporter.common.formatter.Type;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.name.IndexNameGenerator;
import io.vertx.core.buffer.Buffer;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractIndexer implements Indexer {

    private static final String INDEX_OPTION = "index";
    private static final String PIPELINE_OPTION = "pipeline";

    /**
     * Reporter configuration.
     */
    protected final ReporterConfiguration configuration;

    /**
     * Pipeline configuration.
     */
    private final PipelineConfiguration pipelineConfiguration;

    private final IndexNameGenerator indexNameGenerator;

    private final Formatter<Reportable> formatter;

    protected AbstractIndexer(
        ReporterConfiguration configuration,
        PipelineConfiguration pipelineConfiguration,
        IndexNameGenerator indexNameGenerator,
        Node node
    ) {
        this.configuration = configuration;
        this.pipelineConfiguration = pipelineConfiguration;
        this.indexNameGenerator = indexNameGenerator;
        this.formatter = new FormatterFactory(node, formatterFactoryConfiguration()).getFormatter(Type.ELASTICSEARCH);
    }

    protected Buffer transform(Reportable reportable) {
        Map<String, Object> options = Map.of(
            INDEX_OPTION,
            indexNameGenerator.generate(reportable),
            PIPELINE_OPTION,
            pipelineConfiguration.getPipeline()
        );
        return formatter.format(reportable, options);
    }

    protected abstract FormatterFactoryConfiguration formatterFactoryConfiguration();
}
