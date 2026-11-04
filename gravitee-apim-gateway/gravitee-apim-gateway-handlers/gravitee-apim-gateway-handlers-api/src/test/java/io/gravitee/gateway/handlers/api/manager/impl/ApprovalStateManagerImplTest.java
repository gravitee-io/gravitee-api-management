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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl.Index;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateManagerImplTest {

    private static final long NOW = System.currentTimeMillis();
    private static final long LATER = NOW + 60_000L;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private final SettableClock clock = new SettableClock(NOW);
    private final TestScheduler timers = new TestScheduler();
    private ApprovalStateManagerImpl manager;

    @BeforeEach
    void setUp() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        lenient().when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);
        manager = new ApprovalStateManagerImpl(new ApprovalFeature(licenseManager, true), clock);
    }

    private Optional<DeployedApprovalState> byId(String id) {
        return Optional.ofNullable(manager.byId(id).blockingGet());
    }

    private Optional<DeployedApprovalState> byOpenKey(String openKey) {
        return Optional.ofNullable(manager.byOpenKey(openKey).blockingGet());
    }

    private Optional<DeployedApprovalState> activeGrant(String subject) {
        return Optional.ofNullable(manager.activeGrant("env-1", "api-1", "rule-1", "app-1", subject, "digest-1").blockingGet());
    }

    @Nested
    class ById {

        @Test
        void should_return_a_deployed_state() {
            DeployedApprovalState state = pending("req-1", "open-1", 1L);
            manager.deploy(state);

            assertThat(byId("req-1")).contains(state);
        }

        @Test
        void should_return_the_state_an_alias_was_joined_into() {
            DeployedApprovalState state = withAliases(pending("req-1", "open-1", 1L), "alias-1", "alias-2");
            manager.deploy(state);

            assertThat(byId("alias-1")).contains(state);
            assertThat(byId("alias-2")).contains(state);
        }

        @Test
        void should_return_nothing_for_an_unknown_id() {
            assertThat(byId("unknown")).isEmpty();
            assertThat(byId(null)).isEmpty();
        }

        @Test
        void should_keep_a_request_found_by_its_id_only_once_released() {
            DeployedApprovalState ran = withOpenKey(withStatus(approved("req-1", 2L), "EXECUTED"), null);

            manager.deploy(ran);

            assertThat(byId("req-1")).contains(ran);
        }

        @Test
        void should_hide_a_state_once_its_keep_until_has_passed() {
            manager.deploy(pending("req-1", "open-1", 1L));

            clock.set(LATER);

            assertThat(byId("req-1")).isEmpty();
            assertThat(byOpenKey("open-1")).isEmpty();
        }

        @Test
        void should_remove_a_state_deployed_after_its_keep_until() {
            manager.deploy(pending("req-1", "open-1", 1L));

            manager.deploy(keptUntil(approved("req-1", 2L), NOW - 1));

            assertThat(byId("req-1")).isEmpty();
            assertThat(manager.states()).isZero();
            assertThat(manager.indexed(Index.OPEN_KEY)).isEmpty();
        }

        @Test
        void should_read_when_subscribed_and_not_before() {
            Maybe<DeployedApprovalState> lookup = manager.byId("req-1");
            DeployedApprovalState state = pending("req-1", "open-1", 1L);
            manager.deploy(state);

            assertThat(lookup.blockingGet()).isEqualTo(state);
        }
    }

    @Nested
    class Version {

        @Test
        void should_ignore_an_older_copy() {
            manager.deploy(approved("req-1", 2L));

            manager.deploy(pending("req-1", "open-1", 1L));

            assertThat(byId("req-1")).get().extracting(DeployedApprovalState::status).isEqualTo("APPROVED");
        }

        @Test
        void should_keep_the_higher_version_when_a_lower_one_arrives_after_it() {
            manager.deploy(versioned(approved("req-1", 1L), 6L));

            manager.deploy(versioned(pending("req-1", "open-1", 9L), 5L));

            assertThat(byId("req-1")).get().extracting(DeployedApprovalState::version).isEqualTo(6L);
        }

        @Test
        void should_replace_a_state_with_a_different_copy_written_in_the_same_millisecond() {
            manager.deploy(pending("req-1", "open-1", 2L));

            manager.deploy(approved("req-1", 2L));

            assertThat(byId("req-1")).get().extracting(DeployedApprovalState::status).isEqualTo("APPROVED");
        }

        @Test
        void should_not_wake_a_wait_for_an_older_copy() {
            manager.deploy(pending("req-1", "open-1", 2L));
            TestObserver<Void> observer = manager.awaitChange("req-1", Duration.ofSeconds(30), timers).test();

            manager.deploy(pending("req-1", "open-1", 1L));
            timers.triggerActions();

            observer.assertNotComplete();
        }
    }

    @Nested
    class ByOpenKey {

        @Test
        void should_return_the_pending_state_with_that_open_key() {
            DeployedApprovalState state = pending("req-1", "open-1", 1L);
            manager.deploy(state);

            assertThat(byOpenKey("open-1")).contains(state);
            assertThat(byOpenKey("open-2")).isEmpty();
            assertThat(byOpenKey(null)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "PENDING", "APPROVED", "EXECUTING", "REJECTED", "EXPIRED" })
        void should_find_a_request_not_yet_released_by_its_open_key(String status) {
            manager.deploy(withStatus(pending("req-1", "open-1", 1L), status));

            assertThat(byOpenKey("open-1")).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = { "EXECUTED", "UNKNOWN" })
        void should_not_find_a_request_a_call_again_never_resumes(String status) {
            manager.deploy(withStatus(pending("req-1", "open-1", 1L), status));

            assertThat(byOpenKey("open-1")).isEmpty();
            assertThat(manager.indexed(Index.OPEN_KEY)).isEmpty();
        }

        @Test
        void should_stop_finding_a_request_once_released() {
            manager.deploy(pending("req-1", "open-1", 1L));

            manager.deploy(withOpenKey(withStatus(pending("req-1", "open-1", 2L), "EXECUTED"), null));

            assertThat(byOpenKey("open-1")).isEmpty();
            assertThat(manager.indexed(Index.OPEN_KEY)).isEmpty();
        }

        @Test
        void should_keep_finding_a_second_request_with_the_same_open_key_when_the_first_is_removed() {
            manager.deploy(pending("req-1", "open-1", 1L));
            DeployedApprovalState second = pending("req-2", "open-1", 2L);
            manager.deploy(second);

            manager.undeploy("req-1");

            assertThat(byOpenKey("open-1")).contains(second);
        }

        @Test
        void should_return_the_latest_updated_request_when_several_share_the_open_key() {
            manager.deploy(pending("req-1", "open-1", 1L));
            DeployedApprovalState latest = pending("req-2", "open-1", 5L);
            manager.deploy(latest);
            manager.deploy(pending("req-3", "open-1", 3L));

            assertThat(byOpenKey("open-1")).contains(latest);
        }
    }

    @Nested
    class ActiveGrant {

        @Test
        void should_return_an_approved_time_based_grant_covering_the_call() {
            DeployedApprovalState grant = timedGrant("req-1", "APPROVED", LATER, "sub-1", 1L);
            manager.deploy(grant);

            assertThat(activeGrant("sub-1")).contains(grant);
        }

        @Test
        void should_match_a_grant_without_subject_to_a_call_without_subject() {
            DeployedApprovalState grant = timedGrant("req-1", "APPROVED", LATER, null, 1L);
            manager.deploy(grant);

            assertThat(activeGrant(null)).contains(grant);
            assertThat(activeGrant("sub-1")).isEmpty();
        }

        @Test
        void should_not_return_a_grant_for_another_caller_or_api() {
            manager.deploy(timedGrant("req-1", "APPROVED", LATER, "sub-1", 1L));

            assertThat(activeGrant("sub-2")).isEmpty();
            assertThat(manager.activeGrant("env-1", "api-2", "rule-1", "app-1", "sub-1", "digest-1").blockingGet()).isNull();
        }

        @Test
        void should_not_index_a_grant_that_is_not_time_based() {
            manager.deploy(approved("req-1", 1L));

            assertThat(manager.indexed(Index.GRANT)).isEmpty();
        }

        @Test
        void should_not_return_a_grant_once_it_no_longer_covers_the_call() {
            manager.deploy(timedGrant("req-1", "APPROVED", NOW + 1_000L, "sub-1", 1L));

            clock.set(NOW + 1_000L);

            assertThat(activeGrant("sub-1")).isEmpty();
        }

        @Test
        void should_forget_a_grant_ended_early() {
            manager.deploy(timedGrant("req-1", "APPROVED", LATER, "sub-1", 1L));

            manager.deploy(timedGrant("req-1", "APPROVED", NOW, "sub-1", 2L));

            assertThat(activeGrant("sub-1")).isEmpty();
            assertThat(manager.indexed(Index.GRANT)).isEmpty();
        }

        @Test
        void should_return_the_grant_covering_the_longest_when_several_do() {
            manager.deploy(timedGrant("req-1", "APPROVED", NOW + 10_000L, "sub-1", 1L));
            DeployedApprovalState longest = timedGrant("req-2", "APPROVED", LATER, "sub-1", 1L);
            manager.deploy(longest);

            assertThat(activeGrant("sub-1")).contains(longest);
        }

        @Test
        void should_keep_no_end_user_id_in_an_index_key() {
            manager.deploy(timedGrant("req-1", "APPROVED", LATER, "alice@example.com", 1L));

            assertThat(manager.indexed(Index.GRANT)).allSatisfy(key -> assertThat(key).doesNotContain("alice").matches("[0-9a-f]{64}"));
        }
    }

    @Nested
    class Removal {

        @Test
        void should_remove_a_state_and_every_index_entry_once_undeployed() {
            manager.deploy(withAliases(timedGrant("req-1", "APPROVED", LATER, "sub-1", 1L), "alias-1"));

            manager.undeploy("req-1");

            assertThat(byId("req-1")).isEmpty();
            assertThat(byId("alias-1")).isEmpty();
            assertThat(manager.states()).isZero();
            assertThat(manager.indexed(Index.ALIAS)).isEmpty();
            assertThat(manager.indexed(Index.OPEN_KEY)).isEmpty();
            assertThat(manager.indexed(Index.GRANT)).isEmpty();
        }

        @Test
        void should_remove_the_index_entries_a_newer_copy_no_longer_has() {
            manager.deploy(withAliases(pending("req-1", "open-1", 1L), "alias-1"));

            manager.deploy(withOpenKey(pending("req-1", "open-2", 2L), "open-2"));

            assertThat(byOpenKey("open-1")).isEmpty();
            assertThat(byId("alias-1")).isEmpty();
            assertThat(byOpenKey("open-2")).isPresent();
        }

        @Test
        void should_ignore_undeploying_an_unknown_state() {
            manager.undeploy("unknown");

            assertThat(manager.states()).isZero();
        }
    }

    @Nested
    class AwaitChange {

        @Test
        void should_complete_on_the_scheduler_when_the_request_changes() {
            TestObserver<Void> observer = manager.awaitChange("req-1", Duration.ofSeconds(30), timers).test();

            manager.deploy(pending("req-1", "open-1", 1L));

            observer.assertNotComplete();
            timers.triggerActions();
            observer.assertComplete();
            assertThat(manager.waiters()).isZero();
        }

        @Test
        void should_complete_when_the_request_an_id_was_joined_into_changes() {
            TestObserver<Void> observer = manager.awaitChange("alias-1", Duration.ofSeconds(30), timers).test();

            manager.deploy(withAliases(pending("req-1", "open-1", 1L), "alias-1"));
            timers.triggerActions();

            observer.assertComplete();
        }

        @Test
        void should_complete_the_waits_on_a_request_and_its_aliases_when_it_is_undeployed() {
            manager.deploy(withAliases(pending("req-1", "open-1", 1L), "alias-1"));
            TestObserver<Void> onId = manager.awaitChange("req-1", Duration.ofSeconds(30), timers).test();
            TestObserver<Void> onAlias = manager.awaitChange("alias-1", Duration.ofSeconds(30), timers).test();

            manager.undeploy("req-1");
            timers.triggerActions();

            onId.assertComplete();
            onAlias.assertComplete();
        }

        @Test
        void should_complete_for_a_change_made_between_asking_and_subscribing() {
            var changed = manager.awaitChange("req-1", Duration.ofSeconds(30), timers);

            manager.deploy(pending("req-1", "open-1", 1L));
            TestObserver<Void> observer = changed.test();
            timers.triggerActions();

            observer.assertComplete();
        }

        @Test
        void should_complete_once_at_most_has_passed_and_forget_the_wait() {
            TestObserver<Void> observer = manager.awaitChange("req-1", Duration.ofSeconds(30), timers).test();

            timers.advanceTimeBy(30, TimeUnit.SECONDS);

            observer.assertComplete();
            assertThat(manager.waiters()).isZero();
        }

        @Test
        void should_forget_a_wait_never_subscribed_to_once_at_most_has_passed() {
            manager.awaitChange("req-1", Duration.ofSeconds(30), timers);

            timers.advanceTimeBy(30, TimeUnit.SECONDS);

            assertThat(manager.waiters()).isZero();
        }

        @Test
        void should_forget_a_disposed_wait() {
            manager.awaitChange("req-1", Duration.ofSeconds(30), timers).test().dispose();

            assertThat(manager.waiters()).isZero();
        }
    }

    @Nested
    class ConcurrentUpdate {

        @Test
        void should_index_and_then_remove_every_state_deployed_concurrently_under_a_shared_open_key() throws Exception {
            // The sync deploys states from several threads (services.sync.appender.parallelism). Every id must survive
            // the concurrent maintenance of a shared index entry, so undeploying them all leaves no entry behind.
            int writerCount = 8;
            int statesPerWriter = 200;
            CyclicBarrier barrier = new CyclicBarrier(writerCount);
            List<Future<?>> futures = new ArrayList<>();
            try (ExecutorService writers = Executors.newFixedThreadPool(writerCount)) {
                for (int w = 0; w < writerCount; w++) {
                    int writerId = w;
                    futures.add(
                        writers.submit(() -> {
                            barrier.await();
                            for (int i = 0; i < statesPerWriter; i++) {
                                manager.deploy(pending("req-" + writerId + "-" + i, "open-1", 1L));
                            }
                            return null;
                        })
                    );
                }
                for (Future<?> future : futures) {
                    future.get(10, TimeUnit.SECONDS);
                }
            }

            assertThat(manager.states()).isEqualTo(writerCount * statesPerWriter);
            assertThat(byOpenKey("open-1")).isPresent();

            for (int w = 0; w < writerCount; w++) {
                for (int i = 0; i < statesPerWriter; i++) {
                    manager.undeploy("req-" + w + "-" + i);
                }
            }

            assertThat(manager.states()).isZero();
            assertThat(manager.indexed(Index.OPEN_KEY)).isEmpty();
        }
    }

    @Test
    void should_receive_states_only_when_gamma_is_enabled_and_the_organization_licensed() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform("org-2")).thenReturn(null);

        assertThat(manager.receivesStates("org-1")).isTrue();
        assertThat(manager.receivesStates("org-2")).isFalse();
    }

    static DeployedApprovalState pending(String id, String openKey, long updatedAt) {
        return new DeployedApprovalState(
            id,
            List.of(),
            "env-1",
            "api-1",
            "rule-1",
            "2026-10-01T00:00:00Z",
            openKey,
            "PENDING",
            LATER,
            null,
            null,
            null,
            LATER,
            NOW - 1_000L,
            null,
            updatedAt,
            0L
        );
    }

    static DeployedApprovalState approved(String id, long updatedAt) {
        return new DeployedApprovalState(
            id,
            List.of(),
            "env-1",
            "api-1",
            "rule-1",
            "2026-10-01T00:00:00Z",
            "open-1",
            "APPROVED",
            null,
            null,
            new Grant("ONCE", null, "app-1", "sub-1", "digest-1"),
            new ExecutorLease("node-1", "lease-1", LATER),
            LATER,
            NOW - 1_000L,
            null,
            updatedAt,
            0L
        );
    }

    static DeployedApprovalState timedGrant(String id, String status, long coversUntil, String subject, long updatedAt) {
        return new DeployedApprovalState(
            id,
            List.of(),
            "env-1",
            "api-1",
            "rule-1",
            "2026-10-01T00:00:00Z",
            "open-" + id,
            status,
            null,
            null,
            new Grant("TIMED", coversUntil, "app-1", subject, "digest-1"),
            null,
            LATER,
            NOW - 1_000L,
            null,
            updatedAt,
            0L
        );
    }

    static DeployedApprovalState withAliases(DeployedApprovalState s, String... aliasIds) {
        return copy(s, List.of(aliasIds), s.openKey(), s.status(), s.keepUntil(), s.version());
    }

    private static DeployedApprovalState withStatus(DeployedApprovalState s, String status) {
        return copy(s, s.aliasIds(), s.openKey(), status, s.keepUntil(), s.version());
    }

    private static DeployedApprovalState withOpenKey(DeployedApprovalState s, String openKey) {
        return copy(s, s.aliasIds(), openKey, s.status(), s.keepUntil(), s.version());
    }

    private static DeployedApprovalState keptUntil(DeployedApprovalState s, long keepUntil) {
        return copy(s, s.aliasIds(), s.openKey(), s.status(), keepUntil, s.version());
    }

    private static DeployedApprovalState versioned(DeployedApprovalState s, long version) {
        return copy(s, s.aliasIds(), s.openKey(), s.status(), s.keepUntil(), version);
    }

    private static DeployedApprovalState copy(
        DeployedApprovalState s,
        List<String> aliasIds,
        String openKey,
        String status,
        long keepUntil,
        long version
    ) {
        return new DeployedApprovalState(
            s.id(),
            aliasIds,
            s.environmentId(),
            s.apiId(),
            s.ruleId(),
            s.ruleUpdatedAt(),
            openKey,
            status,
            s.expiresAt(),
            s.reason(),
            s.grant(),
            s.executor(),
            keepUntil,
            s.createdAt(),
            s.startedAt(),
            s.updatedAt(),
            version
        );
    }

    private static final class SettableClock extends Clock {

        private final AtomicLong millis;

        SettableClock(long millis) {
            this.millis = new AtomicLong(millis);
        }

        void set(long millis) {
            this.millis.set(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }
    }
}
