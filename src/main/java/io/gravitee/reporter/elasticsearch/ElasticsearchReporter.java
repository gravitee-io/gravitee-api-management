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
package io.gravitee.reporter.elasticsearch;

import io.gravitee.common.service.AbstractService;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.Indexer;
import io.gravitee.reporter.elasticsearch.mapping.IndexPreparer;
import io.gravitee.reporter.elasticsearch.spring.context.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchReporter extends AbstractService implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchReporter.class);

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    /**
     * Indexer is settled in a lazy way as soon as the ES version has been discovered.
     */
    private Indexer indexer;

    @Override
    protected void doStart() throws Exception {
        if (configuration.isEnabled()) {
            super.doStart();
            LOGGER.info("Starting Elastic reporter engine...");

            ElasticsearchInfo elasticsearchInfo = retrieveElasticSearchInfo();
            AbstractElasticBeanRegistrer elasticsearchBeanRegister = getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);
            if (elasticsearchBeanRegister == null) {
                LOGGER.error(
                    "{} version {} is not supported by this connector",
                    elasticsearchInfo.getVersion().isOpenSearch() ? "OpenSearch" : "ElasticSearch",
                    elasticsearchInfo.getVersion().getNumber()
                );
                LOGGER.info("Starting Elastic reporter engine... ERROR");
                return;
            }

            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
            elasticsearchBeanRegister.register(beanFactory, configuration);

            IndexPreparer preparer = applicationContext.getBean(IndexPreparer.class);
            preparer
                .prepare()
                .doOnComplete(() -> {
                    LOGGER.info("Starting Elastic reporter engine... DONE");
                })
                .subscribe(
                    new CompletableObserver() {
                        @Override
                        public void onSubscribe(Disposable d) {}

                        @Override
                        public void onComplete() {
                            LOGGER.info("Index mapping template successfully defined");
                        }

                        @Override
                        public void onError(Throwable t) {
                            LOGGER.error("An error occurs while creating index mapping template", t);
                        }
                    }
                );

            indexer = applicationContext.getBean(Indexer.class);
        }
    }

    @Override
    public void report(Reportable reportable) {
        if (configuration.isEnabled()) {
            indexer.index(reportable);
        }
    }

    Single rxReport(Reportable reportable) {
        indexer.index(reportable);
        return Single.just(reportable);
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return (
            reportable instanceof Metrics ||
            reportable instanceof EndpointStatus ||
            reportable instanceof Monitor ||
            reportable instanceof Log
        );
    }

    @Override
    protected void doStop() throws Exception {
        if (configuration.isEnabled()) {
            super.doStop();
            LOGGER.info("Stopping Elastic reporter engine... DONE");
        }
    }

    private ElasticsearchInfo retrieveElasticSearchInfo() {
        // Wait for a connection to ES and retry each 5 seconds
        Single<ElasticsearchInfo> elasticsearchInfoSingle = client
            .getInfo()
            .retryWhen(error ->
                error.flatMap(throwable -> Observable.just(new Object()).delay(5, TimeUnit.SECONDS).toFlowable(BackpressureStrategy.LATEST))
            );
        elasticsearchInfoSingle.subscribe();
        return elasticsearchInfoSingle.blockingGet();
    }

    protected AbstractElasticBeanRegistrer getBeanRegistrerFromElasticsearchInfo(ElasticsearchInfo elasticsearchInfo) {
        if (elasticsearchInfo.getVersion().isOpenSearch()) {
            if (elasticsearchInfo.getVersion().getMajorVersion() == 1) {
                return new OpenSearchBeanRegistrer();
            }
            return null;
        }

        switch (elasticsearchInfo.getVersion().getMajorVersion()) {
            case 5:
                return new Elastic5xBeanRegistrer();
            case 6:
                return new Elastic6xBeanRegistrer();
            case 7:
                return new Elastic7xBeanRegistrer();
            default:
                return null;
        }
    }
}
