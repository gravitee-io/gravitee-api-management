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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An upgrader that run for each environment.
 *
 * @author GraviteeSource Team
 */
public abstract class EnvironmentUpgrader implements Upgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUpgrader.class);

    @Autowired
    private EnvironmentRepository environmentRepository;

    // upgradeEnvironment is called once for each environment
    protected abstract void upgradeEnvironment(ExecutionContext executionContext);

    @Override
    public final Completable upgrade() {
        return Completable.create(
            new CompletableOnSubscribe() {
                @Override
                public void subscribe(@NotNull CompletableEmitter emitter) throws Exception {
                    try {
                        environmentRepository
                            .findAll()
                            .forEach(
                                environment -> {
                                    ExecutionContext executionContext = new ExecutionContext(environment);
                                    LOGGER.info("Starting {} for {}", this.getClass().getSimpleName(), executionContext);
                                    upgradeEnvironment(executionContext);
                                }
                            );
                        emitter.onComplete();
                    } catch (TechnicalException e) {
                        LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
                        emitter.onError(e);
                    }
                }
            }
        );
    }
}
