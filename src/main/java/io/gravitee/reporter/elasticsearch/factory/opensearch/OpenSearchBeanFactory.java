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
package io.gravitee.reporter.elasticsearch.factory.opensearch;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.factory.BeanFactory;
import io.gravitee.reporter.elasticsearch.indexer.IndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class OpenSearchBeanFactory implements BeanFactory {

    @Override
    public IndexNameGenerator createIndexNameGenerator(final ReporterConfiguration configuration) {
        return configuration.isIlmManagedIndex()
            ? new PerTypeIndexNameGenerator(configuration)
            : new PerTypeAndDateIndexNameGenerator(configuration);
    }

    @Override
    public FormatterFactoryConfiguration createFormatterFactoryConfiguration() {
        return FormatterFactoryConfiguration.builder().elasticSearchVersion(7).build();
    }

    @Override
    public IndexPreparer createIndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client
    ) {
        return new ES7IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client);
    }
}
