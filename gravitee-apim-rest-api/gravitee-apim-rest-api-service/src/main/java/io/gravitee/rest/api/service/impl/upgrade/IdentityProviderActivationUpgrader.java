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

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
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

    @Override
    public boolean upgrade() {
        // FIXME : this upgrader uses the default ExecutionContext, but should handle all environments/organizations
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        upgradeByEnvironment(executionContext);
        return true;
    }

    private void upgradeByEnvironment(ExecutionContext executionContext) {
        // initialize roles.
        final ActivationTarget defaultEnvTarget = new ActivationTarget(
            GraviteeContext.getDefaultEnvironment(),
            IdentityProviderActivationReferenceType.ENVIRONMENT
        );
        final ActivationTarget defaultOrgTarget = new ActivationTarget(
            GraviteeContext.getDefaultOrganization(),
            IdentityProviderActivationReferenceType.ORGANIZATION
        );

        if (
            this.identityProviderActivationService.findAllByTarget(defaultOrgTarget).isEmpty() &&
            this.identityProviderActivationService.findAllByTarget(defaultEnvTarget).isEmpty()
        ) {
            logger.info("    No activation found. Active all idp on all target by default if enabled.");
            this.identityProviderService.findAll(executionContext)
                .forEach(
                    idp -> {
                        if (idp.isEnabled()) {
                            this.identityProviderActivationService.activateIdpOnTargets(
                                    executionContext,
                                    idp.getId(),
                                    defaultOrgTarget,
                                    defaultEnvTarget
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
