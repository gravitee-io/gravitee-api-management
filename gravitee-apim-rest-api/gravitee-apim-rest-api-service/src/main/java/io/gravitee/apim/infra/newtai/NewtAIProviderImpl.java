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
package io.gravitee.apim.infra.newtai;

import io.gravitee.apim.core.newtai.exception.NewtAIReplyException;
import io.gravitee.apim.core.newtai.model.ELGenQuery;
import io.gravitee.apim.core.newtai.model.ELGenReply;
import io.gravitee.apim.core.newtai.service_provider.NewtAIProvider;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.infra.apim.ApimProductInfoImpl;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELCommand;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELCommandPayload;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.InstallationNotFoundException;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NewtAIProviderImpl implements NewtAIProvider {

    private final CockpitConnector cockpitConnector;
    private final ApimProductInfoImpl apimMetadata;
    private final InstallationService installationService;

    public NewtAIProviderImpl(
        @Lazy CockpitConnector cockpitConnector,
        ApimProductInfoImpl apimMetadata,
        InstallationService installationService
    ) {
        this.cockpitConnector = cockpitConnector;
        this.apimMetadata = apimMetadata;
        this.installationService = installationService;
    }

    @Override
    public Single<ELGenReply> generateEL(ELGenQuery query) {
        var installationId = installationService.get().getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID);
        if (StringUtils.isEmpty(installationId)) {
            return Single.error(new InstallationNotFoundException("Cockpit installation not found"));
        }
        var command = new ELCommand(new ELCommandPayload(query.message(), apimMetadata.getVersion(), query.properties(), installationId));

        return cockpitConnector
            .sendCommand(command)
            .onErrorResumeNext(error ->
                Single.error(
                    new NewtAIReplyException(command.getId(), error.getMessage() != null ? error.getMessage() : error.toString(), error)
                )
            )
            .map(reply -> {
                if (reply.getCommandStatus() == CommandStatus.ERROR) {
                    throw new NewtAIReplyException(command.getId(), reply.getErrorDetails());
                } else if (reply.getPayload() instanceof ELReplyPayload payload) {
                    return new Output(payload);
                }
                throw new NewtAIReplyException(command.getId(), "Unexpected reply payload:" + reply.getPayload());
            });
    }

    private record Output(String message, FeedbackId feedbackId) implements ELGenReply {
        private Output(ELReplyPayload payload) {
            this(payload.message(), new FeedbackId(payload.feedbackId()));
        }

        private record FeedbackId(String chatId, String userMessageId, String agentMessageId) implements ELGenReply.FeedbackId {
            private FeedbackId(ELReplyPayload.RequestId requestId) {
                this(requestId.chatId(), requestId.userMessageId(), requestId.agentMessageId());
            }
        }
    }
}
