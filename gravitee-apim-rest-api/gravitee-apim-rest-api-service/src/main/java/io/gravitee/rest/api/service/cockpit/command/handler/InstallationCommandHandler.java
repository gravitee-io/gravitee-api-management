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

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.installation.InstallationCommand;
import io.gravitee.cockpit.api.command.v1.installation.InstallationCommandPayload;
import io.gravitee.cockpit.api.command.v1.installation.InstallationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.service.InstallationService;
import io.reactivex.rxjava3.core.Single;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstallationCommandHandler implements CommandHandler<InstallationCommand, InstallationReply> {

    private final InstallationService installationService;

    @Override
    public String supportType() {
        return CockpitCommandType.INSTALLATION.name();
    }

    @Override
    public Single<InstallationReply> handle(InstallationCommand command) {
        InstallationCommandPayload installationPayload = command.getPayload();

        final Map<String, String> additionalInformation = this.installationService.getOrInitialize().getAdditionalInformation();
        additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, installationPayload.status());
        try {
            this.installationService.setAdditionalInformation(additionalInformation);
            log.info("Installation status is [{}].", installationPayload.status());
            return Single.just(new InstallationReply(command.getId()));
        } catch (Exception ex) {
            String errorDetails = "Error occurred when updating installation status.";
            log.info(errorDetails, ex);
            return Single.just(new InstallationReply(command.getId(), errorDetails));
        }
    }
}
