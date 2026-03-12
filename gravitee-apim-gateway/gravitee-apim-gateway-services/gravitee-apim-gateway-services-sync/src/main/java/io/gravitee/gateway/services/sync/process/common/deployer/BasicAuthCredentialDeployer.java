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

import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCacheService;
import io.gravitee.gateway.services.sync.process.common.model.BasicAuthCredentialDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class BasicAuthCredentialDeployer implements Deployer<BasicAuthCredentialDeployable> {

    private final BasicAuthCacheService basicAuthCacheService;

    @Override
    public Completable deploy(final BasicAuthCredentialDeployable deployable) {
        return Completable.fromRunnable(() -> {
            if (deployable.basicAuthCredentials() != null) {
                deployable
                    .basicAuthCredentials()
                    .forEach(credential -> {
                        try {
                            basicAuthCacheService.register(credential);
                            log.debug("BasicAuth credential [{}] deployed for api [{}]", credential.getId(), credential.getApi());
                        } catch (Exception e) {
                            log.warn("An error occurred when trying to deploy BasicAuth credential [{}].", credential.getId(), e);
                        }
                    });
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final BasicAuthCredentialDeployable deployable) {
        return Completable.complete();
    }

    @Override
    public Completable undeploy(final BasicAuthCredentialDeployable deployable) {
        return Completable.fromRunnable(() -> {
            if (deployable instanceof ApiReactorDeployable apiReactorDeployable) {
                try {
                    basicAuthCacheService.unregisterByApiId(apiReactorDeployable.apiId());
                    log.debug("BasicAuth credentials undeployed for api [{}]", apiReactorDeployable.apiId());
                } catch (Exception e) {
                    log.warn(
                        "An error occurred when trying to undeploy BasicAuth credentials from api [{}].",
                        apiReactorDeployable.apiId(),
                        e
                    );
                }
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final BasicAuthCredentialDeployable deployable) {
        return Completable.complete();
    }
}
