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
import io.gravitee.cockpit.api.command.installation.InstallationCommand;
import io.gravitee.cockpit.api.command.installation.InstallationPayload;
import io.gravitee.cockpit.api.command.installation.InstallationReply;
import io.gravitee.rest.api.service.InstallationService;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class InstallationCommandHandler implements CommandHandler<InstallationCommand, InstallationReply> {

    private final Logger logger = LoggerFactory.getLogger(InstallationCommandHandler.class);

    private final InstallationService installationService;

    public InstallationCommandHandler(InstallationService installationService) {

        this.installationService = installationService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.INSTALLATION_COMMAND;
    }

    @Override
    public Single<InstallationReply> handle(InstallationCommand command) {

        InstallationPayload installationPayload = command.getPayload();

        final Map<String, String> additionalInformation = this.installationService.getOrInitialize().getAdditionalInformation();
        additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, installationPayload.getStatus());
        try {
            this.installationService.setAdditionalInformation(additionalInformation);
            logger.info("Installation status is [{}].", installationPayload.getStatus());
            return Single.just(new InstallationReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception ex) {
            logger.info("Error occurred when updating installation status.", ex);
            return Single.just(new InstallationReply(command.getId(), CommandStatus.ERROR));
        }

    }
}