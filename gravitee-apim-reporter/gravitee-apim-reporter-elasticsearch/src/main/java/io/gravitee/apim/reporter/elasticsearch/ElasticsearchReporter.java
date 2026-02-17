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
package io.gravitee.apim.reporter.elasticsearch;

import io.gravitee.apim.reporter.common.MetricsType;
import io.gravitee.apim.reporter.common.bulk.BulkProcessor;
import io.gravitee.apim.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.apim.reporter.common.bulk.compressor.NoneBulkCompressor;
import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.apim.reporter.elasticsearch.bulk.ElasticBulkSender;
import io.gravitee.apim.reporter.elasticsearch.bulk.ElasticBulkTransformer;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.factory.BeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.BeanFactoryBuilder;
import io.gravitee.apim.reporter.elasticsearch.mapping.IndexPreparer;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import java.util.HashSet;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public class ElasticsearchReporter extends AbstractService<Reporter> implements Reporter {

    private final Node node;
    private final ReporterConfiguration reporterConfiguration;
    private final PipelineConfiguration pipelineConfiguration;
    private final FreeMarkerComponent freeMarkerComponent;
    private final Client client;

    private final Set<Class<? extends Reportable>> acceptableReportables = new HashSet<>();
    private BulkProcessor bulkProcessor;

    @Override
    protected void doStart() throws Exception {
        if (reporterConfiguration.isEnabled()) {
            super.doStart();
            log.info("Starting Elastic reporter engine...");

            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);
            if (beanFactory == null) {
                log.info("Starting Elastic reporter engine... ERROR");
                return;
            }

            // Initialize available reportable classes
            for (MetricsType type : MetricsType.values()) {
                acceptableReportables.add(type.getClazz());
            }

            IndexPreparer preparer = beanFactory.createIndexPreparer(
                reporterConfiguration,
                pipelineConfiguration,
                freeMarkerComponent,
                client
            );
            preparer
                .prepare()
                .doOnComplete(() -> {
                    log.debug("Index mapping template successfully defined");
                    log.info("Starting Elastic reporter engine... DONE");
                })
                .doOnError(throwable -> {
                    log.warn("An error occurs while creating index mapping template", throwable);
                    log.error("Starting Elastic reporter engine... ERROR");
                })
                .subscribe();

            FormatterFactoryConfiguration formatterFactoryConfiguration = beanFactory.createFormatterFactoryConfiguration();
            bulkProcessor = new BulkProcessor(
                new ElasticBulkSender(client),
                reporterConfiguration.getBulkConfiguration(),
                new ElasticBulkTransformer(
                    new FormatterFactory(node, formatterFactoryConfiguration).getFormatter(Type.ELASTICSEARCH),
                    pipelineConfiguration,
                    beanFactory.createIndexNameGenerator(reporterConfiguration)
                ),
                new NoneBulkCompressor(),
                new BulkDropper()
            );
            bulkProcessor.start();
        }
    }

    @Override
    public void report(Reportable reportable) {
        if (reporterConfiguration.isEnabled()) {
            bulkProcessor.process(reportable);
        }
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return acceptableReportables.contains(reportable.getClass());
    }

    @Override
    protected void doStop() throws Exception {
        if (reporterConfiguration.isEnabled()) {
            super.doStop();
            log.info("Stopping Elastic reporter engine... DONE");
        }
    }
}
