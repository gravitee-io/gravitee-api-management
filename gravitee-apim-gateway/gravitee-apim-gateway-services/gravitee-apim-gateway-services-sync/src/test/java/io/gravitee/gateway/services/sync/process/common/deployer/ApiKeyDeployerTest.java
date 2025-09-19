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

import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.ApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import java.util.List;
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
class ApiKeyDeployerTest {

    @Mock
    private ApiKeyService apiKeyService;

    private ApiKeyDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiKeyDeployer(apiKeyService, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_api_keys() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiKey apiKey1 = new ApiKey();
            apiKey1.setId("id1");
            ApiKey apiKey2 = new ApiKey();
            apiKey2.setId("id2");
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(reactableApi)
                .apiKeys(List.of(apiKey1, apiKey2))
                .build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(apiKeyService).register(apiKey1);
            verify(apiKeyService).register(apiKey2);
            verifyNoMoreInteractions(apiKeyService);
        }

        @Test
        void should_ignore_api_keys_in_error() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiKey apiKey1 = new ApiKey();
            apiKey1.setId("id1");
            ApiKey apiKey2 = new ApiKey();
            apiKey2.setId("id2");
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(reactableApi)
                .apiKeys(List.of(apiKey1, apiKey2))
                .build();
            doThrow(new SyncException("error")).when(apiKeyService).register(apiKey1);
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(apiKeyService).register(apiKey1);
            verify(apiKeyService).register(apiKey2);
            verifyNoMoreInteractions(apiKeyService);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment((ApiKeyDeployable) null).test().assertComplete();
            verifyNoInteractions(apiKeyService);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_api_keys_from_api_id() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(apiKeyService).unregisterByApiId("apiId");
            verifyNoMoreInteractions(apiKeyService);
        }

        @Test
        void should_ignore_undeploy_api_key_from_api_in_error() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            doThrow(new SyncException("error")).when(apiKeyService).unregisterByApiId("apiId");
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(apiKeyService).unregisterByApiId("apiId");
            verifyNoMoreInteractions(apiKeyService);
        }

        @Test
        void should_undeploy_api_keys_from_api_key() {
            ApiKey apiKey = new ApiKey();
            SingleApiKeyDeployable apikeyDeployable = SingleApiKeyDeployable.builder().apiKey(apiKey).build();
            cut.undeploy(apikeyDeployable).test().assertComplete();
            verify(apiKeyService).unregister(apiKey);
            verifyNoMoreInteractions(apiKeyService);
        }

        @Test
        void should_ignore_undeploy_api_keys_from_api_key_in_error() {
            ApiKey apiKey = new ApiKey();
            SingleApiKeyDeployable apikeyDeployable = SingleApiKeyDeployable.builder().apiKey(apiKey).build();
            doThrow(new SyncException("error")).when(apiKeyService).unregister(apiKey);
            cut.undeploy(apikeyDeployable).test().assertComplete();
            verify(apiKeyService).unregister(apiKey);
            verifyNoMoreInteractions(apiKeyService);
        }
    }
}
