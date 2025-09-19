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
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IdentityProviderActivationInitializer implements Initializer {

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderActivationService identityProviderActivationService;

    @Override
    public boolean initialize() {
        // FIXME : this initializer uses the default ExecutionContext, but should handle all environments/organizations
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

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
            log.info("    No activation found. Active all idp on all target by default if enabled.");
            this.identityProviderService.findAll(executionContext).forEach(idp -> {
                if (idp.isEnabled()) {
                    this.identityProviderActivationService.activateIdpOnTargets(
                        executionContext,
                        idp.getId(),
                        defaultOrgTarget,
                        defaultEnvTarget
                    );
                }
            });
        }
        return true;
    }

    @Override
    public int getOrder() {
        return InitializerOrder.IDENTITY_PROVIDER_ACTIVATION_INITIALIZER;
    }
}
