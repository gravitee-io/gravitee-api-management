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
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.v4api.V4ApiCommand;
import io.gravitee.cockpit.api.command.v1.v4api.V4ApiCommandPayload;
import io.gravitee.cockpit.api.command.v1.v4api.V4ApiReply;
import io.gravitee.cockpit.api.command.v1.v4api.V4ApiReplyPayload;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.services.V4ApiServiceCockpit;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class V4ApiCommandHandler implements CommandHandler<V4ApiCommand, V4ApiReply> {

    private final V4ApiServiceCockpit v4ApiServiceCockpit;
    private final UserService userService;

    @Override
    public String supportType() {
        return CockpitCommandType.V4_API.name();
    }

    @Override
    public Single<V4ApiReply> handle(V4ApiCommand command) {
        final V4ApiCommandPayload payload = command.getPayload();
        final UserEntity user = userService.findBySource(payload.organizationId(), "cockpit", payload.userId(), true);

        authenticateAs(user);

        try {
            return v4ApiServiceCockpit
                .createPublishApi(payload.organizationId(), payload.environmentId(), user.getId(), payload.apiDefinition())
                .flatMap(apiEntity -> {
                    final V4ApiReplyPayload v4ApiReplyPayload = V4ApiReplyPayload
                        .builder()
                        .apiId(apiEntity.getId())
                        .apiName(apiEntity.getName())
                        .apiVersion(apiEntity.getApiVersion())
                        .build();
                    log.info("Api {} successfully created.", apiEntity.getName());

                    return Single.just(new V4ApiReply(command.getId(), v4ApiReplyPayload));
                });
        } catch (JsonProcessingException exception) {
            String errorDetails = "An error occurred while creating Api.";
            log.error(errorDetails, exception);
            return Single.just(new V4ApiReply(command.getId(), errorDetails));
        }
    }
}
