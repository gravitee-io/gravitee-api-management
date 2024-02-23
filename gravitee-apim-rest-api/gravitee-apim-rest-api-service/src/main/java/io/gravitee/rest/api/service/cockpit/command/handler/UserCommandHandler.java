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

import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.PICTURE;
import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.SUB;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.user.UserCommand;
import io.gravitee.cockpit.api.command.v1.user.UserCommandPayload;
import io.gravitee.cockpit.api.command.v1.user.UserReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserCommandHandler implements CommandHandler<UserCommand, UserReply> {

    public static final String COCKPIT_SOURCE = "cockpit";

    private final UserService userService;

    @Override
    public String supportType() {
        return CockpitCommandType.USER.name();
    }

    @Override
    public Single<UserReply> handle(UserCommand command) {
        UserCommandPayload userPayload = command.getPayload();
        ExecutionContext executionContext = new ExecutionContext(userPayload.organizationId(), null);
        try {
            final UserEntity existingUser = userService.findBySource(userPayload.organizationId(), COCKPIT_SOURCE, userPayload.id(), false);

            UpdateUserEntity updatedUser = new UpdateUserEntity();
            updatedUser.setFirstname(userPayload.firstName());
            updatedUser.setLastname(userPayload.lastName());
            updatedUser.setEmail(userPayload.email());
            updatedUser.setPicture(userPayload.picture());
            updatedUser.setCustomFields(new HashMap<>());

            if (userPayload.additionalInformation() != null) {
                updatedUser.getCustomFields().putAll(userPayload.additionalInformation());
            }

            updatedUser.getCustomFields().computeIfAbsent(PICTURE, k -> userPayload.picture());
            updatedUser.getCustomFields().computeIfAbsent(SUB, k -> userPayload.username());

            UserEntity cockpitUserEntity = userService.update(executionContext, existingUser.getId(), updatedUser);
            log.info("User [{}] with APIM id [{}] updated.", userPayload.username(), cockpitUserEntity.getId());

            return Single.just(new UserReply(command.getId()));
        } catch (UserNotFoundException unfe) {
            NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setSourceId(userPayload.id());
            newUser.setFirstname(userPayload.firstName());
            newUser.setLastname(userPayload.lastName());
            newUser.setEmail(userPayload.email());
            newUser.setPicture(userPayload.picture());
            newUser.setSource(COCKPIT_SOURCE);
            newUser.setCustomFields(new HashMap<>());

            if (userPayload.additionalInformation() != null) {
                newUser.getCustomFields().putAll(userPayload.additionalInformation());
            }

            newUser.getCustomFields().computeIfAbsent(PICTURE, k -> userPayload.picture());
            newUser.getCustomFields().computeIfAbsent(SUB, k -> userPayload.username());

            try {
                UserEntity cockpitUserEntity = userService.create(executionContext, newUser, false);
                log.info("User [{}] created with APIM id [{}].", userPayload.username(), cockpitUserEntity.getId());
                return Single.just(new UserReply(command.getId()));
            } catch (Exception e) {
                String errorDetails =
                    "Error occurred when creating user [%s] for organization [%s].".formatted(
                            userPayload.username(),
                            userPayload.organizationId()
                        );
                log.error(errorDetails, e);
                return Single.just(new UserReply(command.getId(), errorDetails));
            }
        } catch (Exception e) {
            String errorDetails =
                "Error occurred when updating user [%s] for organization [%s].".formatted(
                        userPayload.username(),
                        userPayload.organizationId()
                    );
            log.error(errorDetails, e);
            return Single.just(new UserReply(command.getId(), errorDetails));
        }
    }
}
