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

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.v4api.V4ApiCommand;
import io.gravitee.cockpit.api.command.v4api.V4ApiReply;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.services.V4ApiServiceCockpit;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class V4ApiCommandHandler implements CommandHandler<V4ApiCommand, V4ApiReply> {

    private final Logger logger = LoggerFactory.getLogger(V4ApiCommandHandler.class);

    private final V4ApiServiceCockpit v4ApiServiceCockpit;
    private final UserService userService;
    private final OrganizationService organizationService;

    public V4ApiCommandHandler(V4ApiServiceCockpit v4ApiServiceCockpit, UserService userService, OrganizationService organizationService) {
        this.v4ApiServiceCockpit = v4ApiServiceCockpit;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.V4_API_COMMAND;
    }

    @Override
    public Single<V4ApiReply> handle(V4ApiCommand command) {
        var payload = command.getPayload();
        var org = organizationService.findByCockpitId(payload.getOrganizationId());
        var user = userService.findBySource(org.getId(), "cockpit", payload.getUserId(), true);

        authenticateAs(user);

        try {
            return v4ApiServiceCockpit
                .createPublishApi(payload.getOrganizationId(), payload.getEnvironmentId(), user.getId(), payload.getApiDefinition())
                .flatMap(apiEntity -> {
                    final V4ApiReply reply = new V4ApiReply(command.getId(), CommandStatus.SUCCEEDED);
                    reply.setApiId(apiEntity.getId());
                    reply.setApiName(apiEntity.getName());
                    reply.setApiVersion(apiEntity.getApiVersion());
                    logger.info("Api {} successfully created.", apiEntity.getName());

                    return Single.just(reply);
                });
        } catch (JsonProcessingException exception) {
            logger.error("An error occurred while creating Api.", exception);

            return Single.just(new V4ApiReply(command.getId(), CommandStatus.FAILED));
        }
    }
}
