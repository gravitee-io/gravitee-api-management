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
package io.gravitee.reporter.elasticsearch.factory;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.factory.es7.Elastic7xBeanFactory;
import io.gravitee.reporter.elasticsearch.factory.es8.Elastic8xBeanFactory;
import io.gravitee.reporter.elasticsearch.factory.es9.Elastic9xBeanFactory;
import io.gravitee.reporter.elasticsearch.factory.opensearch.OpenSearchBeanFactory;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class BeanFactoryBuilder {

    private static final Set<Integer> SUPPORTED_OPENSEARCH_MAJOR_VERSIONS = Set.of(1, 2, 3);

    public static BeanFactory buildFactory(final Client client) {
        ElasticsearchInfo info = retrieveElasticSearchInfo(client);
        BeanFactory beanFactory = getBeanFactoryFromElasticsearchInfo(info);
        if (beanFactory == null) {
            log.error(
                "{} version {} is not supported by this connector",
                info.getVersion().isOpenSearch() ? "OpenSearch" : "ElasticSearch",
                info.getVersion().getNumber()
            );
            return null;
        }
        return beanFactory;
    }

    private static ElasticsearchInfo retrieveElasticSearchInfo(final Client client) {
        // Wait for a connection to ES and retry each 5 seconds
        return client
            .getInfo()
            .retryWhen(error ->
                error.flatMap(throwable -> Observable.just(new Object()).delay(5, TimeUnit.SECONDS).toFlowable(BackpressureStrategy.LATEST))
            )
            .blockingGet();
    }

    private static BeanFactory getBeanFactoryFromElasticsearchInfo(ElasticsearchInfo elasticsearchInfo) {
        if (elasticsearchInfo.getVersion().isOpenSearch()) {
            if (SUPPORTED_OPENSEARCH_MAJOR_VERSIONS.contains(elasticsearchInfo.getVersion().getMajorVersion())) {
                return new OpenSearchBeanFactory();
            }
            return null;
        }

        return switch (elasticsearchInfo.getVersion().getMajorVersion()) {
            case 7 -> new Elastic7xBeanFactory();
            case 8 -> new Elastic8xBeanFactory();
            case 9 -> new Elastic9xBeanFactory();
            default -> null;
        };
    }
}
