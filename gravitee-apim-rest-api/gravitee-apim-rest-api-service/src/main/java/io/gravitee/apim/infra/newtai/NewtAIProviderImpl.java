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

import io.gravitee.apim.core.newai.exception.NewtAIReplyException;
import io.gravitee.apim.core.newai.model.ELGenQuery;
import io.gravitee.apim.core.newai.model.ELGenReply;
import io.gravitee.apim.core.newai.service_provider.NewtAIProvider;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestCommand;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestCommandPayload;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.reactivex.rxjava3.core.Single;
import java.io.IOException;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NewtAIProviderImpl implements NewtAIProvider {

    private final CockpitConnector cockpitConnector;
    private final String apimVersion;

    public NewtAIProviderImpl(@Lazy CockpitConnector cockpitConnector) throws IOException {
        this.cockpitConnector = cockpitConnector;

        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("api.properties"));
        apimVersion = properties.getProperty("api.version");
    }

    @Override
    public Single<ELGenReply> generateEL(ELGenQuery query) {
        var command = new ELRequestCommand(new ELRequestCommandPayload(query.message(), apimVersion, query.properties()));

        return cockpitConnector
            .sendCommand(command)
            .onErrorResumeNext(error ->
                Single.error(new NewtAIReplyException(command.getId(), error.getMessage() != null ? error.getMessage() : error.toString()))
            )
            .map(reply -> {
                if (reply.getCommandStatus() == CommandStatus.ERROR) {
                    throw new NewtAIReplyException(command.getId(), reply.getErrorDetails());
                } else if (reply.getPayload() instanceof ELRequestReplyPayload payload) {
                    return new Output(payload);
                }
                throw new NewtAIReplyException(command.getId(), "Unexpected reply payload:" + reply.getPayload());
            });
    }

    private record Output(String message, FeedbackId feedbackId) implements ELGenReply {
        private Output(ELRequestReplyPayload payload) {
            this(payload.message(), new FeedbackId(payload.feedbackId()));
        }

        private record FeedbackId(String chatId, String userMessageId, String agentMessageId) implements ELGenReply.FeedbackId {
            private FeedbackId(ELRequestReplyPayload.RequestId requestId) {
                this(requestId.chatId(), requestId.userMessageId(), requestId.agentMessageId());
            }
        }
    }
}
