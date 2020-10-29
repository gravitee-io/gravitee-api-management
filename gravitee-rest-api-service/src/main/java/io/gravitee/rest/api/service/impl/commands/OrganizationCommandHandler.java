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
package io.gravitee.rest.api.service.impl.commands;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.organization.OrganizationCommand;
import io.gravitee.cockpit.api.command.organization.OrganizationPayload;
import io.gravitee.cockpit.api.command.organization.OrganizationReply;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class OrganizationCommandHandler implements CommandHandler<OrganizationCommand, OrganizationReply> {

    private final Logger logger = LoggerFactory.getLogger(OrganizationCommandHandler.class);

    private final OrganizationService organizationService;

    public OrganizationCommandHandler(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.ORGANIZATION_COMMAND;
    }

    @Override
    public Single<OrganizationReply> handle(OrganizationCommand command) {

        OrganizationPayload organizationPayload = command.getPayload();

        try {
            UpdateOrganizationEntity newOrganization = new UpdateOrganizationEntity();
            newOrganization.setHrids(organizationPayload.getHrids());
            newOrganization.setName(organizationPayload.getName());
            newOrganization.setDescription(organizationPayload.getDescription());
            newOrganization.setDomainRestrictions(organizationPayload.getDomainRestrictions());

            final OrganizationEntity organization = organizationService.createOrUpdate(organizationPayload.getId(), newOrganization);
            logger.info("Organization [{}] handled with id [{}].", organization.getName(), organization.getId());
            return Single.just(new OrganizationReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception e) {
            logger.error("Error occurred when handling organization [{}] with id [{}].", organizationPayload.getName(), organizationPayload.getId(), e);
            return Single.just(new OrganizationReply(command.getId(), CommandStatus.ERROR));
        }
    }
}