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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApplicationHRIDUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApplicationRepository applicationRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Override
    public boolean upgrade() {
        try {
            for (var environment : environmentRepository.findAll()) {
                setHRIDs(new ExecutionContext(environment));
            }
            return true;
        } catch (TechnicalException e) {
            log.error("Error applying upgrader", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.APPLICATION_HRID_UPGRADER;
    }

    private void setHRIDs(ExecutionContext executionContext) throws TechnicalException {
        applicationRepository
            .findAllByEnvironment(executionContext.getEnvironmentId())
            .forEach(application -> {
                application.setHrid(application.getId());
                try {
                    applicationRepository.update(application);
                } catch (TechnicalException e) {
                    log.error("Unable to set HRID for Application {}", application.getId(), e);
                    throw new RuntimeException(e);
                }
            });
    }
}
