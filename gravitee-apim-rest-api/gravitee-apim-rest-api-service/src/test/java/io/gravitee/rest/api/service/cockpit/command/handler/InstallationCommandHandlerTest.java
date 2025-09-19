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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.installation.InstallationCommand;
import io.gravitee.cockpit.api.command.v1.installation.InstallationCommandPayload;
import io.gravitee.cockpit.api.command.v1.installation.InstallationReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.HashMap;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class InstallationCommandHandlerTest extends TestCase {

    private static final String CUSTOM_VALUE = "customValue";
    private static final String CUSTOM_KEY = "customKey";
    private static final String INSTALLATION_ID = "installation#1";

    @Mock
    private InstallationService installationService;

    public InstallationCommandHandler cut;

    @Before
    public void before() {
        cut = new InstallationCommandHandler(installationService);
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.INSTALLATION.name(), cut.supportType());
    }

    @Test
    public void handle() throws InterruptedException {
        final InstallationEntity installation = new InstallationEntity();
        installation.setId(INSTALLATION_ID);
        installation.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        InstallationCommandPayload installationPayload = InstallationCommandPayload.builder()
            .id(INSTALLATION_ID)
            .status("ACCEPTED")
            .build();
        InstallationCommand command = new InstallationCommand(installationPayload);

        when(installationService.getOrInitialize()).thenReturn(installation);

        TestObserver<InstallationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        final HashMap<String, String> expectedAdditionalInfos = new HashMap<>();
        expectedAdditionalInfos.put(CUSTOM_KEY, CUSTOM_VALUE);
        expectedAdditionalInfos.put(InstallationService.COCKPIT_INSTALLATION_STATUS, "ACCEPTED");
        verify(installationService, times(1)).setAdditionalInformation(expectedAdditionalInfos);
    }

    @Test
    public void handleWithException() throws InterruptedException {
        final InstallationEntity installation = new InstallationEntity();
        installation.setId(INSTALLATION_ID);
        installation.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        InstallationCommandPayload installationPayload = InstallationCommandPayload.builder()
            .id(INSTALLATION_ID)
            .status("ACCEPTED")
            .build();
        InstallationCommand command = new InstallationCommand(installationPayload);

        when(installationService.getOrInitialize()).thenReturn(installation);
        when(installationService.setAdditionalInformation(anyMap())).thenThrow(new TechnicalManagementException());

        TestObserver<InstallationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}
