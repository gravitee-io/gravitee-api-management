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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.CredentialDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialSynchronizerTest {

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private CredentialDeployer deployer;

    @Mock
    private EnvironmentService environmentService;

    private CredentialSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new CredentialSynchronizer(
            fetcher,
            new CredentialMapper(new ObjectMapper()),
            environmentService,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createCredentialDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(environmentService.organizationIdOf("env-1")).thenReturn("org-1");
    }

    @Test
    void should_use_the_credential_order() {
        assertThat(synchronizer.order()).isEqualTo(Order.CREDENTIAL.index());
    }

    @Test
    void should_only_fetch_publish_events_on_initial_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(-1L),
            any(),
            eq(Event.EventProperties.CREDENTIAL_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_CREDENTIAL))
        );
    }

    @Test
    void should_fetch_publish_and_unpublish_events_on_incremental_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(123L),
            any(),
            eq(Event.EventProperties.CREDENTIAL_ID),
            eq(Set.of("env-1")),
            eq(Set.of(EventType.PUBLISH_CREDENTIAL, EventType.UNPUBLISH_CREDENTIAL))
        );
    }

    @Test
    void should_deploy_a_published_credential_with_its_organization() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publishEvent("credential-1"))));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<CredentialDeployable> captor = ArgumentCaptor.forClass(CredentialDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("credential-1");
        assertThat(captor.getValue().environmentId()).isEqualTo("env-1");
        assertThat(captor.getValue().organizationId()).isEqualTo("org-1");
    }

    @Test
    void should_undeploy_an_unpublished_credential() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(event("evt-u", EventType.UNPUBLISH_CREDENTIAL, "{\"id\":\"credential-1\",\"environmentId\":\"env-1\"}")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<CredentialDeployable> captor = ArgumentCaptor.forClass(CredentialDeployable.class);
        verify(deployer).undeploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("credential-1");
        verify(deployer, never()).deploy(any());
    }

    @Test
    void should_drop_an_unreadable_event_without_breaking_the_batch() throws InterruptedException {
        Event bad = event("evt-bad", EventType.PUBLISH_CREDENTIAL, "not-json");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(bad, publishEvent("credential-1"))));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
    }

    @Test
    void should_complete_when_a_deploy_fails() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(publishEvent("credential-1"), publishEvent("credential-2")))
        );
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("error")));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
    }

    @Test
    void should_skip_a_credential_whose_environment_cannot_be_found() throws InterruptedException {
        when(environmentService.organizationIdOf("env-unknown")).thenReturn(null);
        Event unknownEnvironment = event(
            "evt-unknown",
            EventType.PUBLISH_CREDENTIAL,
            "{\"id\":\"credential-unknown\",\"environmentId\":\"env-unknown\",\"encryptedSecret\":\"ciphertext\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(unknownEnvironment, publishEvent("credential-1")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1", "env-unknown")).test().await().assertComplete();

        ArgumentCaptor<CredentialDeployable> captor = ArgumentCaptor.forClass(CredentialDeployable.class);
        verify(deployer, times(1)).deploy(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("credential-1");
    }

    private static Event publishEvent(String credentialId) {
        return event(
            "evt-" + credentialId,
            EventType.PUBLISH_CREDENTIAL,
            "{\"id\":\"" + credentialId + "\",\"environmentId\":\"env-1\",\"encryptedSecret\":\"ciphertext\"}"
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
