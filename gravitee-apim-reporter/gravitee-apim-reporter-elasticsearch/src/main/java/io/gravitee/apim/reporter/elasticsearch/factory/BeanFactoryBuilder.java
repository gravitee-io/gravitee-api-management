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

import static java.lang.String.format;

import io.gravitee.apim.reporter.elasticsearch.config.BulkRules;
import io.gravitee.apim.reporter.elasticsearch.config.DataStreamRules;
import io.gravitee.apim.reporter.elasticsearch.config.Flavor;
import io.gravitee.apim.reporter.elasticsearch.config.FlavorRules;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ServerlessRetention;
import io.gravitee.apim.reporter.elasticsearch.factory.es7.Elastic7xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.es8.Elastic8xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.es9.Elastic9xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.opensearch.OpenSearchBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.mapping.datastream.DataStreamLifecycle;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.springframework.util.StringUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class BeanFactoryBuilder {

    private static final Set<Integer> SUPPORTED_OPENSEARCH_MAJOR_VERSIONS = Set.of(1, 2, 3);
    private static final String REPORTER_DISABLED = "The Elasticsearch reporter is disabled.";

    /**
     * @return the beans for the cluster, or null when the reporter must stay inactive: the reason is already logged
     */
    public static BeanFactory buildFactory(final Client client, final ReporterConfiguration configuration) {
        final Optional<Flavor> configuredFlavor;
        try {
            configuredFlavor = Flavor.configured(configuration.getFlavor());
        } catch (IllegalArgumentException e) {
            log.error("{} {}", e.getMessage(), REPORTER_DISABLED);
            return null;
        }

        final Optional<String> bulkViolation = BulkRules.violation(configuration);
        if (bulkViolation.isPresent()) {
            log.error("{} {}", bulkViolation.get(), REPORTER_DISABLED);
            return null;
        }

        ElasticsearchInfo info = retrieveElasticSearchInfo(client, configuration);
        final Flavor flavor = configuredFlavor.orElseGet(() -> Flavor.detected(info));
        final Optional<String> violation = FlavorRules.violation(flavor, configuration);
        if (violation.isPresent()) {
            log.error("{} {}", violation.get(), REPORTER_DISABLED);
            return null;
        }

        final BeanFactory beanFactory = getBeanFactoryFromElasticsearchInfo(info);
        if (beanFactory == null) {
            log.error(
                "{} version {} is not supported by this connector",
                info.getVersion().isOpenSearch() ? "OpenSearch" : "ElasticSearch",
                info.getVersion().getNumber()
            );
            return null;
        }
        // Serverless answers with a regular 8.x or 9.x version: only its document format comes from the classic beans.
        if (flavor == Flavor.SERVERLESS) {
            return serverlessBeanFactory(beanFactory, configuration, configuredFlavor.isEmpty());
        }
        if (!configuration.isDataStreamIndex()) {
            logStartupSummary(configuration, flavor, configuredFlavor.isEmpty());
            return beanFactory;
        }

        final Version version = info.getVersion();
        final Optional<String> dataStreamViolation = DataStreamRules.violation(version, configuration);
        if (dataStreamViolation.isPresent()) {
            log.error("{} {}", dataStreamViolation.get(), REPORTER_DISABLED);
            return null;
        }
        logStartupSummary(configuration, flavor, configuredFlavor.isEmpty());
        DataStreamRules.warnings(version, configuration).forEach(log::warn);
        return new DataStreamBeanFactory(beanFactory, DataStreamLifecycle.of(version, configuration));
    }

    private static BeanFactory serverlessBeanFactory(
        final BeanFactory clusterBeanFactory,
        final ReporterConfiguration configuration,
        final boolean detected
    ) {
        final Optional<String> retentionViolation = ServerlessRetention.violation(configuration);
        if (retentionViolation.isPresent()) {
            log.error("{} {}", retentionViolation.get(), REPORTER_DISABLED);
            return null;
        }
        final ServerlessRetention retention = ServerlessRetention.of(configuration);
        log.info("{}, retention [{}]", startupSummary(configuration, Flavor.SERVERLESS, detected), retention.summary());
        return new ServerlessBeanFactory(clusterBeanFactory, retention);
    }

    private static void logStartupSummary(final ReporterConfiguration configuration, final Flavor flavor, final boolean detected) {
        log.info(startupSummary(configuration, flavor, detected));
    }

    private static String startupSummary(final ReporterConfiguration configuration, final Flavor flavor, final boolean detected) {
        return format(
            "Elasticsearch reporter: flavor [%s] (%s), authentication [%s], index mode [%s], disabled by the flavor [%s]",
            flavor.value(),
            detected ? "detected" : "configured",
            authenticationMode(configuration),
            FlavorRules.indexMode(flavor, configuration),
            FlavorRules.disabledFeatures(flavor)
        );
    }

    private static String authenticationMode(final ReporterConfiguration configuration) {
        if (StringUtils.hasText(configuration.getApiKey())) {
            return "api_key";
        }
        return StringUtils.hasText(configuration.getUsername()) ? "basic" : "none";
    }

    private static ElasticsearchInfo retrieveElasticSearchInfo(final Client client, final ReporterConfiguration configuration) {
        // Wait for a connection to ES and retry each 5 seconds
        return client
            .getInfo()
            .retryWhen(error ->
                error.flatMap(throwable -> {
                    if (isUnauthorized(throwable)) {
                        log.error(
                            "Elasticsearch authentication failed (401). Please verify your {} configuration.",
                            authenticationSettings(configuration),
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

    private static String authenticationSettings(final ReporterConfiguration configuration) {
        // An empty API key counts as unset, as it does for the Elasticsearch client.
        return StringUtils.hasText(configuration.getApiKey())
            ? "'reporters.elasticsearch.security.api_key'"
            : "'reporters.elasticsearch.security.username' and 'reporters.elasticsearch.security.password'";
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
