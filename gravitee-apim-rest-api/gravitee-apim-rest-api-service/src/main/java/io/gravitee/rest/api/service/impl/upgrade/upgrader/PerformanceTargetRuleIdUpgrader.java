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
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.repository.management.model.PerformanceTarget;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Performance targets stored before rules had ids get one per rule, so their rules can be followed from one
 * evaluation to the next and their owners told when a verdict changes.
 */
@CustomLog
@Component
public class PerformanceTargetRuleIdUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private PerformanceTargetRepository performanceTargetRepository;

    @Override
    public boolean upgrade() {
        try {
            for (var target : performanceTargetRepository.findAll()) {
                if (
                    target
                        .getRules()
                        .stream()
                        .anyMatch(rule -> rule.id() == null)
                ) {
                    log.info("Giving the rules of performance target {} an id", target.getId());
                    target.setRules(target.getRules().stream().map(PerformanceTargetRuleIdUpgrader::identified).toList());
                    performanceTargetRepository.update(target);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Performance target rules could not be given an id", e);
            return false;
        }
    }

    private static PerformanceTarget.Rule identified(PerformanceTarget.Rule rule) {
        if (rule.id() != null) {
            return rule;
        }
        return new PerformanceTarget.Rule(
            UuidString.generateRandom(),
            rule.metric(),
            rule.measure(),
            rule.operator(),
            rule.threshold(),
            rule.apiTypes(),
            rule.filters()
        );
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PERFORMANCE_TARGET_RULE_ID_UPGRADER;
    }
}
