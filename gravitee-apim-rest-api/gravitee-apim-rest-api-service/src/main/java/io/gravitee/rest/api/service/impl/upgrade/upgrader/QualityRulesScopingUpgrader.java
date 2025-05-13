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
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * The QualityRulesScopingUpgrader class is responsible for upgrading the quality rules by scoping them at the environment level.
 *
 * This is the structure of the upgrader:
 *  - List all quality rules with no reference type
 *  - For each quality rule:
 *    - Scope the quality rule to the default environment
 *    - For each other environment:
 *      - Create a copy of the quality rule for the environment (with a different id)
 *      - For each API quality rule that references the source quality rule:
 *          - If the API is still existing and related to the "otherEnv" then reference the new quality rule
 *
 * This upgrader is idempotent and can be run multiple times if needed.
 *
 * After the upgrade:
 *  - All data in the quality_rule table/collection should have a reference_type and a reference_id
 *  - All data in the api_quality_rule table/collection should reference a quality rule scoped to the same environment as the API it refers to
 */
@Component
@Slf4j
public class QualityRulesScopingUpgrader implements Upgrader {

    private final QualityRuleRepository qualityRuleRepository;
    private final ApiQualityRuleRepository apiQualityRuleRepository;
    private final EnvironmentRepository environmentRepository;

    private final ApiRepository apiRepository;

    @Autowired
    public QualityRulesScopingUpgrader(
        @Lazy QualityRuleRepository qualityRuleRepository,
        @Lazy ApiQualityRuleRepository apiQualityRuleRepository,
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy ApiRepository apiRepository
    ) {
        this.qualityRuleRepository = qualityRuleRepository;
        this.apiQualityRuleRepository = apiQualityRuleRepository;
        this.environmentRepository = environmentRepository;
        this.apiRepository = apiRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.QUALITY_RULES_SCOPING_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            scopeExistingQualityRulesToEnvironments();
            return isUpgradeSuccessful();
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }
    }

    /**
     * Checks that all quality rules have been scoped to an environment.
     *
     * @return true if the upgrade was successful, false otherwise
     * @throws TechnicalException if an error occurs during the upgrade process
     */
    private boolean isUpgradeSuccessful() throws TechnicalException {
        // Check that all quality rules have been scoped
        List<QualityRule> qualityRulesWithNoReference = qualityRuleRepository
            .findAll()
            .stream()
            .filter(qr -> qr.getReferenceType() == null)
            .toList();
        if (!qualityRulesWithNoReference.isEmpty()) {
            log.error("Upgrade failed!");
            log.error(
                "The following quality rules have not been scoped at the environment level: {}",
                qualityRulesWithNoReference.stream().map(QualityRule::getId).toList()
            );
            log.error(
                "You can try to manually set the reference_type and reference_id to your default environment for these quality rules."
            );
            log.error("Then you can run the upgrader again.");
            return false;
        }
        return true;
    }

    /**
     * Scopes existing quality rules at the environment level.
     *
     * @throws TechnicalException if there is an error while scoping quality rules at the environment level.
     */
    private void scopeExistingQualityRulesToEnvironments() throws TechnicalException {
        log.info("Start scoping existing quality rules at the environment level");

        // Get all quality rules with no reference type
        List<QualityRule> qualityRules = qualityRuleRepository.findAll().stream().filter(qr -> qr.getReferenceType() == null).toList();

        // Log the number of quality rules to scope
        log.info("Found {} quality rules with no reference", qualityRules.size());

        // Get all environments
        List<Environment> environments = environmentRepository.findAll().stream().toList();
        if (environments.isEmpty()) {
            log.error("No environment found, skipping scoping of quality rules");
            return;
        }

        // Get the default environment
        Environment defaultEnv = environments
            .stream()
            .filter(env -> env.getId().equals(GraviteeContext.getDefaultEnvironment()))
            .findFirst()
            .orElse(environments.get(0));

        log.info("Default environment is [{}]", defaultEnv.getId());
        List<Environment> otherEnvs = environments.stream().filter(env -> !env.getId().equals(defaultEnv.getId())).toList();
        log.info("Other environments are {}", otherEnvs.stream().map(Environment::getId).toList());

        // Scope the quality rules to the environments
        qualityRules.forEach(qualityRule -> {
            try {
                scopeQualityRuleToEnvironments(qualityRule, defaultEnv, otherEnvs);
            } catch (TechnicalException e) {
                log.error("Error while scoping quality rule [{}] to environments", qualityRule.getId(), e);
            }
        });
    }

    /**
     * Scope the provided quality rule to multiple environments.
     *
     * @param qualityRule The quality rule to be scoped.
     * @param defaultEnv The default environment to scope the quality rule to.
     * @param otherEnvs The set of other environments to scope the quality rule to.
     * @throws TechnicalException If any technical error occurs during the scoping process.
     */
    private void scopeQualityRuleToEnvironments(QualityRule qualityRule, Environment defaultEnv, List<Environment> otherEnvs)
        throws TechnicalException {
        log.info("Scoping quality rule [{}] to environments", qualityRule.getId());
        // First, scope the quality rule to the default environment
        qualityRule.setReferenceType(QualityRule.ReferenceType.ENVIRONMENT);
        qualityRule.setReferenceId(defaultEnv.getId());
        qualityRuleRepository.update(qualityRule);

        // Then for each other env
        for (Environment otherEnv : otherEnvs) {
            // Create a copy of the quality rule for the environment (with a different id)
            QualityRule qualityRuleForOtherEnv = duplicateForEnv(qualityRule, otherEnv);
            qualityRuleRepository.create(qualityRuleForOtherEnv);

            // Then update all api quality rules that reference the quality rule to reference the new quality rule
            updateApiQualityRules(qualityRule, qualityRuleForOtherEnv, otherEnv);
        }
    }

    /**
     * Updates API quality rules for a specific environment.
     *
     * @param qualityRuleForDefaultEnv The quality rule to be replaced for the default environment.
     * @param qualityRuleForOtherEnv The new quality rule for the other environment.
     * @param otherEnv The other environment for which the quality rule will be updated.
     * @throws TechnicalException If there is an error while updating the API quality rules.
     */
    private void updateApiQualityRules(QualityRule qualityRuleForDefaultEnv, QualityRule qualityRuleForOtherEnv, Environment otherEnv)
        throws TechnicalException {
        List<ApiQualityRule> apiQualityRules = apiQualityRuleRepository.findByQualityRule(qualityRuleForDefaultEnv.getId());

        for (ApiQualityRule apiQualityRule : apiQualityRules) {
            log.debug(
                "Updating api quality rule API [{}] - Quality rule [{}] for Environment [{}]",
                apiQualityRule.getApi(),
                apiQualityRule.getQualityRule(),
                otherEnv.getId()
            );
            Optional<Api> apiOptional = apiRepository.findById(apiQualityRule.getApi());

            // Is the API still existing and related to the "otherEnv"?
            if (apiOptional.isPresent() && apiOptional.get().getEnvironmentId().equals(otherEnv.getId())) {
                // Reference the new quality rule
                apiQualityRule.setQualityRule(qualityRuleForOtherEnv.getId());
                apiQualityRuleRepository.update(apiQualityRule);
            }
        }
    }

    /**
     * Creates a duplicate of a QualityRule for a specific Environment.
     *
     * @param source the original QualityRule to duplicate
     * @param environment the Environment for which the QualityRule is duplicated
     * @return a new QualityRule instance that is a duplicate of the source QualityRule, but with the specified Environment ID and a new ID
     */
    private QualityRule duplicateForEnv(QualityRule source, Environment environment) {
        return new QualityRule(
            UuidString.generateRandom(),
            QualityRule.ReferenceType.ENVIRONMENT,
            environment.getId(),
            source.getName(),
            source.getDescription(),
            source.getWeight(),
            source.getCreatedAt(),
            source.getUpdatedAt()
        );
    }
}
