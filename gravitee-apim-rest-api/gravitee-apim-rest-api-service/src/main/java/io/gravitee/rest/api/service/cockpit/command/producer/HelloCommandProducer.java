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
package io.gravitee.rest.api.service.cockpit.command.producer;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandProducer;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.hello.HelloCommand;
import io.gravitee.cockpit.api.command.hello.HelloReply;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("cockpitHelloCommandProducer")
public class HelloCommandProducer implements CommandProducer<HelloCommand, HelloReply> {

    private static final String UI_URL = "UI_URL";
    private static final String API_URL = "API_URL";

    @Value("${console.ui.url:http://localhost:3000}")
    private String uiURL;

    @Value("${console.api.url:http://localhost:8083/management}")
    private String apiURL;

    private final Node node;
    private final InstallationService installationService;
    private final EnvironmentService environmentService;
    private final OrganizationService organizationService;

    public HelloCommandProducer(
        Node node,
        InstallationService installationService,
        EnvironmentService environmentService,
        OrganizationService organizationService
    ) {
        this.node = node;
        this.installationService = installationService;
        this.environmentService = environmentService;
        this.organizationService = organizationService;
    }

    @Override
    public Command.Type produceType() {
        return Command.Type.HELLO_COMMAND;
    }

    @Override
    public Single<HelloCommand> prepare(HelloCommand command) {
        final InstallationEntity installation = installationService.getOrInitialize();

        command.getPayload().getNode().setInstallationId(installation.getId());
        command.getPayload().getNode().setHostname(node.hostname());
        command.getPayload().getAdditionalInformation().putAll(installation.getAdditionalInformation());
        command.getPayload().getAdditionalInformation().put(UI_URL, uiURL);
        command.getPayload().getAdditionalInformation().put(API_URL, apiURL);
        command.getPayload().setDefaultOrganizationId(GraviteeContext.getDefaultOrganization());
        command.getPayload().setDefaultEnvironmentId(GraviteeContext.getDefaultEnvironment());

        return Single.just(command);
    }

    @Override
    public Single<HelloReply> handleReply(HelloReply reply) {
        if (reply.getCommandStatus() == CommandStatus.SUCCEEDED) {
            final Map<String, String> additionalInformation = installationService.getOrInitialize().getAdditionalInformation();
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_ID, reply.getInstallationId());
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, reply.getInstallationStatus());
            installationService.setAdditionalInformation(additionalInformation);

            if (reply.getDefaultEnvironmentCockpitId() != null) {
                updateDefaultEnvironmentCockpitId(reply.getDefaultEnvironmentCockpitId());
            }

            if (reply.getDefaultOrganizationCockpitId() != null) {
                updateDefaultOrganizationCockpitId(reply.getDefaultOrganizationCockpitId());
            }
        }

        return Single.just(reply);
    }

    private void updateDefaultEnvironmentCockpitId(String defaultEnvironmentCockpitId) {
        EnvironmentEntity defaultEnvironment = environmentService.findById(GraviteeContext.getDefaultEnvironment());

        UpdateEnvironmentEntity updateEnvironment = new UpdateEnvironmentEntity(defaultEnvironment);
        updateEnvironment.setCockpitId(defaultEnvironmentCockpitId);

        environmentService.createOrUpdate(defaultEnvironment.getOrganizationId(), defaultEnvironment.getId(), updateEnvironment);
    }

    private void updateDefaultOrganizationCockpitId(String defaultOrganizationCockpitId) {
        OrganizationEntity defaultOrganization = organizationService.findById(GraviteeContext.getDefaultOrganization());

        UpdateOrganizationEntity updateOrganization = new UpdateOrganizationEntity(defaultOrganization);
        updateOrganization.setCockpitId(defaultOrganizationCockpitId);

        organizationService.createOrUpdate(GraviteeContext.getExecutionContext(), updateOrganization);
    }
}
