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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import java.util.Set;
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
class ApiDeployerTest {

    @Mock
    private ApiManager apiManager;

    private ApiDeployer cut;
    private PlanService planService;

    @BeforeEach
    public void beforeEach() {
        planService = new PlanService();
        cut = new ApiDeployer(apiManager, planService, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_api() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(apiManager).register(reactableApi);
        }

        @Test
        void should_return_error_when_api_manager_throw_exception() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            when(apiManager.register(reactableApi)).thenThrow(new SyncException("error"));
            cut.deploy(apiReactorDeployable).test().assertFailure(SyncException.class);
            verify(apiManager).register(reactableApi);
        }

        @Test
        void should_do_post_action() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .subscribablePlans(Set.of("plan"))
                .reactableApi(reactableApi)
                .build();
            cut.doAfterDeployment(apiReactorDeployable).test().assertComplete();
            assertThat(planService.isDeployed("apiId", "plan")).isTrue();
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_api() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(apiManager).unregister("apiId");
        }

        @Test
        void should_complete_on_error_when_api_manager_throw_exception() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            doThrow(new SyncException("error")).when(apiManager).unregister("apiId");
            cut.undeploy(apiReactorDeployable).test().assertFailure(SyncException.class);
            verify(apiManager).unregister("apiId");
        }
    }
}
