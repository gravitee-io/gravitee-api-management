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
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOrganizationCommandHandler implements CommandHandler<DeleteOrganizationCommand, DeleteOrganizationReply> {

    @Lazy
    private final AccessPointRepository accessPointRepository;

    @Lazy
    private final FlowRepository flowRepository;

    @Lazy
    private final ParameterRepository parameterRepository;

    @Lazy
    private final AuditRepository auditRepository;

    private final OrganizationService organizationService;
    private final AccessPointCrudService accessPointService;
    private final IdentityProviderActivationService identityProviderActivationService;

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_ORGANIZATION.name();
    }

    @Override
    public Single<DeleteOrganizationReply> handle(DeleteOrganizationCommand command) {
        var payload = command.getPayload();

        try {
            var organization = organizationService.findByCockpitId(payload.cockpitId());
            var executionContext = new ExecutionContext(organization);

            disableOrganization(executionContext);
            deleteOrganization(executionContext, organization);

            log.info("Organization [{}] with id [{}] has been deleted.", organization.getName(), organization.getId());
            return Single.just(new DeleteOrganizationReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when deleting organization [%s] with id [%s].".formatted(payload.name(), payload.id());
            log.error(errorDetails, e);
            return Single.just(new DeleteOrganizationReply(command.getId(), errorDetails));
        }
    }

    private void disableOrganization(ExecutionContext executionContext) {
        // Delete related access points
        this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, executionContext.getOrganizationId());

        var context = new ExecutionContext(executionContext.getOrganizationId());

        // Deactivate all identity providers
        this.identityProviderActivationService.removeAllIdpsFromTarget(
                context,
                new IdentityProviderActivationService.ActivationTarget(
                    executionContext.getOrganizationId(),
                    IdentityProviderActivationReferenceType.ORGANIZATION
                )
            );
    }

    private void deleteOrganization(ExecutionContext executionContext, OrganizationEntity organization) throws TechnicalException {
        accessPointRepository.deleteByReference(AccessPointReferenceType.ORGANIZATION, organization.getId());
        flowRepository.deleteByReference(FlowReferenceType.ORGANIZATION, organization.getId());
        parameterRepository.deleteByReferenceIdAndReferenceType(ParameterReferenceType.ORGANIZATION, organization.getId());
        auditRepository.deleteByReferenceIdAndReferenceType(Audit.AuditReferenceType.ORGANIZATION, organization.getId());
        organizationService.delete(organization.getId());
        log.info("Organization [{}] with id [{}] has been deleted.", organization.getName(), organization.getId());
        // TODO: remove users
    }
}
