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
package io.gravitee.gateway.services.sync.process.deployer;

import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.services.sync.process.model.ApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.synchronizer.apikey.SingleApikeyDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyDeployer implements Deployer<ApiKeyDeployable> {

    private final ApiKeyService apiKeyService;

    @Override
    public Completable deploy(final ApiKeyDeployable deployable) {
        return Completable.fromRunnable(() -> {
            if (deployable.apiKeys() != null) {
                deployable
                    .apiKeys()
                    .forEach(apiKey -> {
                        try {
                            apiKeyService.register(apiKey);
                            log.debug(
                                "ApiKey [{}] of subscription [{}] deployed for api [{}] ",
                                apiKey.getId(),
                                apiKey.getSubscription(),
                                apiKey.getApi()
                            );
                        } catch (Exception e) {
                            log.warn("An error occurred when trying to deploy ApiKey [{}].", apiKey.getId(), e);
                        }
                    });
            }
        });
    }

    @Override
    public Completable undeploy(final ApiKeyDeployable deployable) {
        return Completable.fromRunnable(() -> {
            if (deployable instanceof ApiReactorDeployable) {
                undeployApiReactor((ApiReactorDeployable) deployable);
            } else if (deployable instanceof SingleApikeyDeployable) {
                undeployApiKey((SingleApikeyDeployable) deployable);
            }
        });
    }

    private void undeployApiReactor(final ApiReactorDeployable apiReactorDeployable) {
        try {
            apiKeyService.unregisterByApiId(apiReactorDeployable.apiId());
            log.debug("ApiKeys undeployed for api [{}] ", apiReactorDeployable.apiId());
        } catch (Exception e) {
            log.warn("An error occurred when trying to undeploy apiKeys from api [{}].", apiReactorDeployable.apiId(), e);
        }
    }

    private void undeployApiKey(final SingleApikeyDeployable singleApikeyDeployable) {
        try {
            apiKeyService.unregister(singleApikeyDeployable.apiKey());
            log.debug("ApiKey [{}] undeployed for api [{}] ", singleApikeyDeployable.id(), singleApikeyDeployable.apiId());
        } catch (Exception e) {
            log.warn("An error occurred when trying to undeploy apiKey [{}].", singleApikeyDeployable.id(), e);
        }
    }
}
