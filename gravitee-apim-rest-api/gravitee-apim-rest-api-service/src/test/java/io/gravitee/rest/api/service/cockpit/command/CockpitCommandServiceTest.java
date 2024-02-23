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
        BridgeCommandPayload payload = BridgeCommandPayload
            .builder()
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
        BridgeCommandPayload payload = BridgeCommandPayload
            .builder()
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
}
