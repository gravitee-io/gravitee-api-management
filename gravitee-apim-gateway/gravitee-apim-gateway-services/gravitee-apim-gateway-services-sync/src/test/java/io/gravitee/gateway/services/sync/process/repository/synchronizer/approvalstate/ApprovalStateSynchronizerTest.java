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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl;
import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalStateDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateSynchronizerTest {

    private static final Set<EventType> INCREMENTAL = Set.of(EventType.PUBLISH_APPROVAL_STATE, EventType.UNPUBLISH_APPROVAL_STATE);

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApprovalStateDeployer deployer;

    @Mock
    private EnvironmentService environmentService;

    private ApprovalStateSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new ApprovalStateSynchronizer(
            fetcher,
            new ApprovalStateMapper(new ObjectMapper()),
            environmentService,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createApprovalStateDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(environmentService.organizationIdOf("env-1")).thenReturn("org-1");
    }

    @Test
    void should_run_after_the_approval_rules_and_the_apis() {
        assertThat(synchronizer.order()).isEqualTo(Order.APPROVAL_STATE.index());
    }

    @Test
    void should_only_fetch_publish_events_on_initial_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(-1L),
            eq(100L),
            eq(Event.EventProperties.APPROVAL_REQUEST_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_APPROVAL_STATE))
        );
    }

    @Test
    void should_fetch_publish_and_unpublish_events_on_incremental_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(100L),
            eq(200L),
            eq(Event.EventProperties.APPROVAL_REQUEST_ID),
            eq(Set.of("env-1")),
            eq(INCREMENTAL)
        );
    }

    @Test
    void should_deploy_a_published_state_with_its_organization() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publishEvent("req-1", "env-1"))));

        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<ApprovalStateDeployable> captor = ArgumentCaptor.forClass(ApprovalStateDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("req-1");
        assertThat(captor.getValue().organizationId()).isEqualTo("org-1");
        assertThat(captor.getValue().state().apiId()).isEqualTo("api-1");
    }

    @Test
    void should_undeploy_an_unpublished_state() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(event(EventType.UNPUBLISH_APPROVAL_STATE, "{\"approvalRequestId\":\"req-1\"}")))
        );

        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<ApprovalStateDeployable> captor = ArgumentCaptor.forClass(ApprovalStateDeployable.class);
        verify(deployer).undeploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("req-1");
        verify(deployer, never()).deploy(any());
    }

    @Test
    void should_skip_a_state_whose_environment_cannot_be_found() throws InterruptedException {
        when(environmentService.organizationIdOf("env-unknown")).thenReturn(null);
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("req-unknown", "env-unknown"), publishEvent("req-1", "env-1")))
        );

        synchronizer.synchronize(100L, 200L, Set.of("env-1", "env-unknown")).test().await().assertComplete();

        ArgumentCaptor<ApprovalStateDeployable> captor = ArgumentCaptor.forClass(ApprovalStateDeployable.class);
        verify(deployer, times(1)).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("req-1");
    }

    @Test
    void should_complete_when_a_deploy_fails() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("req-1", "env-1"), publishEvent("req-2", "env-1")))
        );
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("error")));

        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
    }

    @Test
    void should_isolate_a_failed_initial_pass_and_run_it_again_as_an_initial_one() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("repository down")))
            .thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(-1L),
            eq(200L),
            eq(Event.EventProperties.APPROVAL_REQUEST_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_APPROVAL_STATE))
        );
    }

    @Test
    void should_isolate_a_failed_incremental_pass_and_fetch_its_window_again() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("repository down")))
            .thenReturn(Flowable.error(new RuntimeException("repository still down")))
            .thenReturn(Flowable.empty())
            .thenReturn(Flowable.empty());

        synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(200L, 300L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(300L, 400L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(400L, 500L, Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(eq(100L), eq(300L), any(), any(), eq(INCREMENTAL));
        verify(fetcher).fetchLatest(eq(100L), eq(400L), any(), any(), eq(INCREMENTAL));
        verify(fetcher).fetchLatest(eq(400L), eq(500L), any(), any(), eq(INCREMENTAL));
    }

    /** From the latest events to what the policy reads, through the real deployer and state manager. */
    @Nested
    class IntoTheStateManager {

        private ApprovalStateManagerImpl manager;

        @BeforeEach
        void useARealManager() {
            manager = new ApprovalStateManagerImpl(
                new ApprovalFeature(licensedLicenseManager(), true),
                Clock.fixed(Instant.parse("2026-10-31T10:30:00Z"), ZoneOffset.UTC)
            );
            when(deployerFactory.createApprovalStateDeployer()).thenReturn(
                new ApprovalStateDeployer(manager, new NoopDistributedSyncService())
            );
        }

        @Test
        void a_published_state_is_found_by_id_and_open_key_then_gone_once_unpublished() throws InterruptedException {
            when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
                .thenReturn(Flowable.just(List.of(publishEvent("req-1", "env-1"))))
                .thenReturn(Flowable.just(List.of(event(EventType.UNPUBLISH_APPROVAL_STATE, "{\"approvalRequestId\":\"req-1\"}"))));

            synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();

            assertThat(Optional.ofNullable(manager.byId("req-1").blockingGet())).hasValueSatisfying(state ->
                assertThat(state.status()).isEqualTo("PENDING")
            );
            assertThat(Optional.ofNullable(manager.byOpenKey("open-1").blockingGet())).hasValueSatisfying(state ->
                assertThat(state.id()).isEqualTo("req-1")
            );

            synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

            assertThat(Optional.ofNullable(manager.byId("req-1").blockingGet())).isEmpty();
            assertThat(Optional.ofNullable(manager.byOpenKey("open-1").blockingGet())).isEmpty();
        }

        @Test
        void a_started_call_carries_when_it_started_to_the_policy() throws InterruptedException {
            when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
                Flowable.just(
                    List.of(
                        event(
                            EventType.PUBLISH_APPROVAL_STATE,
                            "{\"approvalRequestId\":\"req-1\",\"environmentId\":\"env-1\",\"apiId\":\"api-1\",\"ruleId\":\"rule-1\"," +
                                "\"ruleUpdatedAt\":\"2026-10-01T00:00:00Z\",\"status\":\"EXECUTING\",\"keepUntil\":\"2026-10-31T12:00:00Z\"," +
                                "\"createdAt\":\"2026-10-31T09:00:00Z\",\"startedAt\":\"2026-10-31T10:02:00Z\",\"updatedAt\":\"2026-10-31T10:05:00Z\"," +
                                "\"version\":4}"
                        )
                    )
                )
            );

            synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();

            assertThat(Optional.ofNullable(manager.byId("req-1").blockingGet())).hasValueSatisfying(state -> {
                assertThat(state.startedAt()).isEqualTo(Instant.parse("2026-10-31T10:02:00Z").toEpochMilli());
                assertThat(state.updatedAt()).isEqualTo(Instant.parse("2026-10-31T10:05:00Z").toEpochMilli());
            });
        }

        private LicenseManager licensedLicenseManager() {
            LicenseManager licenseManager = mock(LicenseManager.class);
            License license = mock(License.class);
            when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
            when(license.isFeatureEnabled(any())).thenReturn(true);
            return licenseManager;
        }
    }

    private static Event publishEvent(String approvalRequestId, String environmentId) {
        return event(
            EventType.PUBLISH_APPROVAL_STATE,
            "{\"approvalRequestId\":\"" +
                approvalRequestId +
                "\",\"environmentId\":\"" +
                environmentId +
                "\",\"apiId\":\"api-1\",\"ruleId\":\"rule-1\",\"ruleUpdatedAt\":\"2026-10-01T00:00:00Z\",\"openKey\":\"open-1\"," +
                "\"status\":\"PENDING\",\"expiresAt\":\"2026-10-31T11:00:00Z\",\"keepUntil\":\"2026-10-31T12:00:00Z\",\"createdAt\":\"2026-10-31T09:00:00Z\"," +
                "\"updatedAt\":\"2026-10-31T10:00:00Z\"}"
        );
    }

    private static Event event(EventType type, String payload) {
        Event event = new Event();
        event.setId("evt-" + type);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }
}
