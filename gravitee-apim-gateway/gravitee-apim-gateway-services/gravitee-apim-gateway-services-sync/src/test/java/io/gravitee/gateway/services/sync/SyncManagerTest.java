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
package io.gravitee.gateway.services.sync;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.services.sync.synchronizer.ApiSynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.DictionarySynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.OrganizationSynchronizer;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncManagerTest {

    @InjectMocks
    private SyncManager syncManager = new SyncManager();

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private ApiSynchronizer apiSynchronizer;

    @Mock
    private DictionarySynchronizer dictionarySynchronizer;

    @Mock
    private OrganizationSynchronizer organizationSynchronizer;

    private List<String> environments;

    @Mock
    private ThreadPoolExecutor executor;

    @Before
    public void setUp() {
        when(clusterManager.isMasterNode()).thenReturn(true);
        environments = Arrays.asList("DEFAULT", "ENVIRONMENT_2");
    }

    @Test
    public void shouldNotSync_notMasterNode() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        syncManager.setDistributed(true);
        syncManager.setEnvironments(this.environments);
        syncManager.refresh();

        verify(apiSynchronizer, never()).synchronize(anyLong(), anyLong(), anyList());
        verify(dictionarySynchronizer, never()).synchronize(anyLong(), anyLong(), anyList());
        verify(organizationSynchronizer, never()).synchronize(anyLong(), anyLong(), anyList());
    }

    @Test
    public void shouldSync_notMasterNode_notDistributed() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        syncManager.setDistributed(false);

        syncManager.setEnvironments(this.environments);
        syncManager.refresh();

        verify(apiSynchronizer, times(1)).synchronize(anyLong(), anyLong(), anyList());
        verify(dictionarySynchronizer, times(1)).synchronize(anyLong(), anyLong(), anyList());
        verify(organizationSynchronizer, times(1)).synchronize(anyLong(), anyLong(), anyList());
    }
}
