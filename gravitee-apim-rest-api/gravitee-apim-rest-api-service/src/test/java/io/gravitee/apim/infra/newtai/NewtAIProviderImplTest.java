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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.newai.exception.NewtAIReplyException;
import io.gravitee.apim.core.newai.model.ELGenQuery;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestCommand;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestReply;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELRequestReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.reactivex.rxjava3.core.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class NewtAIProviderImplTest {

    @Mock
    private CockpitConnector cockpitConnector;

    @InjectMocks
    private NewtAIProviderImpl newtAIProvider;

    @Test
    void should_generate_el(VertxTestContext context) {
        var query = new Input("test message", Map.of("key", "value"));
        var replyPayload = new ELRequestReplyPayload(
            "generated el",
            new ELRequestReplyPayload.RequestId("chatId", "userMessageId", "agentMessageId")
        );
        var reply = new ELRequestReply("commandId", CommandStatus.SUCCEEDED, replyPayload);
        when(cockpitConnector.sendCommand(any(ELRequestCommand.class))).thenReturn(Single.just(reply));

        newtAIProvider
            .generateEL(query)
            .subscribe(
                elGenReply ->
                    context.verify(() -> {
                        assertThat(elGenReply.message()).isEqualTo("generated el");
                        assertThat(elGenReply.feedbackId().chatId()).isEqualTo("chatId");
                        assertThat(elGenReply.feedbackId().userMessageId()).isEqualTo("userMessageId");
                        assertThat(elGenReply.feedbackId().agentMessageId()).isEqualTo("agentMessageId");
                        context.completeNow();
                    }),
                context::failNow
            );
    }

    @Test
    void should_throw_exception_when_command_status_is_error(VertxTestContext context) {
        var query = new Input("test message");
        var reply = new ELRequestReply("commandId", "error details");
        when(cockpitConnector.sendCommand(any(ELRequestCommand.class))).thenReturn(Single.just(reply));

        newtAIProvider
            .generateEL(query)
            .subscribe(
                reply1 -> context.failNow("expect error"),
                e ->
                    context.verify(() -> {
                        assertThat(e).isInstanceOf(NewtAIReplyException.class).hasMessage("error details");
                        context.completeNow();
                    })
            );
    }

    @Test
    void should_throw_exception_when_send_command_fails(VertxTestContext context) {
        var query = new Input("test message");
        when(cockpitConnector.sendCommand(any(ELRequestCommand.class)))
            .thenReturn(Single.error(new RuntimeException("send command error")));

        newtAIProvider
            .generateEL(query)
            .subscribe(
                reply1 -> context.failNow("expect error"),
                e ->
                    context.verify(() -> {
                        assertThat(e).isInstanceOf(NewtAIReplyException.class).hasMessage("send command error");
                        context.completeNow();
                    })
            );
    }

    record Input(String message, Map<String, String> properties) implements ELGenQuery {
        Input(String message) {
            this(message, Map.of());
        }
    }
}
