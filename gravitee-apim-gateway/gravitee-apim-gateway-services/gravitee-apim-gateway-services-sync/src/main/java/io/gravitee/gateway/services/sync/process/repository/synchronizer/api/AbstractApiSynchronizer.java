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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.flowables.GroupedFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiSynchronizer {

    protected final ApiManager apiManager;
    protected final ApiMapper apiMapper;
    protected final PlanAppender planAppender;
    protected final SubscriptionAppender subscriptionAppender;
    protected final ApiKeyAppender apiKeyAppender;
    protected final DeployerFactory deployerFactory;
    protected final ThreadPoolExecutor syncFetcherExecutor;
    protected final ThreadPoolExecutor syncDeployerExecutor;

    protected abstract int bulkEvents();

    protected Flowable<ApiReactorDeployable> processEvents(final boolean initialSync, final Flowable<List<Event>> eventsFlowable) {
        return eventsFlowable
            // fetch per page
            .flatMap(events ->
                Flowable
                    .just(events)
                    .doOnNext(e -> log.debug("New api events fetch"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.PUBLISH_API || eventsByType.getKey() == EventType.START_API) {
                            return prepareForDeployment(initialSync, eventsByType);
                        } else if (eventsByType.getKey() == EventType.UNPUBLISH_API || eventsByType.getKey() == EventType.STOP_API) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream -> {
                SubscriptionDeployer subscriptionDeployer = deployerFactory.createSubscriptionDeployer();
                ApiKeyDeployer apiKeyDeployer = deployerFactory.createApiKeyDeployer();
                ApiDeployer apiDeployer = deployerFactory.createApiDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deployApi(subscriptionDeployer, apiKeyDeployer, apiDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeployApi(subscriptionDeployer, apiKeyDeployer, apiDeployer, deployable);
                        } else {
                            return Flowable.just(deployable);
                        }
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<ApiReactorDeployable> prepareForDeployment(
        final boolean initialSync,
        final GroupedFlowable<EventType, Event> eventsByType
    ) {
        return eventsByType
            .flatMapMaybe(apiMapper::to)
            .groupBy(apiManager::requiredActionFor)
            .flatMap(reactableByAction -> {
                if (reactableByAction.getKey() == ActionOnApi.DEPLOY) {
                    return reactableByAction
                        .map(reactableApi ->
                            ApiReactorDeployable
                                .builder()
                                .apiId(reactableApi.getId())
                                .syncAction(SyncAction.DEPLOY)
                                .reactableApi(reactableApi)
                                .build()
                        )
                        .buffer(bulkEvents())
                        .map(planAppender::appends)
                        .map(deployables -> subscriptionAppender.appends(initialSync, deployables))
                        .map(deployables -> apiKeyAppender.appends(initialSync, deployables))
                        .flatMapIterable(d -> d);
                } else if (reactableByAction.getKey() == ActionOnApi.UNDEPLOY) {
                    return reactableByAction.map(reactableApi ->
                        ApiReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).apiId(reactableApi.getId()).build()
                    );
                } else {
                    return Flowable.empty();
                }
            });
    }

    private Flowable<ApiReactorDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(apiMapper::toId)
            .map(apiId -> ApiReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).apiId(apiId).build());
    }

    private Flowable<ApiReactorDeployable> deployApi(
        final SubscriptionDeployer subscriptionDeployer,
        final ApiKeyDeployer apiKeyDeployer,
        final ApiDeployer apiDeployer,
        final ApiReactorDeployable deployable
    ) {
        return subscriptionDeployer
            .deploy(deployable)
            .andThen(apiKeyDeployer.deploy(deployable))
            .andThen(apiDeployer.deploy(deployable))
            .andThen(subscriptionDeployer.doAfterDeployment(deployable))
            .andThen(apiKeyDeployer.doAfterDeployment(deployable))
            .andThen(apiDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<ApiReactorDeployable> undeployApi(
        final SubscriptionDeployer subscriptionDeployer,
        final ApiKeyDeployer apiKeyDeployer,
        final ApiDeployer apiDeployer,
        final ApiReactorDeployable deployable
    ) {
        return apiDeployer
            .undeploy(deployable)
            .andThen(subscriptionDeployer.undeploy(deployable))
            .andThen(apiKeyDeployer.undeploy(deployable))
            .andThen(subscriptionDeployer.doAfterUndeployment(deployable))
            .andThen(apiKeyDeployer.doAfterUndeployment(deployable))
            .andThen(apiDeployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }
}
