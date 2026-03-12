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
import io.gravitee.gateway.services.sync.process.repository.synchronizer.basicauth.SingleBasicAuthDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class BasicAuthDeployer implements Deployer<SingleBasicAuthDeployable> {

    private final BasicAuthCacheService basicAuthCacheService;

    @Override
    public Completable deploy(final SingleBasicAuthDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                basicAuthCacheService.register(deployable.credential());
                log.debug(
                    "BasicAuth credential [{}] deployed for api [{}]",
                    deployable.credential().getId(),
                    deployable.credential().getApi()
                );
            } catch (Exception e) {
                log.warn("An error occurred when trying to deploy BasicAuth credential [{}].", deployable.credential().getId(), e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final SingleBasicAuthDeployable deployable) {
        return Completable.complete();
    }

    @Override
    public Completable undeploy(final SingleBasicAuthDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                basicAuthCacheService.unregister(deployable.credential());
                log.debug(
                    "BasicAuth credential [{}] undeployed for api [{}]",
                    deployable.credential().getId(),
                    deployable.credential().getApi()
                );
            } catch (Exception e) {
                log.warn("An error occurred when trying to undeploy BasicAuth credential [{}].", deployable.credential().getId(), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final SingleBasicAuthDeployable deployable) {
        return Completable.complete();
    }
}
