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
package io.gravitee.rest.api.service.cockpit.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgePayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.common.utils.UUID;
import io.gravitee.exchange.api.command.CommandStatus;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CockpitCommandServiceTest {

    private CockpitCommandService cockpitCommandService;

    @Mock
    private CockpitConnector cockpitConnector;

    @Before
    public void setup() {
        cockpitCommandService = new CockpitCommandServiceImpl(cockpitConnector);
    }

    @Test
    public void shouldSendCommandToCockpitConnector() {
        BridgeCommandPayload payload = BridgeCommandPayload.builder()
            .installationId(UUID.toString(UUID.random()))
            .organizationId(UUID.toString(UUID.random()))
            .operation("an_operation")
            .target(new BridgeCommandPayload.BridgeTarget(null, null))
            .content("a content")
            .build();
        BridgeCommand command = new BridgeCommand(payload);

        BridgeReply reply = mock(BridgeReply.class);
        when(cockpitConnector.sendCommand(command)).thenReturn(Single.just(reply));

        BridgeReply bridgeReply = cockpitCommandService.send(command);

        assertThat(bridgeReply).isEqualTo(reply);
    }

    @Test
    public void shouldReturnAnErrorBridgeReplyWhenWebSocketIsThrowing() {
        BridgeCommandPayload payload = BridgeCommandPayload.builder()
            .installationId(UUID.toString(UUID.random()))
            .organizationId(UUID.toString(UUID.random()))
            .operation("an_operation")
            .target(new BridgeCommandPayload.BridgeTarget(null, null))
            .content("a content")
            .build();

        BridgeCommand command = new BridgeCommand(payload);
        when(cockpitConnector.sendCommand(command)).thenReturn(Single.error(new RuntimeException()));

        BridgeReply bridgeReply = cockpitCommandService.send(command);
        assertThat(bridgeReply.getCommandId()).isEqualTo(command.getId());
        assertThat(bridgeReply.getCommandStatus()).isEqualTo(CommandStatus.ERROR);
    }

    // Reproduction test case
    @Test(timeout = 5000)
    public void shouldReturnErrorWhenCockpitConnectorHangs() {
        BridgeCommandPayload payload = BridgeCommandPayload.builder()
            .installationId(UUID.toString(UUID.random()))
            .organizationId(UUID.toString(UUID.random()))
            .operation("an_operation")
            .target(new BridgeCommandPayload.BridgeTarget(null, null))
            .content("a content")
            .build();

        BridgeCommand command = new BridgeCommand(payload);
        // Simulate a hanging connection using Single.never()
        // We expect the service to timeout (default 10s, but we'll mock or rely on test
        // timeout if default is used)
        // Actually, since default is 10s and test timeout is 5s, the test would fail if
        // we don't injecting a smaller timeout value.
        // We need to inject the timeout value into the service instance or mock it.
        // Since we cannot easily inject private field in this unit test setup without
        // Reflection, allow me to use ReflectionTestUtils or similar if available,
        // or just rely on the fact that we changed the code to throw Exception which is
        // caught.

        // Wait, if default is 10s and test timeout is 5s, the blockingGet(10s) will
        // block for 5s and test fails.
        // I should stick to verify the code handles timeout exception.

        when(cockpitConnector.sendCommand(command)).thenReturn(Single.never());

        // We can't easily change the private timeout field here without spring context
        // or reflection.
        // For the sake of this test verifying the fix logic, I'll trust the logic if it
        // compiles.
        // But to make it pass, I'll use reflection to set timeout to 100ms.
        org.springframework.test.util.ReflectionTestUtils.setField(cockpitCommandService, "cockpitCommandTimeout", 100);

        BridgeReply reply = cockpitCommandService.send(command);

        assertThat(reply.getCommandId()).isEqualTo(command.getId());
        assertThat(reply.getErrorDetails()).contains("Error while sending command to cockpit");
    }
}
