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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleManagerImpl;
import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalRuleDeployer;
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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalRuleSynchronizerTest {

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApprovalRuleDeployer deployer;

    @Mock
    private EnvironmentService environmentService;

    private ApprovalRuleSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new ApprovalRuleSynchronizer(
            fetcher,
            new ApprovalRuleMapper(new ObjectMapper()),
            environmentService,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createApprovalRuleDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.commit()).thenReturn(Completable.complete());
        lenient().when(environmentService.organizationIdOf("env-1")).thenReturn("org-1");
    }

    @Test
    void should_use_the_approval_rule_order() {
        assertThat(synchronizer.order()).isEqualTo(Order.APPROVAL_RULE.index());
    }

    @Test
    void should_only_fetch_publish_events_on_initial_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(-1L),
            any(),
            eq(Event.EventProperties.APPROVAL_RULE_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_APPROVAL_RULE))
        );
    }

    @Test
    void should_fetch_publish_and_unpublish_events_on_incremental_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(123L),
            any(),
            eq(Event.EventProperties.APPROVAL_RULE_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_APPROVAL_RULE, EventType.UNPUBLISH_APPROVAL_RULE))
        );
    }

    @Test
    void should_deploy_a_published_approval_rule_with_its_organization() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("approval-rule-1", "env-1")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<ApprovalRuleDeployable> captor = ArgumentCaptor.forClass(ApprovalRuleDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("approval-rule-1");
        assertThat(captor.getValue().environmentId()).isEqualTo("env-1");
        assertThat(captor.getValue().organizationId()).isEqualTo("org-1");
    }

    @Test
    void should_undeploy_an_unpublished_approval_rule() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(
                List.of(event("evt-u", EventType.UNPUBLISH_APPROVAL_RULE, "{\"id\":\"approval-rule-1\",\"environmentId\":\"env-1\"}"))
            )
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<ApprovalRuleDeployable> captor = ArgumentCaptor.forClass(ApprovalRuleDeployable.class);
        verify(deployer).undeploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("approval-rule-1");
        verify(deployer, never()).deploy(any());
    }

    @Test
    void should_drop_an_unreadable_event_without_breaking_the_batch() throws InterruptedException {
        Event bad = event("evt-bad", EventType.PUBLISH_APPROVAL_RULE, "not-json");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(bad, publishEvent("approval-rule-1", "env-1")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
    }

    @Test
    void should_complete_when_a_deploy_fails() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("approval-rule-1", "env-1"), publishEvent("approval-rule-2", "env-1")))
        );
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("error")));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
    }

    @Test
    void should_skip_an_approval_rule_whose_environment_cannot_be_found() throws InterruptedException {
        when(environmentService.organizationIdOf("env-unknown")).thenReturn(null);
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("approval-rule-unknown", "env-unknown"), publishEvent("approval-rule-1", "env-1")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1", "env-unknown")).test().await().assertComplete();

        ArgumentCaptor<ApprovalRuleDeployable> captor = ArgumentCaptor.forClass(ApprovalRuleDeployable.class);
        verify(deployer, times(1)).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("approval-rule-1");
    }

    @Test
    void should_commit_once_for_every_rule_of_the_pass() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(
                IntStream.range(0, 100)
                    .mapToObj(i -> publishEvent("approval-rule-" + i, "env-1"))
                    .toList()
            )
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        InOrder inOrder = inOrder(deployer);
        inOrder.verify(deployer, times(100)).deploy(any());
        inOrder.verify(deployer, times(1)).commit();
    }

    @Test
    void should_fail_an_initial_sync_whose_rules_cannot_be_fetched() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("repository down")));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertError(RuntimeException.class);

        verify(deployer, never()).commit();
    }

    @Nested
    class IncrementalFailure {

        private final ApprovalRuleManagerImpl manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licensedLicenseManager(), true));

        @BeforeEach
        void useARealManager() {
            when(deployerFactory.createApprovalRuleDeployer()).thenReturn(
                new ApprovalRuleDeployer(manager, new NoopDistributedSyncService())
            );
        }

        @Test
        void should_keep_the_committed_rules_and_fetch_the_missed_window_again_on_the_next_pass() throws InterruptedException {
            when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
                .thenReturn(Flowable.just(List.of(publishEvent("approval-rule-1", "env-1"))))
                .thenReturn(Flowable.error(new RuntimeException("repository down")))
                .thenReturn(Flowable.just(List.of(publishEvent("approval-rule-2", "env-1"))));
            synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();

            synchronizer.synchronize(100L, 200L, Set.of("env-1")).test().await().assertComplete();

            assertThat(manager.candidates("env-1", "api-1", "payments", null))
                .extracting(DeployedApprovalRule::id)
                .containsExactly("approval-rule-1");

            synchronizer.synchronize(200L, 300L, Set.of("env-1")).test().await().assertComplete();

            verify(fetcher).fetchLatest(
                eq(100L),
                eq(300L),
                eq(Event.EventProperties.APPROVAL_RULE_ID),
                eq(Set.of("env-1")),
                eq(Set.of(EventType.PUBLISH_APPROVAL_RULE, EventType.UNPUBLISH_APPROVAL_RULE))
            );
            assertThat(manager.candidates("env-1", "api-1", "payments", null))
                .extracting(DeployedApprovalRule::id)
                .containsExactlyInAnyOrder("approval-rule-1", "approval-rule-2");
        }

        /** Until a pass loaded the rules, "no rule" does not mean no call is covered: the policy refuses calls. */
        @Test
        void should_receive_the_rules_only_once_a_pass_loaded_them_even_when_there_are_none() throws InterruptedException {
            when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
                .thenReturn(Flowable.error(new RuntimeException("repository down")))
                .thenReturn(Flowable.empty());

            synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertError(RuntimeException.class);
            assertThat(manager.receivesRules("org-1")).isFalse();

            synchronizer.synchronize(-1L, 100L, Set.of("env-1")).test().await().assertComplete();
            assertThat(manager.receivesRules("org-1")).isTrue();
            assertThat(manager.hasRules("env-1")).isFalse();
        }

        private LicenseManager licensedLicenseManager() {
            LicenseManager licenseManager = mock(LicenseManager.class);
            License license = mock(License.class);
            when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
            when(license.isFeatureEnabled(any())).thenReturn(true);
            return licenseManager;
        }
    }

    private static Event publishEvent(String approvalRuleId, String environmentId) {
        return event(
            "evt-" + approvalRuleId,
            EventType.PUBLISH_APPROVAL_RULE,
            "{\"id\":\"" +
                approvalRuleId +
                "\",\"environmentId\":\"" +
                environmentId +
                "\",\"toolPattern\":\"payments\",\"priority\":10,\"createdAt\":\"2026-09-14T00:00:00Z\",\"updatedAt\":\"2026-09-15T00:00:00Z\"}"
        );
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        event.setUpdatedAt(new Date());
        return event;
    }
}
