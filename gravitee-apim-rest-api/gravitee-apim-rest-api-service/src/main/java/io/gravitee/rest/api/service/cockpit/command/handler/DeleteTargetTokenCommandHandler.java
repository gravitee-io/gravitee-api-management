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

import static io.gravitee.rest.api.service.cockpit.command.handler.TargetTokenCommandHandler.CLOUD_TOKEN_SOURCE;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.targettoken.DeleteTargetTokenCommand;
import io.gravitee.cockpit.api.command.v1.targettoken.DeleteTargetTokenCommandPayload;
import io.gravitee.cockpit.api.command.v1.targettoken.DeleteTargetTokenReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteTargetTokenCommandHandler implements CommandHandler<DeleteTargetTokenCommand, DeleteTargetTokenReply> {

    private final UserService userService;

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_TARGET_TOKEN.name();
    }

    public Single<DeleteTargetTokenReply> handle(DeleteTargetTokenCommand command) {
        DeleteTargetTokenCommandPayload payload = command.getPayload();
        ExecutionContext context = new ExecutionContext(payload.organizationId(), payload.environmentId());
        try {
            var userEntity = userService.findBySource(payload.organizationId(), CLOUD_TOKEN_SOURCE, payload.id(), false);
            userService.delete(context, userEntity.getId());
            log.info("User with id [{}] has been deleted.", userEntity.getId());
        } catch (UserNotFoundException e) {
            log.info("User with id [{}] has not been found.", payload.id());
        } catch (Exception e) {
            log.error("Error occurred while deleting target token for user with id [{}].", payload.id(), e);
            return Single.just(new DeleteTargetTokenReply(command.getId(), CommandStatus.ERROR));
        }

        return Single.just(new DeleteTargetTokenReply(command.getId(), CommandStatus.SUCCEEDED));
    }
}
