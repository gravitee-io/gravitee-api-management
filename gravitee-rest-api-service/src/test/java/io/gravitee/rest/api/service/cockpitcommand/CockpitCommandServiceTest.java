/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.cockpitcommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgePayload;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeTarget;
import io.gravitee.common.utils.UUID;
import io.reactivex.Single;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CockpitCommandServiceTest {

    @InjectMocks
    private final CockpitCommandService cockpitCommandService = new CockpitCommandServiceImpl();

    @Mock
    private CockpitConnector cockpitConnector;

    @Test
    public void shouldSendCommandToCockpitConnector() {
        BridgePayload payload = new BridgePayload();
        payload.setContent("a content");

        BridgeCommand command = new BridgeCommand();
        command.setId(UUID.toString(UUID.random()));
        command.setInstallationId(UUID.toString(UUID.random()));
        command.setOrganizationId(UUID.toString(UUID.random()));
        command.setOperation("an_operation");
        command.setTarget(new BridgeTarget());
        command.setPayload(payload);

        BridgeReply reply = mock(BridgeReply.class);
        when(cockpitConnector.sendCommand(command)).thenReturn(Single.just(reply));

        BridgeReply bridgeReply = cockpitCommandService.send(command);

        assertThat(bridgeReply).isEqualTo(reply);
    }
}
