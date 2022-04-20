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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class IdentityProviderActivationUpgrader implements Upgrader, Ordered {

    private final Logger logger = LoggerFactory.getLogger(IdentityProviderActivationUpgrader.class);

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderActivationService identityProviderActivationService;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Override
    public boolean upgrade() {
        try {
            for (Environment environment : environmentRepository.findAll()) {
                upgradeByEnvironment(new ExecutionContext(environment.getOrganizationId(), environment.getId()));
            }
        } catch (TechnicalException e) {
            logger.error("{} execution failed", this.getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }

    private void upgradeByEnvironment(ExecutionContext executionContext) {
        // initialize roles.
        final ActivationTarget envTarget = new ActivationTarget(
            executionContext.getEnvironmentId(),
            IdentityProviderActivationReferenceType.ENVIRONMENT
        );
        final ActivationTarget orgTarget = new ActivationTarget(
            executionContext.getOrganizationId(),
            IdentityProviderActivationReferenceType.ORGANIZATION
        );

        if (
            this.identityProviderActivationService.findAllByTarget(envTarget).isEmpty() &&
            this.identityProviderActivationService.findAllByTarget(orgTarget).isEmpty()
        ) {
            logger.info("    No activation found. Active all idp on all target by default if enabled.");
            this.identityProviderService.findAll(executionContext)
                .forEach(
                    idp -> {
                        if (idp.isEnabled()) {
                            this.identityProviderActivationService.activateIdpOnTargets(
                                    executionContext,
                                    idp.getId(),
                                    envTarget,
                                    orgTarget
                                );
                        }
                    }
                );
        }
    }

    @Override
    public int getOrder() {
        return 400;
    }
}
