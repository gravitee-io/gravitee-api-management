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
package io.gravitee.repository.distributedsync;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import lombok.CustomLog;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class DistributedEventRepositoryTest extends AbstractRepositoryTest {

    private static final String TEST_CLUSTER_ID = "test-cluster";

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    DistributedEventRepository distributedEventRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/distributedsyncevent-tests/";
    }

    @Override
    protected String getModelPackage() {
        return "io.gravitee.repository.distributedsync.model.";
    }

    @Override
    protected void createModel(Object object) {
        DistributedEvent distributedEvent = (DistributedEvent) object;
        distributedEventRepository.createOrUpdate(distributedEvent).blockingAwait();
        log.info("Created {}", distributedEvent);
    }

    @Test
    public void should_return_all_distributed_event_without_criteria_and_page() throws InterruptedException {
        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).build(), -1L, -1L)
            .test()
            .await()
            .assertValueAt(0, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            })
            .assertValueAt(1, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            })
            .assertValueAt(2, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("3");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.DICTIONARY);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-26T15:25:14.051Z"));
                return true;
            })
            .assertValueAt(3, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("4");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API_KEY);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-06-24T15:24:34.051Z"));
                return true;
            })
            .assertValueAt(4, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("5");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.SUBSCRIPTION);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-06-24T15:25:34.051Z"));
                return true;
            })
            .assertValueAt(5, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("DEFAULT");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.LICENSE);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2024-01-24T16:35:34.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_paginated_distributed_event_without_criteria() throws InterruptedException {
        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).build(), 0L, 1L)
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            });

        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).build(), 1L, 1L)
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            });

        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).build(), 2L, 1L)
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("3");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.DICTIONARY);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-26T15:25:14.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_only_api_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).type(DistributedEventType.API).build(), -1L, -1L)
            .test()
            .await()
            .assertValueAt(0, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            })
            .assertValueAt(1, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_only_deployed_api_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(
                DistributedEventCriteria.builder()
                    .clusterId(TEST_CLUSTER_ID)
                    .type(DistributedEventType.API)
                    .syncActions(Set.of(DistributedSyncAction.DEPLOY))
                    .build(),
                -1L,
                -1L
            )
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_deployed_and_undeployed_api_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(
                DistributedEventCriteria.builder()
                    .clusterId(TEST_CLUSTER_ID)
                    .type(DistributedEventType.API)
                    .syncActions(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
                    .build(),
                -1L,
                -1L
            )
            .test()
            .await()
            .assertValueAt(0, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            })
            .assertValueAt(1, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_updated_after_from_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(
                DistributedEventCriteria.builder()
                    .clusterId(TEST_CLUSTER_ID)
                    .from(Instant.parse("2023-04-25T15:24:40.051Z").toEpochMilli())
                    .build(),
                -1L,
                -1L
            )
            .test()
            .await()
            .assertValueAt(0, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            })
            .assertValueAt(1, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("3");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.DICTIONARY);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-26T15:25:14.051Z"));
                return true;
            })
            .assertValueAt(2, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("4");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API_KEY);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-06-24T15:24:34.051Z"));
                return true;
            })
            .assertValueAt(3, distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("5");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.SUBSCRIPTION);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-06-24T15:25:34.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_updated_before_to_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(
                DistributedEventCriteria.builder()
                    .clusterId(TEST_CLUSTER_ID)
                    .to(Instant.parse("2023-04-25T15:24:40.051Z").toEpochMilli())
                    .build(),
                -1L,
                -1L
            )
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("1");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-24T15:24:34.051Z"));
                return true;
            });
    }

    @Test
    public void should_return_updated_between_from_and_to_distributed_event() throws InterruptedException {
        distributedEventRepository
            .search(
                DistributedEventCriteria.builder()
                    .clusterId(TEST_CLUSTER_ID)
                    .from(Instant.parse("2023-04-24T15:24:40.051Z").toEpochMilli())
                    .to(Instant.parse("2023-04-26T15:25:00.051Z").toEpochMilli())
                    .build(),
                -1L,
                -1L
            )
            .test()
            .await()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent).isNotNull();
                assertThat(distributedEvent.getId()).isEqualTo("2");
                assertThat(distributedEvent.getPayload()).isEqualTo("payload");
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(distributedEvent.getUpdatedAt()).isEqualTo(Instant.parse("2023-04-25T15:24:54.051Z"));
                return true;
            });
    }

    @Test
    public void should_update_all_related_to_api() throws InterruptedException {
        Date updateAt = new Date();
        var testObserver = distributedEventRepository
            .updateAll(TEST_CLUSTER_ID, DistributedEventType.API, "1", DistributedSyncAction.UNDEPLOY, updateAt)
            .andThen(
                distributedEventRepository.search(
                    DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).from(updateAt.getTime()).build(),
                    -1L,
                    -1L
                )
            )
            .test();
        testObserver.await().assertComplete();
        assertThat(testObserver.values()).hasSize(3);
        assertThat(testObserver.values()).extracting(DistributedEvent::getId).containsExactlyInAnyOrder("1", "4", "5");
        assertThat(testObserver.values()).allSatisfy(distributedEvent -> {
            assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
            assertThat(distributedEvent.getUpdatedAt()).isEqualTo(updateAt);
        });
    }

    @Test
    public void should_not_return_events_from_other_cluster() throws InterruptedException {
        distributedEventRepository
            .createOrUpdate(
                DistributedEvent.builder()
                    .clusterId("other-cluster")
                    .id("other-api")
                    .payload("payload")
                    .type(DistributedEventType.API)
                    .syncAction(DistributedSyncAction.DEPLOY)
                    .updatedAt(Date.from(Instant.parse("2023-04-24T15:24:34.051Z")))
                    .build()
            )
            .blockingAwait();

        distributedEventRepository
            .search(DistributedEventCriteria.builder().clusterId(TEST_CLUSTER_ID).type(DistributedEventType.API).build(), -1L, -1L)
            .test()
            .await()
            .assertValueCount(2)
            .assertValueAt(0, distributedEvent -> {
                assertThat(distributedEvent.getId()).isEqualTo("1");
                return true;
            })
            .assertValueAt(1, distributedEvent -> {
                assertThat(distributedEvent.getId()).isEqualTo("2");
                return true;
            });
    }
}
