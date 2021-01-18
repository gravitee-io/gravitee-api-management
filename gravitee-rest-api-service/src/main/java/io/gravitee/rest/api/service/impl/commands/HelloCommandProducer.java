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
import io.gravitee.cockpit.api.command.CommandProducer;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.hello.HelloCommand;
import io.gravitee.cockpit.api.command.hello.HelloReply;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Single;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("cockpitHelloCommandProducer")
public class HelloCommandProducer implements CommandProducer<HelloCommand, HelloReply> {

    private final Node node;
    private final InstallationService installationService;

    public HelloCommandProducer(Node node, InstallationService installationService) {
        this.node = node;
        this.installationService = installationService;
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
        command.getPayload().setDefaultOrganizationId(GraviteeContext.getDefaultOrganization());
        command.getPayload().setDefaultEnvironmentId(GraviteeContext.getDefaultEnvironment());

        return Single.just(command);
    }

    @Override
    public Single<HelloReply> handleReply(HelloReply reply) {

        if (reply.getCommandStatus() == CommandStatus.SUCCEEDED) {
            final Map<String, String> additionalInformation = new HashMap<>();
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_ID, reply.getInstallationId());
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, reply.getInstallationStatus());
            installationService.setAdditionalInformation(additionalInformation);
        }

        return Single.just(reply);
    }
}
