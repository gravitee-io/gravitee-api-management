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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.command.hello.HelloReplyPayload;
import io.gravitee.integration.api.command.IntegrationCommandType;
import io.gravitee.integration.api.command.hello.HelloCommand;
import io.gravitee.integration.api.command.hello.HelloCommandPayload;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.gravitee.integration.controller.command.IntegrationControllerCommandHandlerFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HelloCommandHandlerTest {

    private static final String COMMAND_ID = "command-id";
    private static final String INTEGRATION_ID = "my-integration-id";
    private static final String INTEGRATION_PROVIDER = "amazon";

    private static final HelloCommand COMMAND = new HelloCommand(
        COMMAND_ID,
        HelloCommandPayload.builder().targetId(INTEGRATION_ID).provider(INTEGRATION_PROVIDER).build()
    );

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    HelloCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        var factory = new IntegrationControllerCommandHandlerFactory(integrationCrudServiceInMemory);

        commandHandler =
            (HelloCommandHandler) factory
                .buildCommandHandlers(new IntegrationCommandContext(true))
                .stream()
                .filter(handler -> handler.supportType().equals(IntegrationCommandType.HELLO.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No handler found for type [HELLO]"));
    }

    @AfterEach
    void tearDown() {
        integrationCrudServiceInMemory.reset();
    }

    @Test
    void should_reply_succeeded_when_integration_exists() {
        var integration = givenIntegration(
            IntegrationFixture.anIntegration().toBuilder().id(INTEGRATION_ID).provider(INTEGRATION_PROVIDER).build()
        );

        commandHandler
            .handle(COMMAND)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(reply -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(reply.getCommandStatus()).isEqualTo(CommandStatus.SUCCEEDED);
                    soft.assertThat(reply.getCommandId()).isEqualTo(COMMAND_ID);
                    soft.assertThat(reply.getPayload()).isEqualTo(HelloReplyPayload.builder().targetId(integration.getId()).build());
                });

                return true;
            })
            .assertNoErrors();
    }

    @Test
    void should_reply_error_when_integration_does_not_exist() {
        commandHandler
            .handle(COMMAND)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(reply -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(reply.getCommandStatus()).isEqualTo(CommandStatus.ERROR);
                    soft.assertThat(reply.getCommandId()).isEqualTo(COMMAND_ID);
                    soft.assertThat(reply.getErrorDetails()).isEqualTo("Integration [id=my-integration-id] not found");
                });

                return true;
            })
            .assertNoErrors();
    }

    @Test
    void should_reply_error_when_integration_exist_but_provider_mismatch() {
        givenIntegration(IntegrationFixture.anIntegration().toBuilder().id("my-integration-id").provider("other").build());

        commandHandler
            .handle(COMMAND)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(reply -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(reply.getCommandStatus()).isEqualTo(CommandStatus.ERROR);
                    soft.assertThat(reply.getCommandId()).isEqualTo(COMMAND_ID);
                    soft
                        .assertThat(reply.getErrorDetails())
                        .isEqualTo("Integration [id=my-integration-id] does not match. Expected provider [provider=other]");
                });

                return true;
            })
            .assertNoErrors();
    }

    @Test
    void should_reply_error_when_exception_occurs() {
        var spied = Mockito.spy(integrationCrudServiceInMemory);
        lenient().when(spied.findById(any())).thenThrow(new TechnicalDomainException("error"));
        commandHandler = new HelloCommandHandler(spied);

        commandHandler
            .handle(COMMAND)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(reply -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(reply.getCommandStatus()).isEqualTo(CommandStatus.ERROR);
                    soft.assertThat(reply.getCommandId()).isEqualTo(COMMAND_ID);
                    soft.assertThat(reply.getErrorDetails()).isEqualTo("error");
                });

                return true;
            })
            .assertNoErrors();
    }

    private Integration givenIntegration(Integration integration) {
        integrationCrudServiceInMemory.initWith(List.of(integration));
        return integration;
    }
}
