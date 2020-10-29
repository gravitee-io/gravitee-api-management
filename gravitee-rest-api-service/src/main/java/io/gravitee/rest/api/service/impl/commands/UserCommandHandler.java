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
import io.gravitee.cockpit.api.command.user.UserCommand;
import io.gravitee.cockpit.api.command.user.UserPayload;
import io.gravitee.cockpit.api.command.user.UserReply;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.PICTURE;
import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.SUB;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserCommandHandler implements CommandHandler<UserCommand, UserReply> {

    public static final String COCKPIT_SOURCE = "cockpit";
    private final Logger logger = LoggerFactory.getLogger(UserCommandHandler.class);

    private final UserService userService;

    public UserCommandHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.USER_COMMAND;
    }

    @Override
    public Single<UserReply> handle(UserCommand command) {

        UserPayload userPayload = command.getPayload();
        GraviteeContext.setCurrentOrganization(userPayload.getOrganizationId());

        try {
            NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setSourceId(userPayload.getId());
            newUser.setFirstname(userPayload.getFirstName());
            newUser.setLastname(userPayload.getLastName());
            newUser.setEmail(userPayload.getEmail());
            newUser.setSource(COCKPIT_SOURCE);
            newUser.setCustomFields(new HashMap<>());

            if (userPayload.getAdditionalInformation() != null) {
                newUser.getCustomFields().putAll(userPayload.getAdditionalInformation());
            }

            newUser.getCustomFields().computeIfAbsent(PICTURE, k -> userPayload.getPicture());
            newUser.getCustomFields().computeIfAbsent(SUB, k -> userPayload.getUsername());

            final UserEntity userEntity = userService.create(newUser, false);
            logger.info("User [{}] created with id [{}].", userPayload.getUsername(), userEntity.getId());
            return Single.just(new UserReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception e) {
            logger.info("Error occurred when creating user [{}] for organization [{}].", userPayload.getUsername(), userPayload.getOrganizationId(), e);
            return Single.just(new UserReply(command.getId(), CommandStatus.ERROR));
        } finally {
            GraviteeContext.cleanContext();
        }
    }
}