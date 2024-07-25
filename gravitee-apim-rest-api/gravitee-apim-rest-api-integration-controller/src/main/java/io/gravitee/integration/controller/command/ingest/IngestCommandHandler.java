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
package io.gravitee.integration.controller.command.ingest;

import io.gravitee.apim.core.integration.use_case.IngestFederatedApisUseCase;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.integration.api.command.IntegrationCommandType;
import io.gravitee.integration.api.command.ingest.IngestCommand;
import io.gravitee.integration.api.command.ingest.IngestReply;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class IngestCommandHandler implements CommandHandler<IngestCommand, IngestReply> {

    private final IngestFederatedApisUseCase ingestFederatedApisUseCase;
    private final IntegrationCommandContext integrationCommandContext;

    @Override
    public String supportType() {
        return IntegrationCommandType.INGEST.name();
    }

    @Override
    public Single<IngestReply> handle(IngestCommand command) {
        var payload = command.getPayload();
        return ingestFederatedApisUseCase
            .execute(
                new IngestFederatedApisUseCase.Input(
                    integrationCommandContext.getOrganizationId(),
                    payload.ingestJobId(),
                    payload
                        .apis()
                        .stream()
                        .map(api -> IntegrationAdapter.INSTANCE.map(api, integrationCommandContext.getIntegrationId()))
                        .toList(),
                    payload.done()
                )
            )
            .andThen(Single.just(new IngestReply(command.getId())))
            .doOnError(throwable ->
                log.error(
                    "Unable to process ingest command payload for integration [{}]",
                    integrationCommandContext.getIntegrationId(),
                    throwable
                )
            )
            .onErrorReturn(throwable -> new IngestReply(command.getId(), throwable.getMessage()));
    }
}
