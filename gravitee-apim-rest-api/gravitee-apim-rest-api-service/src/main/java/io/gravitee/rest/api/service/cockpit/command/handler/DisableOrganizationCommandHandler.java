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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.organization.DisableOrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.DisableOrganizationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisableOrganizationCommandHandler implements CommandHandler<DisableOrganizationCommand, DisableOrganizationReply> {

    private final OrganizationService organizationService;
    private final AccessPointCrudService accessPointService;
    private final IdentityProviderActivationService identityProviderActivationService;

    public DisableOrganizationCommandHandler(
        OrganizationService organizationService,
        AccessPointCrudService accessPointService,
        IdentityProviderActivationService identityProviderActivationService
    ) {
        this.organizationService = organizationService;
        this.accessPointService = accessPointService;
        this.identityProviderActivationService = identityProviderActivationService;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.DISABLE_ORGANIZATION.name();
    }

    @Override
    public Single<DisableOrganizationReply> handle(DisableOrganizationCommand command) {
        var organizationPayload = command.getPayload();
        try {
            var organization = organizationService.findByCockpitId(organizationPayload.cockpitId());

            // Delete related access points
            this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organization.getId());

            var context = new ExecutionContext(organization.getId());

            // Deactivate all identity providers
            this.identityProviderActivationService.removeAllIdpsFromTarget(
                    context,
                    new IdentityProviderActivationService.ActivationTarget(
                        organization.getId(),
                        IdentityProviderActivationReferenceType.ORGANIZATION
                    )
                );

            log.info("Organization [{}] with id [{}] has been disabled.", organization.getName(), organization.getId());
            return Single.just(new DisableOrganizationReply(command.getId()));
        } catch (Exception e) {
            String errorDetails =
                "Error occurred when disabling organization [%s] with id [%s].".formatted(
                        organizationPayload.name(),
                        organizationPayload.id()
                    );
            log.error(errorDetails, e);
            return Single.just(new DisableOrganizationReply(command.getId(), errorDetails));
        }
    }
}
