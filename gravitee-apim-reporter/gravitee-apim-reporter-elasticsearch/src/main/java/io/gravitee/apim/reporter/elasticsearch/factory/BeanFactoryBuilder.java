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

import io.gravitee.apim.reporter.elasticsearch.factory.es7.Elastic7xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.es8.Elastic8xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.es9.Elastic9xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.opensearch.OpenSearchBeanFactory;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
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
                error.flatMap(throwable -> {
                    if (isUnauthorized(throwable)) {
                        log.error(
                            "Elasticsearch authentication failed (401). Please verify your 'reporters.elasticsearch.security.username' and 'reporters.elasticsearch.security.password' configuration.",
                            throwable
                        );
                        return Flowable.error(throwable);
                    }
                    if (isForbidden(throwable)) {
                        log.error(
                            "Elasticsearch access denied (403). The configured user does not have sufficient permissions to access the Elasticsearch cluster. Please verify the user's roles and privileges.",
                            throwable
                        );
                        return Flowable.error(throwable);
                    }
                    log.warn("Unable to connect to Elasticsearch, retrying in 5 seconds. Cause: {}", throwable.getMessage());
                    return Observable.just(new Object()).delay(5, TimeUnit.SECONDS).toFlowable(BackpressureStrategy.LATEST);
                })
            )
            .blockingGet();
    }

    static boolean isUnauthorized(Throwable throwable) {
        var esException = findElasticsearchException(throwable);
        return esException != null && Integer.valueOf(401).equals(esException.getStatusCode());
    }

    static boolean isForbidden(Throwable throwable) {
        var esException = findElasticsearchException(throwable);
        return esException != null && Integer.valueOf(403).equals(esException.getStatusCode());
    }

    static ElasticsearchException findElasticsearchException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ElasticsearchException esException) {
                return esException;
            }
            current = current.getCause();
        }
        return null;
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
