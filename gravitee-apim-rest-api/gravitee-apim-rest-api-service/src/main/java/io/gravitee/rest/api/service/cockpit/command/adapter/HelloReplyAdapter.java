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
package io.gravitee.rest.api.service.cockpit.command.adapter;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.hello.HelloReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.command.ReplyAdapter;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.OrganizationService;
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
@RequiredArgsConstructor
@Slf4j
public class HelloReplyAdapter implements ReplyAdapter<HelloReply, io.gravitee.exchange.api.command.hello.HelloReply> {

    private final InstallationService installationService;
    private final EnvironmentService environmentService;
    private final OrganizationService organizationService;

    @Override
    public String supportType() {
        return CockpitCommandType.HELLO.name();
    }

    @Override
    public Single<io.gravitee.exchange.api.command.hello.HelloReply> adapt(final HelloReply reply) {
        return Single
            .just(reply.getPayload())
            .map(replyPayload -> {
                if (reply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    final Map<String, String> additionalInformation = installationService.getOrInitialize().getAdditionalInformation();
                    additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_ID, replyPayload.getInstallationId());
                    additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, replyPayload.getInstallationStatus());
                    installationService.setAdditionalInformation(additionalInformation);

                    if (replyPayload.getDefaultEnvironmentCockpitId() != null) {
                        updateDefaultEnvironmentCockpitId(replyPayload.getDefaultEnvironmentCockpitId());
                    }

                    if (replyPayload.getDefaultOrganizationCockpitId() != null) {
                        updateDefaultOrganizationCockpitId(replyPayload.getDefaultOrganizationCockpitId());
                    }
                }

                return new io.gravitee.exchange.api.command.hello.HelloReply(reply.getCommandId(), replyPayload);
            });
    }

    private void updateDefaultEnvironmentCockpitId(String defaultEnvironmentCockpitId) {
        EnvironmentEntity defaultEnvironment = environmentService.getDefaultOrInitialize();

        UpdateEnvironmentEntity updateEnvironment = new UpdateEnvironmentEntity(defaultEnvironment);
        updateEnvironment.setCockpitId(defaultEnvironmentCockpitId);

        environmentService.createOrUpdate(defaultEnvironment.getOrganizationId(), defaultEnvironment.getId(), updateEnvironment);
    }

    private void updateDefaultOrganizationCockpitId(String defaultOrganizationCockpitId) {
        OrganizationEntity defaultOrganization = organizationService.getDefaultOrInitialize();

        UpdateOrganizationEntity updateOrganization = new UpdateOrganizationEntity(defaultOrganization);
        updateOrganization.setCockpitId(defaultOrganizationCockpitId);

        organizationService.updateOrganization(defaultOrganization.getId(), updateOrganization);
    }
}
