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
package io.gravitee.apim.infra.integration;

import io.gravitee.apim.core.integration.exception.IntegrationIngestionException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.integration.api.command.ingest.IngestCommand;
import io.gravitee.integration.api.command.ingest.IngestCommandPayload;
import io.gravitee.integration.api.command.ingest.IngestReply;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationAgentImpl implements IntegrationAgent {

    private final ExchangeController exchangeController;

    public IntegrationAgentImpl(@Qualifier("integrationExchangeController") ExchangeController exchangeController) {
        this.exchangeController = exchangeController;
    }

    @Override
    public Flowable<IntegrationApi> fetchAllApis(Integration integration) {
        var targetId = integration.getId();
        var command = new IngestCommand(new IngestCommandPayload(List.of()));

        log.debug("Fetch all assets for [integrationId={}]", targetId);
        return sendIngestCommand(command, targetId)
            .toFlowable()
            .flatMap(reply -> {
                if (reply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    log.debug("Received apis for [integrationId={}] [total={}]", targetId, reply.getPayload().apis().size());
                    return Flowable
                        .fromIterable(reply.getPayload().apis())
                        .map(api -> IntegrationAdapter.INSTANCE.map(api, integration.getId()));
                }
                return Flowable.error(new IntegrationIngestionException(reply.getErrorDetails()));
            });
    }

    private Single<IngestReply> sendIngestCommand(IngestCommand fetchCommand, String integrationId) {
        return exchangeController
            .sendCommand(fetchCommand, integrationId)
            .cast(IngestReply.class)
            .onErrorReturn(throwable -> new IngestReply(fetchCommand.getId(), throwable.getMessage()));
    }
}
