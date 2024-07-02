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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.organization;

import static io.gravitee.repository.management.model.Event.EventProperties.ORGANIZATION_ID;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.OrganizationDeployer;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.OrganizationMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class OrganizationSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> EVENT_TYPES = Set.of(EventType.PUBLISH_ORGANIZATION);
    private final LatestEventFetcher eventsFetcher;
    private final OrganizationMapper organizationMapper;
    private final FlowAppender flowAppender;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return eventsFetcher
            .fetchLatest(from, to, ORGANIZATION_ID, environments, EVENT_TYPES)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            // fetch per page
            .flatMap(events -> Flowable.just(events).flatMapIterable(e -> e).compose(this::prepareForDeployment))
            // per deployable
            .compose(upstream -> {
                OrganizationDeployer organizationDeployer = deployerFactory.createOrganizationDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> deploy(organizationDeployer, deployable))
                    .sequential(eventsFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s organizations synchronized in %sms",
                    count,
                    (System.currentTimeMillis() - launchTime.get())
                );
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    private Flowable<OrganizationDeployable> prepareForDeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(organizationMapper::to)
            .map(organization -> OrganizationDeployable.builder().reactableOrganization(organization).build())
            .map(flowAppender::appends);
    }

    private static Flowable<OrganizationDeployable> deploy(
        final OrganizationDeployer organizationDeployer,
        final OrganizationDeployable deployable
    ) {
        return organizationDeployer
            .deploy(deployable)
            .andThen(organizationDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    @Override
    public int order() {
        return Order.ORGANIZATION.index();
    }
}
