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

import static org.mockito.Mockito.verify;

import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.accesspoint.model.AccessPoint;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccessPointDeployerTest {

    @Mock
    private AccessPointManager accessPointManager;

    private AccessPointDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new AccessPointDeployer(accessPointManager, new NoopDistributedSyncService());
    }

    @Test
    void should_deploy_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        AccessPointDeployable accessPointDeployable = AccessPointDeployable.builder().reactableAccessPoint(reactableAccessPoint).build();

        cut.deploy(accessPointDeployable).test().assertComplete();
        verify(accessPointManager).register(reactableAccessPoint);
    }

    @Test
    void should_undeploy_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        AccessPointDeployable accessPointDeployable = AccessPointDeployable.builder().reactableAccessPoint(reactableAccessPoint).build();

        cut.undeploy(accessPointDeployable).test().assertComplete();
        verify(accessPointManager).unregister(reactableAccessPoint);
    }
}
