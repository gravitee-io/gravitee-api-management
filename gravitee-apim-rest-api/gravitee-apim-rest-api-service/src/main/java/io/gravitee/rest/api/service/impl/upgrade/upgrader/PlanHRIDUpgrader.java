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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
public class PlanHRIDUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Override
    public boolean upgrade() {
        try {
            planRepository
                .findAll()
                .forEach(plan -> {
                    plan.setHrid(plan.getId());
                    try {
                        planRepository.update(plan);
                    } catch (TechnicalException e) {
                        log.error("Unable to set HRID for Plan {}", plan.getId(), e);
                        throw new RuntimeException(e);
                    }
                });
            return true;
        } catch (TechnicalException e) {
            log.error("Error applying upgrader", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PLAN_HRID_UPGRADER;
    }
}
