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
package io.gravitee.gateway.debug.sync;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugSyncManagerTest {

    @InjectMocks
    private final DebugSyncManager syncManager = new DebugSyncManager();

    @Mock
    private EventManager eventManager;

    @Mock
    private DebugApiSynchronizer debugApiSynchronizer;

    private List<String> environments;

    @Before
    public void setUp() {
        environments = Arrays.asList("DEFAULT", "ENVIRONMENT_2");
    }

    @Test
    public void shouldSync() throws TechnicalException {
        syncManager.setEnvironments(this.environments);
        syncManager.refresh();

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));

        verify(debugApiSynchronizer, times(1)).synchronize(anyLong(), anyLong(), anyList());
    }
}
