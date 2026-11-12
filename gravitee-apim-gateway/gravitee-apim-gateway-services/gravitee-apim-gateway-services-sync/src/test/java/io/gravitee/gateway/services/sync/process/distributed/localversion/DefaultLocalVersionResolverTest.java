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
package io.gravitee.gateway.services.sync.process.distributed.localversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventKey;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedLocalVersion;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.management.model.Subscription;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultLocalVersionResolverTest {

    private static final String CLUSTER = "cluster";
    private static final String RECORDS_CLUSTER = DistributedLocalVersion.recordsClusterId(CLUSTER);
    private static final long BASE = 1_000L;

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private DistributedEventRepository repository;

    @Mock
    private ClusterManager clusterManager;

    private DefaultLocalVersionResolver cut;

    @BeforeEach
    void setUp() {
        lenient().when(clusterManager.clusterId()).thenReturn(CLUSTER);
        lenient().when(repository.createOrUpdate(any())).thenReturn(Completable.complete());
        cut = new DefaultLocalVersionResolver(
            repository,
            clusterManager,
            objectMapper,
            new ApiMapper(objectMapper, new SubscriptionMapper(objectMapper), new ApiKeyMapper(objectMapper)),
            new DictionaryMapper(objectMapper)
        );
    }

    private static Api api(final String revision, final long deployedAt, final boolean enabled) {
        var definition = new io.gravitee.definition.model.v4.Api();
        definition.setId("api");
        definition.setName("api");
        definition.setType(ApiType.PROXY);
        var api = new Api(definition);
        api.setRevision(revision);
        api.setDeployedAt(new Date(deployedAt));
        api.setEnabled(enabled);
        return api;
    }

    private void givenApiLocalVersion(final Api local, final boolean running, final DistributedSyncAction recordAction) throws Exception {
        var record = DistributedLocalVersion.builder()
            .baseDeployedAt(BASE)
            .localPayload(objectMapper.writeValueAsString(local))
            .running(running)
            .build();
        when(repository.find(recordKey(DistributedEventType.API, "api"))).thenReturn(
            Maybe.just(DistributedEvent.builder().payload(objectMapper.writeValueAsString(record)).syncAction(recordAction).build())
        );
    }

    @Nested
    class ApiDeployment {

        @Test
        void should_keep_the_control_plane_version_when_there_is_no_local_version() {
            when(repository.find(recordKey(DistributedEventType.API, "api"))).thenReturn(Maybe.empty());
            var controlPlane = api("12", BASE, true);

            cut.resolveDeployment(controlPlane).test().assertValue(controlPlane);
        }

        @Test
        void should_ignore_a_closed_local_version() throws Exception {
            givenApiLocalVersion(api("local-1", BASE + 1, true), true, DistributedSyncAction.UNDEPLOY);
            var controlPlane = api("12", BASE, true);

            cut.resolveDeployment(controlPlane).test().assertValue(controlPlane);
        }

        @Test
        void should_replace_the_local_version_and_close_its_record_when_the_deployment_is_newer() throws Exception {
            givenApiLocalVersion(api("local-1", BASE + 1, true), true, DistributedSyncAction.DEPLOY);
            var controlPlane = api("13", BASE + 60_000, true);

            cut.resolveDeployment(controlPlane).test().assertValue(controlPlane);

            var closed = ArgumentCaptor.forClass(DistributedEvent.class);
            verify(repository).createOrUpdate(closed.capture());
            assertThat(closed.getValue().getClusterId()).isEqualTo(RECORDS_CLUSTER);
            assertThat(closed.getValue().getId()).isEqualTo("api");
            assertThat(closed.getValue().getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        }

        @Test
        void should_use_the_local_definition_when_the_replayed_deployment_is_not_newer() throws Exception {
            givenApiLocalVersion(api("local-1", BASE + 1, true), true, DistributedSyncAction.DEPLOY);

            cut
                .resolveDeployment(api("12", BASE, true))
                .test()
                .assertValue(resolved -> "local-1".equals(resolved.getRevision()) && resolved.enabled());
            verify(repository, never()).createOrUpdate(any());
        }

        @Test
        void should_ignore_a_replayed_deployment_when_the_local_version_stops_the_api() throws Exception {
            givenApiLocalVersion(api("local-2", BASE + 2, false), false, DistributedSyncAction.DEPLOY);

            cut.resolveDeployment(api("12", BASE, true)).test().assertComplete().assertNoValues();
        }

        @Test
        void should_fail_rather_than_deploy_when_the_record_cannot_be_read() {
            when(repository.find(recordKey(DistributedEventType.API, "api"))).thenReturn(Maybe.error(new RuntimeException("redis down")));

            cut.resolveDeployment(api("12", BASE, true)).test().assertError(RuntimeException.class);
        }
    }

    @Nested
    class ApiUndeployment {

        @Test
        void should_undeploy_and_close_the_record_when_the_stop_is_newer() throws Exception {
            givenApiLocalVersion(api("local-1", BASE + 1, true), true, DistributedSyncAction.DEPLOY);

            cut.resolveUndeployment("api", new Date(BASE + 60_000)).test().assertValue("api");

            verify(repository).createOrUpdate(any());
        }

        @Test
        void should_ignore_an_old_stop_replayed_by_the_margin() throws Exception {
            givenApiLocalVersion(api("local-1", BASE + 1, true), true, DistributedSyncAction.DEPLOY);

            cut.resolveUndeployment("api", new Date(BASE - 60_000)).test().assertComplete().assertNoValues();
        }
    }

    @Nested
    class DictionaryDeployment {

        private Dictionary dictionary(final long deployedAt) {
            var dictionary = new Dictionary();
            dictionary.setId("dict");
            dictionary.setDeployedAt(new Date(deployedAt));
            return dictionary;
        }

        private void givenLocalVersion() throws Exception {
            var record = DistributedLocalVersion.builder()
                .baseDeployedAt(BASE)
                .localPayload(objectMapper.writeValueAsString(dictionary(BASE + 1)))
                .running(true)
                .build();
            when(repository.find(recordKey(DistributedEventType.DICTIONARY, "dict"))).thenReturn(
                Maybe.just(
                    DistributedEvent.builder()
                        .payload(objectMapper.writeValueAsString(record))
                        .syncAction(DistributedSyncAction.DEPLOY)
                        .build()
                )
            );
        }

        @Test
        void should_replace_the_local_version_when_the_deployment_is_newer() throws Exception {
            givenLocalVersion();
            var controlPlane = dictionary(BASE + 60_000);

            cut.resolveDeployment(controlPlane).test().assertValue(controlPlane);

            verify(repository).createOrUpdate(any());
        }

        @Test
        void should_use_the_local_values_when_the_deployment_is_not_newer() throws Exception {
            givenLocalVersion();

            cut
                .resolveDeployment(dictionary(BASE))
                .test()
                .assertValue(resolved -> resolved.getDeployedAt().getTime() == BASE + 1);
        }

        @Test
        void should_keep_the_local_version_when_the_deployment_has_no_date() throws Exception {
            givenLocalVersion();
            var undated = new Dictionary();
            undated.setId("dict");

            cut
                .resolveDeployment(undated)
                .test()
                .assertValue(resolved -> resolved.getDeployedAt().getTime() == BASE + 1);
            verify(repository, never()).createOrUpdate(any());
        }
    }

    @Nested
    class SubscriptionResolution {

        private Subscription subscription(final String id, final Date updatedAt) {
            var subscription = new Subscription();
            subscription.setId(id);
            subscription.setStatus(Subscription.Status.ACCEPTED);
            subscription.setUpdatedAt(updatedAt);
            return subscription;
        }

        private void givenLocalVersions(final String... subscriptionIds) throws Exception {
            var record = objectMapper.writeValueAsString(DistributedLocalVersion.builder().baseDeployedAt(BASE).running(false).build());
            var events = new ArrayList<DistributedEvent>();
            for (String id : subscriptionIds) {
                events.add(DistributedEvent.builder().id(id).payload(record).syncAction(DistributedSyncAction.DEPLOY).build());
            }
            when(
                repository.searchAll(
                    argThat(
                        criteria ->
                            RECORDS_CLUSTER.equals(criteria.getClusterId()) &&
                            criteria.getType() == DistributedEventType.SUBSCRIPTION &&
                            Set.of(DistributedSyncAction.DEPLOY).equals(criteria.getSyncActions())
                    ),
                    anyLong()
                )
            ).thenReturn(Flowable.fromIterable(events));
        }

        @Test
        void should_keep_every_subscription_when_there_is_no_local_version() throws Exception {
            givenLocalVersions();
            var subscriptions = List.of(subscription("sub-1", new Date(BASE)), subscription("sub-2", new Date(BASE)));

            cut.subscriptionLocalVersions().resolve(subscriptions).test().assertValue(subscriptions);
        }

        @Test
        void should_drop_a_subscription_the_control_plane_has_not_changed_since_the_local_change() throws Exception {
            givenLocalVersions("sub-paused");
            var untouched = subscription("sub-1", new Date(BASE));

            cut
                .subscriptionLocalVersions()
                .resolve(List.of(untouched, subscription("sub-paused", new Date(BASE - 60_000))))
                .test()
                .assertValue(List.of(untouched));
            verify(repository, never()).createOrUpdate(any());
        }

        @Test
        void should_keep_a_subscription_and_close_its_local_version_when_the_control_plane_changed_it_later() throws Exception {
            givenLocalVersions("sub-paused");
            var changedOnControlPlane = subscription("sub-paused", new Date(BASE + 60_000));

            cut.subscriptionLocalVersions().resolve(List.of(changedOnControlPlane)).test().assertValue(List.of(changedOnControlPlane));

            var closed = ArgumentCaptor.forClass(DistributedEvent.class);
            verify(repository).createOrUpdate(closed.capture());
            assertThat(closed.getValue().getClusterId()).isEqualTo(RECORDS_CLUSTER);
            assertThat(closed.getValue().getType()).isEqualTo(DistributedEventType.SUBSCRIPTION);
            assertThat(closed.getValue().getId()).isEqualTo("sub-paused");
            assertThat(closed.getValue().getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        }

        @Test
        void should_drop_a_subscription_with_a_local_version_when_the_control_plane_gives_no_date() throws Exception {
            givenLocalVersions("sub-paused");

            cut.subscriptionLocalVersions().resolve(List.of(subscription("sub-paused", null))).test().assertValue(List.of());
        }

        @Test
        void should_read_local_versions_once_for_every_page_of_a_load() throws Exception {
            givenLocalVersions("sub-paused");
            var localVersions = cut.subscriptionLocalVersions();
            var untouched = subscription("sub-1", new Date(BASE));

            localVersions.resolve(List.of(untouched)).test().assertValue(List.of(untouched));
            localVersions.resolve(List.of(subscription("sub-paused", new Date(BASE)))).test().assertValue(List.of());

            verify(repository, times(1)).searchAll(any(), anyLong());
        }

        @Test
        void should_not_read_local_versions_when_there_are_no_subscriptions() {
            cut.subscriptionLocalVersions().resolve(List.of()).test().assertValue(List.of());

            verifyNoInteractions(repository);
        }

        @Test
        void should_fail_rather_than_apply_subscriptions_when_local_versions_cannot_be_read() {
            when(repository.searchAll(any(), anyLong())).thenReturn(Flowable.error(new RuntimeException("redis down")));

            cut
                .subscriptionLocalVersions()
                .resolve(List.of(subscription("sub-1", new Date(BASE))))
                .test()
                .assertError(RuntimeException.class);
        }
    }

    private static DistributedEventKey recordKey(final DistributedEventType type, final String id) {
        return DistributedEventKey.builder().clusterId(RECORDS_CLUSTER).type(type).id(id).build();
    }
}
