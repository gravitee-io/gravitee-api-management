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
package io.gravitee.reporter.elasticsearch;

import io.gravitee.common.service.AbstractService;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.reporter.common.MetricsType;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.Indexer;
import io.gravitee.reporter.elasticsearch.mapping.IndexPreparer;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchReporter extends AbstractService<Reporter> implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchReporter.class);

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    /**
     * Indexer is settled in a lazy way as soon as the ES version has been discovered.
     */
    private Indexer indexer;

    private final Set<Class<? extends Reportable>> acceptableReportables = new HashSet<>();

    @Override
    protected void doStart() throws Exception {
        if (configuration.isEnabled()) {
            super.doStart();
            LOGGER.info("Starting Elastic reporter engine...");

            var beanRegister = new BeanRegister(applicationContext);
            if (!beanRegister.registerBeans(retrieveElasticSearchInfo(), configuration)) {
                LOGGER.info("Starting Elastic reporter engine... ERROR");
                return;
            }

            // Initialize available reportable classes
            for (MetricsType type : MetricsType.values()) {
                acceptableReportables.add(type.getClazz());
            }

            IndexPreparer preparer = applicationContext.getBean(IndexPreparer.class);
            preparer
                .prepare()
                .doOnComplete(() -> LOGGER.info("Starting Elastic reporter engine... DONE"))
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

    Single<Reportable> rxReport(Reportable reportable) {
        indexer.index(reportable);
        return Single.just(reportable);
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return acceptableReportables.contains(reportable.getClass());
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
}
