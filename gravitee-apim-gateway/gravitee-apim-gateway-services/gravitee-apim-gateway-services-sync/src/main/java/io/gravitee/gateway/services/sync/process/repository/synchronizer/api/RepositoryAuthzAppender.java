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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyReactorDeployable;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Event.EventProperties;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.CustomLog;

@CustomLog
public class RepositoryAuthzAppender implements AuthzAppender {

    static final int LARGE_EVENT_SET_WARN_THRESHOLD = 5_000;
    static final long PAGE_TIMEOUT_SECONDS = 30L;
    static final Duration EVENT_CACHE_TTL = Duration.ofSeconds(30);

    private final AuthzEntityIdExtractor extractor;
    private final EventLatestRepository eventLatestRepository;
    private final AuthzPolicyMapper policyMapper;
    private final AuthzEnginePort enginePort;

    private final AtomicReference<CachedEvents> cachedEvents = new AtomicReference<>();

    public RepositoryAuthzAppender(
        AuthzEntityIdExtractor extractor,
        EventLatestRepository eventLatestRepository,
        AuthzPolicyMapper policyMapper,
        AuthzEnginePort enginePort
    ) {
        this.extractor = extractor;
        this.eventLatestRepository = eventLatestRepository;
        this.policyMapper = policyMapper;
        this.enginePort = enginePort;
    }

    @Override
    public Single<List<ApiReactorDeployable>> appends(
        boolean initialSync,
        List<ApiReactorDeployable> deployables,
        Set<String> environments
    ) {
        if (!initialSync || deployables == null || deployables.isEmpty()) {
            return Single.just(deployables == null ? List.of() : deployables);
        }

        Set<String> allEntityIds = new HashSet<>();
        for (ApiReactorDeployable d : deployables) {
            ReactableApi<?> api = d.reactableApi();
            if (api != null) {
                allEntityIds.addAll(extractor.extract(api));
            }
        }
        if (allEntityIds.isEmpty()) {
            return Single.just(deployables);
        }

        // I6: the staging chain may resume on different reactive schedulers; a plain HashSet
        // here would be a data race when doOnSuccess fires concurrently with completion.
        Set<String> stagedPolicies = ConcurrentHashMap.newKeySet();

        return Completable.defer(() -> stageResourcePolicies(allEntityIds, environments, stagedPolicies))
            .andThen(Completable.defer(() -> commitIfStagedAny(stagedPolicies)))
            .timeout(PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .toSingleDefault(deployables)
            .onErrorReturn(t -> {
                log.warn("Authz cold-start lookup failed; APIs deploy without their RESOURCE policies, will recover on next sync tick", t);
                return deployables;
            });
    }

    private Completable stageResourcePolicies(Set<String> entityIds, Set<String> environments, Set<String> staged) {
        return Completable.defer(() -> {
            List<Event> events = lookupEvents(environments);
            if (events.isEmpty()) {
                return Completable.complete();
            }
            warnIfLargeEventSet("RESOURCE policies", events.size());

            return Flowable.fromIterable(events)
                .concatMapMaybe(event ->
                    policyMapper
                        .toDeploy(event)
                        .filter(d -> d.kind() == AuthzPolicyReactorDeployable.Kind.RESOURCE)
                        .filter(d -> d.entityId() != null && entityIds.contains(d.entityId()))
                        .doOnSuccess(d -> staged.add(d.docId()))
                )
                .concatMapCompletable(deployable ->
                    enginePort.addOrUpdatePolicy(deployable.docId(), deployable.name(), deployable.policyText())
                )
                .doOnComplete(() -> log.info("AuthzAppender staged {} RESOURCE policies for cold-start", staged.size()));
        });
    }

    private List<Event> lookupEvents(Set<String> environments) {
        CachedEvents snapshot = cachedEvents.get();
        Instant now = Instant.now();
        if (
            snapshot != null && snapshot.matches(environments) && Duration.between(snapshot.timestamp(), now).compareTo(EVENT_CACHE_TTL) < 0
        ) {
            return snapshot.events();
        }
        EventCriteria criteria = EventCriteria.builder().types(Set.of(EventType.PUBLISH_AUTHZ_POLICY)).environments(environments).build();
        List<Event> events = eventLatestRepository.search(criteria, EventProperties.AUTHZ_POLICY_ID, null, null);
        List<Event> safe = events == null ? List.of() : List.copyOf(events);
        cachedEvents.set(new CachedEvents(environments == null ? Set.of() : Set.copyOf(environments), safe, now));
        return safe;
    }

    private Completable commitIfStagedAny(Set<String> stagedPolicies) {
        if (stagedPolicies.isEmpty()) {
            return Completable.complete();
        }
        return enginePort.commit();
    }

    private static void warnIfLargeEventSet(String kind, int size) {
        if (size > LARGE_EVENT_SET_WARN_THRESHOLD) {
            log.warn(
                "AuthzAppender: {} returned {} events from EventLatestRepository — in-memory filter may dominate cold-start latency.",
                kind,
                size
            );
        }
    }

    private record CachedEvents(Set<String> environments, List<Event> events, Instant timestamp) {
        boolean matches(Set<String> other) {
            Set<String> safeOther = other == null ? Set.of() : other;
            return environments.equals(safeOther);
        }
    }
}
