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
package io.gravitee.apim.core.newai.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.newai.model.ELGenQuery;
import io.gravitee.apim.core.newai.model.ELGenReply;
import io.gravitee.apim.core.newai.service_provider.NewtAIProvider;
import io.reactivex.rxjava3.core.Single;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GenerateELUseCase {

    private final NewtAIProvider newtAIProvider;

    public Single<Output> execute(Input input) {
        return newtAIProvider.generateEL(input).map(Output::new);
    }

    public record Input(String message, Map<String, String> properties, AuditInfo auditInfo) implements ELGenQuery {}

    public record Output(String message, FeedbackId feedbackId) {
        public Output(ELGenReply reply) {
            this(reply.message(), new FeedbackId(reply.feedbackId()));
        }

        public record FeedbackId(String chatId, String userMessageId, String agentMessageId) {
            public FeedbackId(ELGenReply.FeedbackId feedbackId) {
                this(feedbackId.chatId(), feedbackId.userMessageId(), feedbackId.agentMessageId());
            }
        }
    }
}
