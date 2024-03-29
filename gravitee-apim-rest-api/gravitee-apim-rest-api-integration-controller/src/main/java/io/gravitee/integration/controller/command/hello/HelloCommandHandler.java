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
package io.gravitee.integration.controller.command.hello;

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.hello.HelloReply;
import io.gravitee.exchange.api.command.hello.HelloReplyPayload;
import io.gravitee.integration.api.command.IntegrationCommandType;
import io.gravitee.integration.api.command.hello.HelloCommand;
import io.gravitee.integration.api.command.hello.HelloCommandPayload;
import io.reactivex.rxjava3.core.Single;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class HelloCommandHandler implements CommandHandler<HelloCommand, HelloReply> {

    private final IntegrationCrudService integrationCrudService;

    @Override
    public String supportType() {
        return IntegrationCommandType.HELLO.name();
    }

    @Override
    public Single<HelloReply> handle(HelloCommand command) {
        return Single
            .fromCallable(() -> {
                HelloCommandPayload payload = command.getPayload();

                return integrationCrudService
                    .findById(payload.getTargetId())
                    .map(integration -> {
                        if (integration.getProvider().equals(payload.getProvider())) {
                            return new HelloReply(command.getId(), HelloReplyPayload.builder().targetId(integration.getId()).build());
                        }
                        return new HelloReply(
                            command.getId(),
                            String.format(
                                "Integration [id=%s] does not match. Expected provider [provider=%s]",
                                integration.getId(),
                                integration.getProvider()
                            )
                        );
                    })
                    .orElse(new HelloReply(command.getId(), String.format("Integration [id=%s] not found", payload.getTargetId())));
            })
            .doOnError(throwable ->
                log.error("Unable to process hello command payload for target [{}]", command.getPayload().getTargetId(), throwable)
            )
            .onErrorReturn(throwable -> new HelloReply(command.getId(), throwable.getMessage()));
    }
}
