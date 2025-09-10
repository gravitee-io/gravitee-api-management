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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * An upgrader that run for each environment.
 *
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class EnvironmentInitializer implements Initializer {

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    // initializeEnvironment is called once for each environment
    protected abstract void initializeEnvironment(ExecutionContext executionContext);

    @Override
    public final boolean initialize() {
        try {
            environmentRepository
                .findAll()
                .forEach(environment -> {
                    ExecutionContext executionContext = new ExecutionContext(environment);
                    log.info("Starting {} for {}", this.getClass().getSimpleName(), executionContext);
                    initializeEnvironment(executionContext);
                });
        } catch (TechnicalException e) {
            log.error("{} execution failed", this.getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }
}
