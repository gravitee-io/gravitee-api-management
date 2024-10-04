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

import io.gravitee.apim.core.integration.use_case.CheckIntegrationUseCase;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.hello.HelloReply;
import io.gravitee.exchange.api.command.hello.HelloReplyPayload;
import io.gravitee.integration.api.command.IntegrationCommandType;
import io.gravitee.integration.api.command.hello.HelloCommand;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class HelloCommandHandler implements CommandHandler<HelloCommand, HelloReply> {

    private final CheckIntegrationUseCase checkIntegrationUseCase;
    private final IntegrationCommandContext integrationCommandContext;

    @Override
    public String supportType() {
        return IntegrationCommandType.HELLO.name();
    }

    @Override
    public Single<HelloReply> handle(HelloCommand command) {
        return Single
            .fromCallable(() -> {
                var payload = command.getPayload();
                var result = checkIntegrationUseCase.execute(
                    new CheckIntegrationUseCase.Input(
                        integrationCommandContext.getOrganizationId(),
                        integrationCommandContext.getUserId(),
                        payload.getTargetId(),
                        payload.getProvider()
                    )
                );

                if (result.success()) {
                    integrationCommandContext.setIntegrationId(payload.getTargetId());
                    return new HelloReply(command.getId(), HelloReplyPayload.builder().targetId(payload.getTargetId()).build());
                }

                return new HelloReply(command.getId(), result.message());
            })
            .doOnError(throwable ->
                log.error("Unable to process hello command payload for target [{}]", command.getPayload().getTargetId(), throwable)
            )
            .onErrorReturn(throwable -> new HelloReply(command.getId(), throwable.getMessage()));
    }
}
