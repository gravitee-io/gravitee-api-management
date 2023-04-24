/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class DistributedSyncStateRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    DistributedSyncStateRepository distributedSyncStateRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/distributedsyncstate-tests/";
    }

    @Override
    protected String getModelPackage() {
        return "io.gravitee.repository.distributedsync.model.";
    }

    @Override
    protected void createModel(Object object) {
        DistributedSyncState distributedSyncState = (DistributedSyncState) object;
        distributedSyncStateRepository.createOrUpdate(distributedSyncState).blockingAwait();

        log.info("Created {}", distributedSyncState);
    }

    @Test
    public void should_return_state_from_existing_clusterId() throws InterruptedException {
        log.info("should_return_state_from_existing_clusterId");
        distributedSyncStateRepository
            .findByClusterId("clusterId")
            .test()
            .await()
            .assertValue(distributedSyncState -> {
                assertThat(distributedSyncState).isNotNull();
                assertThat(distributedSyncState.getClusterId()).isEqualTo("clusterId");
                assertThat(distributedSyncState.getNodeId()).isEqualTo("nodeId");
                assertThat(distributedSyncState.getNodeVersion()).isEqualTo("nodeVersion");
                assertThat(distributedSyncState.getFrom()).isEqualTo(1683877018L);
                assertThat(distributedSyncState.getTo()).isEqualTo(1683877618L);
                return true;
            });
    }

    @Test
    public void should_not_return_state_from_wrong_clusterId() throws InterruptedException {
        distributedSyncStateRepository.findByClusterId("wrong").test().await().assertComplete();
    }
}
