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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.genericevent.GenericEventManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventDeployable;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenericEventDeployerTest {

    private static final GenericEvent CONFIG = new GenericEvent("aihub.hub", "hub-1", "env-1", "org-1", null, null, "{}");

    @Mock
    private GenericEventManager genericEventManager;

    @Mock
    private DistributedSyncService distributedSyncService;

    private GenericEventDeployer cut;

    @BeforeEach
    void beforeEach() {
        cut = new GenericEventDeployer(genericEventManager, distributedSyncService);
    }

    @Test
    void should_upsert_the_event() {
        cut.deploy(new GenericEventDeployable("event-1", SyncAction.DEPLOY, CONFIG)).test().assertComplete();

        verify(genericEventManager).upsert(CONFIG);
    }

    @Test
    void should_remove_the_event_by_environment_kind_and_id() {
        cut.undeploy(new GenericEventDeployable("event-1", SyncAction.UNDEPLOY, CONFIG)).test().assertComplete();

        verify(genericEventManager).remove("env-1", "aihub.hub", "hub-1");
    }

    @Test
    void should_fail_with_a_sync_exception_when_the_manager_fails() {
        doThrow(new IllegalStateException("boom")).when(genericEventManager).upsert(any());

        cut.deploy(new GenericEventDeployable("event-1", SyncAction.DEPLOY, CONFIG)).test().assertError(SyncException.class);
    }

    @Test
    void should_distribute_after_deployment() {
        var deployable = new GenericEventDeployable("event-1", SyncAction.DEPLOY, CONFIG);
        when(distributedSyncService.distributeIfNeeded(deployable)).thenReturn(Completable.complete());

        cut.doAfterDeployment(deployable).test().assertComplete();

        verify(distributedSyncService).distributeIfNeeded(deployable);
    }

    @Test
    void should_distribute_after_undeployment() {
        var deployable = new GenericEventDeployable("event-1", SyncAction.UNDEPLOY, CONFIG);
        when(distributedSyncService.distributeIfNeeded(deployable)).thenReturn(Completable.complete());

        cut.doAfterUndeployment(deployable).test().assertComplete();

        verify(distributedSyncService).distributeIfNeeded(deployable);
    }
}
