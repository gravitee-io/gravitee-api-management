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

import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DictionaryDeployer implements Deployer<DictionaryDeployable> {

    private final DictionaryManager dictionaryManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final DictionaryDeployable deployable) {
        return Completable.fromRunnable(() -> {
            Dictionary dictionary = deployable.dictionary();
            try {
                dictionaryManager.deploy(dictionary);
                log.debug("Dictionary [{}] deployed ", deployable.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy dictionary %s", dictionary), e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final DictionaryDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final DictionaryDeployable deployable) {
        return Completable.fromRunnable(() -> {
            Dictionary dictionary = deployable.dictionary();
            try {
                dictionaryManager.undeploy(dictionary);
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy dictionary [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final DictionaryDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
