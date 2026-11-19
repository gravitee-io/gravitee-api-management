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
package io.gravitee.apim.reporter.elasticsearch.factory;

import io.gravitee.apim.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.apim.reporter.common.formatter.elasticsearch.BulkOperation;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ServerlessRetention;
import io.gravitee.apim.reporter.elasticsearch.indexer.IndexNameGenerator;
import io.gravitee.apim.reporter.elasticsearch.indexer.PerTypeIndexNameGenerator;
import io.gravitee.apim.reporter.elasticsearch.mapping.IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.serverless.ServerlessIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.common.util.Version;
import io.gravitee.elasticsearch.client.Client;

/**
 * Elastic Cloud Serverless: documents are created in the data stream {@code <prefix>-<type>}, with the document format of
 * the version the project reports, and each type gets a serverless template carrying its retention and the APIM version of the gateway.
 */
public class ServerlessBeanFactory implements BeanFactory {

    private final BeanFactory clusterBeanFactory;
    private final ServerlessRetention retention;

    public ServerlessBeanFactory(BeanFactory clusterBeanFactory, ServerlessRetention retention) {
        this.clusterBeanFactory = clusterBeanFactory;
        this.retention = retention;
    }

    @Override
    public IndexNameGenerator createIndexNameGenerator(final ReporterConfiguration configuration) {
        return new PerTypeIndexNameGenerator(configuration);
    }

    @Override
    public FormatterFactoryConfiguration createFormatterFactoryConfiguration() {
        return clusterBeanFactory.createFormatterFactoryConfiguration().toBuilder().bulkOperation(BulkOperation.CREATE).build();
    }

    @Override
    public IndexPreparer createIndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client
    ) {
        return new ServerlessIndexPreparer(
            configuration,
            pipelineConfiguration,
            freeMarkerComponent,
            client,
            retention,
            Version.RUNTIME_VERSION.MAJOR_VERSION
        );
    }
}
