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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.dictionary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.DictionaryDeployer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.Date;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedDictionarySynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private DictionaryDeployer dictionaryDeployer;

    private DistributedDictionarySynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DistributedDictionarySynchronizer(
            eventsFetcher,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            deployerFactory,
            new DictionaryMapper(objectMapper)
        );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createDictionaryDeployer()).thenReturn(dictionaryDeployer);
        lenient().when(dictionaryDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(dictionaryDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(dictionaryDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(dictionaryDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_dictionaries_when_no_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.DICTIONARY), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verifyNoInteractions(dictionaryDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher).fetchLatest(
                eq(-1L),
                any(),
                eq(DistributedEventType.DICTIONARY),
                eq(Set.of(DistributedSyncAction.DEPLOY))
            );
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher).fetchLatest(
                any(),
                any(),
                eq(DistributedEventType.DICTIONARY),
                eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
            );
        }
    }

    @Nested
    class DistributedDictionarySynchronizationTest {

        private Dictionary dictionary;

        @BeforeEach
        public void init() {
            dictionary = new Dictionary();
            dictionary.setId("id");
        }

        @Test
        void should_deploy_dictionary_when_fetching_deployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent.builder()
                .id("dictionary")
                .payload(objectMapper.writeValueAsString(dictionary))
                .type(DistributedEventType.DICTIONARY)
                .syncAction(DistributedSyncAction.DEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(dictionaryDeployer).deploy(any());
            verify(dictionaryDeployer).doAfterDeployment(any());
        }

        @Test
        void should_undeploy_dictionary_when_fetching_undeployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent.builder()
                .id("dictionary")
                .payload(objectMapper.writeValueAsString(dictionary))
                .type(DistributedEventType.DICTIONARY)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(dictionaryDeployer).undeploy(any());
            verify(dictionaryDeployer).doAfterUndeployment(any());
        }
    }
}
