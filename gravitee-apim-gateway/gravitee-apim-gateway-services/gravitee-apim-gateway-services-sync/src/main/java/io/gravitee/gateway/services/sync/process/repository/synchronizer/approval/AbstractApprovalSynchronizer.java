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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approval;

import io.gravitee.gateway.services.sync.process.common.deployer.Deployer;
import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.ResyncWindow;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

/**
 * Shared synchronization engine for the approval documents (rules and states). It owns the fetch → map → resolve
 * organization → (un)deploy → commit pipeline and the {@link ResyncWindow} isolation of a failing pass; subclasses
 * only supply the type-specific bindings (mapper, deployer, event types, labels) and decide whether a failing initial
 * pass fails the sync.
 *
 * @param <D> the deployable type handled by this synchronizer
 * @param <R> the deployer type staging it
 */
@CustomLog
public abstract class AbstractApprovalSynchronizer<D extends Deployable, R extends Deployer<D>> implements RepositorySynchronizer {

    private final LatestEventFetcher eventsFetcher;
    private final EnvironmentService environmentService;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;
    private final ResyncWindow resyncWindow = new ResyncWindow();

    protected AbstractApprovalSynchronizer(
        LatestEventFetcher eventsFetcher,
        EnvironmentService environmentService,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.environmentService = environmentService;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    protected abstract Maybe<D> toDeploy(Event event);

    protected abstract Maybe<D> toUndeploy(Event event);

    protected abstract R createDeployer();

    protected abstract Event.EventProperties eventProperty();

    protected abstract EventType publishType();

    protected abstract EventType unpublishType();

    protected abstract String environmentIdOf(D deployable);

    protected abstract D withOrganizationId(D deployable, String organizationId);

    /** Singular log label, e.g. {@code "approval rule"}. */
    protected abstract String singularLabel();

    /** Plural log label, e.g. {@code "approval rules"}. */
    protected abstract String pluralLabel();

    /** Whether a failing initial pass is isolated like an incremental one instead of failing the sync. */
    protected abstract boolean isolatesFailedInitialPass();

    /** Runs once at the end of a pass, after every item of the pass has been staged. */
    protected Completable commit(R deployer) {
        return Completable.complete();
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        return Completable.defer(() -> {
            Long effectiveFrom = resyncWindow.effectiveFrom(from);
            boolean initialSync = effectiveFrom == null || effectiveFrom == -1L;
            Completable pass = synchronizePass(effectiveFrom, to, environments, initialSync).doOnComplete(() -> {
                if (resyncWindow.markSucceeded()) {
                    log.info("{} synchronization recovered, the missed window has been fetched again", capitalizedPluralLabel());
                }
            });
            if (initialSync && !isolatesFailedInitialPass()) {
                return pass;
            }
            return pass.onErrorResumeNext(throwable -> {
                if (resyncWindow.markFailed(effectiveFrom)) {
                    log.error(
                        "{} synchronization failed, the window from {} will be fetched again on the next pass",
                        capitalizedPluralLabel(),
                        effectiveFrom,
                        throwable
                    );
                } else {
                    log.debug("{} synchronization still failing: {}", capitalizedPluralLabel(), throwable.toString());
                }
                return Completable.complete();
            });
        });
    }

    private Completable synchronizePass(final Long from, final Long to, final Set<String> environments, final boolean initialSync) {
        AtomicLong launchTime = new AtomicLong();
        R deployer = createDeployer();
        return eventsFetcher
            .fetchLatest(
                from,
                to,
                eventProperty(),
                environments,
                initialSync ? Set.of(publishType()) : Set.of(publishType(), unpublishType())
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            // fetch per page
            .flatMap(events ->
                Flowable.just(events)
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == publishType()) {
                            return eventsByType.flatMapMaybe(this::toDeploy).flatMapMaybe(this::withOrganization);
                        } else if (eventsByType.getKey() == unpublishType()) {
                            return eventsByType.flatMapMaybe(this::toUndeploy);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream ->
                upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(deployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(deployer, deployable);
                        } else {
                            return Flowable.empty();
                        }
                    })
                    .sequential(eventsFetcher.bulkItems())
            )
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .flatMapCompletable(count ->
                commit(deployer).doOnComplete(() -> {
                    String logMsg = String.format(
                        "%s %s synchronized in %sms",
                        count,
                        pluralLabel(),
                        (System.currentTimeMillis() - launchTime.get())
                    );
                    if (initialSync) {
                        log.info(logMsg);
                    } else {
                        log.debug(logMsg);
                    }
                })
            );
    }

    private Maybe<D> withOrganization(final D deployable) {
        String environmentId = environmentIdOf(deployable);
        String organizationId = environmentService.organizationIdOf(environmentId);
        if (organizationId == null) {
            log.debug("Skipping {} [{}]: its environment [{}] could not be found", singularLabel(), deployable.id(), environmentId);
            return Maybe.empty();
        }
        return Maybe.just(withOrganizationId(deployable, organizationId));
    }

    private Flowable<D> deploy(final R deployer, final D deployable) {
        return deployer
            .deploy(deployable)
            .andThen(deployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error("Failed to deploy {} '{}'", singularLabel(), deployable.id(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<D> undeploy(final R deployer, final D deployable) {
        return deployer
            .undeploy(deployable)
            .andThen(deployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error("Failed to undeploy {} '{}'", singularLabel(), deployable.id(), throwable);
                return Flowable.empty();
            });
    }

    private String capitalizedPluralLabel() {
        String label = pluralLabel();
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
