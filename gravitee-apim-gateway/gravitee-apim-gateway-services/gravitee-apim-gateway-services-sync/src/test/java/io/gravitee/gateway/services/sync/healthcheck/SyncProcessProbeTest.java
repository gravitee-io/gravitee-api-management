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
package io.gravitee.gateway.services.sync.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.node.api.healthcheck.Result;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class SyncProcessProbeTest {

    @Mock
    SyncManager syncManager1;

    @Mock
    SyncManager syncManager2;

    @Test
    void should_check_completed_with_healthy_state_when_sync_managers_are_done() {
        when(syncManager1.syncDone()).thenReturn(true);
        when(syncManager2.syncDone()).thenReturn(true);
        SyncProcessProbe syncProcessProbe = new SyncProcessProbe(List.of(syncManager1, syncManager2));
        assertThat(syncProcessProbe.check()).isCompletedWithValue(Result.healthy());
    }

    @Test
    void should_check_completed_with_unhealthy_state_when_all_sync_managers_are_not_done() {
        when(syncManager1.syncDone()).thenReturn(true);
        when(syncManager2.syncDone()).thenReturn(false);
        SyncProcessProbe syncProcessProbe = new SyncProcessProbe(List.of(syncManager1, syncManager2));
        assertThat(syncProcessProbe.check()).isCompletedWithValue(Result.notReady());
    }

    @Test
    void should_not_be_visible() {
        SyncProcessProbe syncProcessProbe = new SyncProcessProbe(List.of());
        assertThat(syncProcessProbe.isVisibleByDefault()).isFalse();
    }
}
