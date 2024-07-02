/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.gateway.services.sync.process.repository.synchronizer.node;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.NodeMetadataDeployer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.InstallationIdFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.OrganizationIdsFetcher;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
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
class NodeMetadataSynchronizerTest {

    @Mock
    private OrganizationIdsFetcher organizationIdsFetcher;

    @Mock
    private InstallationIdFetcher installationIdFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private NodeMetadataDeployer nodeMetadataDeployer;

    private NodeMetadataSynchronizer cut;

    @BeforeEach
    void setUp() {
        cut =
            new NodeMetadataSynchronizer(
                organizationIdsFetcher,
                installationIdFetcher,
                deployerFactory,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
            );

        lenient().when(deployerFactory.createNodeMetadataDeployer()).thenReturn(nodeMetadataDeployer);
        lenient().when(nodeMetadataDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_add_node_metadata_when_fetching_ids() throws InterruptedException {
        when(organizationIdsFetcher.fetch(any())).thenReturn(Maybe.just(Set.of("orga-id")));
        when(installationIdFetcher.fetch()).thenReturn(Maybe.just("installation-id"));
        cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

        verify(nodeMetadataDeployer).deploy(any());
        verify(nodeMetadataDeployer).doAfterDeployment(any());
    }

    @Test
    void should_not_fetch_incremental() throws InterruptedException {
        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();
        verifyNoInteractions(installationIdFetcher);
        verifyNoInteractions(organizationIdsFetcher);
    }
}
