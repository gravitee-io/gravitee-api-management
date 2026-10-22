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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.guardian;

import static io.gravitee.repository.management.model.Event.EventProperties.GUARDIAN_ID;
import static io.gravitee.repository.management.model.EventType.DEPLOY_GUARDIAN;
import static io.gravitee.repository.management.model.EventType.UNDEPLOY_GUARDIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.GuardianDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.GuardianMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class GuardianSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private LatestEventFetcher latestEventFetcher;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private GuardianDeployer guardianDeployer;

    private GuardianSynchronizer cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianSynchronizer(
            latestEventFetcher,
            new GuardianMapper(objectMapper, environmentService),
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(latestEventFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createGuardianDeployer()).thenReturn(guardianDeployer);
        lenient().when(guardianDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(guardianDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(guardianDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(guardianDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_run_after_shared_policy_groups_and_before_apis() {
        assertThat(cut.order()).isEqualTo(Order.GUARDIAN.index());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_anything_when_no_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(guardianDeployer);
        }

        @Test
        void should_ignore_events_of_an_unknown_type() throws InterruptedException {
            Event event = new Event();
            event.setId("event");
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(guardianDeployer);
        }

        @Test
        void should_fetch_only_deploy_events_on_initial_sync() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();

            verify(latestEventFetcher).fetchLatest(eq(-1L), any(), eq(GUARDIAN_ID), eq(Set.of("env")), eq(Set.of(DEPLOY_GUARDIAN)));
        }

        @Test
        void should_fetch_deploy_and_undeploy_events_on_incremental_sync() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();

            verify(latestEventFetcher).fetchLatest(
                any(),
                any(),
                eq(GUARDIAN_ID),
                eq(Set.of("env")),
                eq(Set.of(DEPLOY_GUARDIAN, UNDEPLOY_GUARDIAN))
            );
        }
    }

    @Nested
    class EventTest {

        @Test
        void should_deploy_guardian_carried_by_a_deploy_event() throws InterruptedException, JsonProcessingException {
            Event event = new Event();
            event.setId("id");
            event.setType(DEPLOY_GUARDIAN);
            event.setPayload(
                objectMapper.writeValueAsString(
                    Guardian.builder()
                        .id("guardian-id")
                        .name("name")
                        .environmentId("env")
                        .phase(Guardian.Phase.REQUEST)
                        .policies(List.of())
                        .build()
                )
            );
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            ArgumentCaptor<GuardianReactorDeployable> deployableCaptor = ArgumentCaptor.forClass(GuardianReactorDeployable.class);
            verify(guardianDeployer).deploy(deployableCaptor.capture());
            assertThat(deployableCaptor.getValue().syncAction()).isEqualTo(SyncAction.DEPLOY);
            assertThat(deployableCaptor.getValue().guardianId()).isEqualTo("guardian-id");
            assertThat(deployableCaptor.getValue().reactableGuardian().getEnvironmentId()).isEqualTo("env");
            verify(guardianDeployer).doAfterDeployment(any());
        }

        @Test
        void should_skip_a_deploy_event_whose_payload_is_not_a_guardian() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setType(DEPLOY_GUARDIAN);
            event.setPayload("not a guardian");
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(guardianDeployer);
        }

        @Test
        void should_undeploy_the_environment_carried_by_the_event() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setType(UNDEPLOY_GUARDIAN);
            event.setEnvironments(Set.of("env"));
            event.setProperties(Map.of(GUARDIAN_ID.getValue(), "guardian-id"));
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();

            ArgumentCaptor<GuardianReactorDeployable> deployableCaptor = ArgumentCaptor.forClass(GuardianReactorDeployable.class);
            verify(guardianDeployer).undeploy(deployableCaptor.capture());
            assertThat(deployableCaptor.getValue().syncAction()).isEqualTo(SyncAction.UNDEPLOY);
            assertThat(deployableCaptor.getValue().guardianId()).isEqualTo("guardian-id");
            assertThat(deployableCaptor.getValue().environmentId()).isEqualTo("env");
            assertThat(deployableCaptor.getValue().allEnvironments()).isFalse();
        }

        @Test
        void should_undeploy_every_copy_when_the_event_lists_no_environment() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setType(UNDEPLOY_GUARDIAN);
            event.setProperties(Map.of(GUARDIAN_ID.getValue(), "guardian-id"));
            event.setPayload("description text, not a definition");
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            ArgumentCaptor<GuardianReactorDeployable> deployableCaptor = ArgumentCaptor.forClass(GuardianReactorDeployable.class);
            verify(guardianDeployer).undeploy(deployableCaptor.capture());
            assertThat(deployableCaptor.getValue().guardianId()).isEqualTo("guardian-id");
            assertThat(deployableCaptor.getValue().allEnvironments()).isTrue();
        }

        @Test
        void should_undeploy_each_environment_listed_on_the_event() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setType(UNDEPLOY_GUARDIAN);
            event.setEnvironments(Set.of("env-a", "env-b"));
            event.setProperties(Map.of(GUARDIAN_ID.getValue(), "guardian-id"));
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            ArgumentCaptor<GuardianReactorDeployable> deployableCaptor = ArgumentCaptor.forClass(GuardianReactorDeployable.class);
            verify(guardianDeployer, times(2)).undeploy(deployableCaptor.capture());
            assertThat(deployableCaptor.getAllValues())
                .extracting(GuardianReactorDeployable::environmentId)
                .containsExactlyInAnyOrder("env-a", "env-b");
        }

        @Test
        void should_ignore_an_undeploy_event_without_guardian_id() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setType(UNDEPLOY_GUARDIAN);
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));

            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(guardianDeployer);
        }
    }
}
