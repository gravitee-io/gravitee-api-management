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
package io.gravitee.gateway.services.sync.process.kubernetes.synchronizer;

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.kubernetes.KubernetesSynchronizer;
import io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.AbstractApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class KubernetesApiSynchronizer extends AbstractApiSynchronizer implements KubernetesSynchronizer {

    private final ConfigMapEventFetcher configMapEventFetcher;

    public KubernetesApiSynchronizer(
        final ConfigMapEventFetcher configMapEventFetcher,
        final ApiManager apiManager,
        final ApiMapper apiMapper,
        final PlanAppender planAppender,
        final SubscriptionAppender subscriptionAppender,
        final ApiKeyAppender apiKeyAppender,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncKubernetesExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        super(
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncKubernetesExecutor,
            syncDeployerExecutor
        );
        this.configMapEventFetcher = configMapEventFetcher;
    }

    public Completable synchronize(final Set<String> environments) {
        return configMapEventFetcher
            .fetchAll(ConfigMapEventFetcher.API_DEFINITIONS_KIND)
            .compose(upstream -> processEvents(true, upstream, environments))
            .doOnNext(apiReactor -> log.debug("api {} synchronized from kubernetes", apiReactor.apiId()))
            .ignoreElements();
    }

    @Override
    public Completable watch(Set<String> environments) {
        return configMapEventFetcher
            .fetchLatest(ConfigMapEventFetcher.API_DEFINITIONS_KIND)
            .compose(upstream -> processEvents(false, upstream, environments))
            .doOnNext(apiReactor -> log.debug("api {} synchronized from kubernetes", apiReactor.apiId()))
            .ignoreElements();
    }

    @Override
    protected int bulkEvents() {
        return 1;
    }
}
