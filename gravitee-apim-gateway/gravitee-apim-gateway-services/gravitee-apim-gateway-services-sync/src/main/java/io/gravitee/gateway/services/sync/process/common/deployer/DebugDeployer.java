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
package io.gravitee.gateway.services.sync.process.common.deployer;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.debug.DebugDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DebugDeployer implements Deployer<DebugDeployable> {

    private final EventManager eventManager;

    @Override
    public Completable deploy(final DebugDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                eventManager.publishEvent(ReactorEvent.DEBUG, deployable.reactable());
                log.debug("Debug event sent [{}]", deployable.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy debug reactable [%s].", deployable.id()), e);
            }
        });
    }
}
