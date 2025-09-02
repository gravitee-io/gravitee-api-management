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

import io.gravitee.apim.core.newtai.exception.NewtAIReplyException;
import io.gravitee.apim.core.newtai.model.ELGenQuery;
import io.gravitee.apim.infra.apim.ApimProductInfoImpl;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELCommand;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELReply;
import io.gravitee.cockpit.api.command.v1.newtai.elgen.ELReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.reactivex.rxjava3.core.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
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

    public static final InstallationEntity INSTALLATION_ENTITY = InstallationEntity
        .builder()
        .additionalInformation(Map.of(InstallationService.COCKPIT_INSTALLATION_ID, "installationId"))
        .build();

    @Mock
    private CockpitConnector cockpitConnector;

    @Mock
    private InstallationService installationService;

    @Mock
    private ApimProductInfoImpl apimMetadata;

    @InjectMocks
    private NewtAIProviderImpl cut;

    @BeforeEach
    void setUp() {
        when(apimMetadata.getVersion()).thenReturn("4.9");
    }

    @Test
    @SneakyThrows
    void should_generate_el(VertxTestContext context) {
        when(installationService.get()).thenReturn(INSTALLATION_ENTITY);
        var query = new Input("apiId", "test message", "cid", Map.of("key", "value"));
        var replyPayload = new ELReplyPayload("generated el", new ELReplyPayload.RequestId("chatId", "userMessageId", "agentMessageId"));
        var reply = new ELReply("commandId", CommandStatus.SUCCEEDED, replyPayload);
        when(cockpitConnector.sendCommand(any(ELCommand.class))).thenReturn(Single.just(reply));

        cut
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
    @SneakyThrows
    void should_throw_exception_when_command_status_is_error(VertxTestContext context) {
        when(installationService.get()).thenReturn(INSTALLATION_ENTITY);
        var query = new Input("apiId", "test message", "cid");
        var reply = new ELReply("commandId", "error details");
        when(cockpitConnector.sendCommand(any(ELCommand.class))).thenReturn(Single.just(reply));

        cut
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
    @SneakyThrows
    void should_throw_exception_when_send_command_fails(VertxTestContext context) {
        when(installationService.get()).thenReturn(INSTALLATION_ENTITY);
        var query = new Input("apiId", "test message", "cid");
        when(cockpitConnector.sendCommand(any(ELCommand.class))).thenReturn(Single.error(new RuntimeException("send command error")));

        cut
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

    record Input(String apiId, String message, String chatId, Map<String, String> properties) implements ELGenQuery {
        Input(String apiId, String message, String chatId) {
            this(apiId, message, chatId, Map.of());
        }
    }
}
