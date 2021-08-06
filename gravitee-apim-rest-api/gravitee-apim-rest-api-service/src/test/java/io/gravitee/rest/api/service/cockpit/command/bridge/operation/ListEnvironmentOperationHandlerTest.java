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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.observers.TestObserver;
import java.util.Arrays;
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
public class ListEnvironmentOperationHandlerTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";
    private static final String ENVIRONMENT_ID = "my-environment-id";
    private static final String COMMAND_ID = "my-command-id";
    public ListEnvironmentOperationHandler cut;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private InstallationEntity installationEntity;

    @Mock
    private InstallationService installationService;

    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void before() {
        cut = new ListEnvironmentOperationHandler(environmentService, installationService, objectMapper);
        installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
    }

    @Test
    public void handleOperation() {
        assertTrue(cut.canHandle(BridgeOperation.LIST_ENVIRONMENT.name()));
    }

    @Test
    public void shouldListEnvironments() throws JsonProcessingException {
        // Given
        EnvironmentEntity envA = new EnvironmentEntity();
        envA.setId("my-env-A");
        envA.setOrganizationId(ORGANIZATION_ID);
        envA.setName("ENV A");

        EnvironmentEntity envB = new EnvironmentEntity();
        envB.setId("my-env-B");
        envB.setOrganizationId(ORGANIZATION_ID);
        envB.setName("ENV B");

        EnvironmentEntity envC_ERROR = new EnvironmentEntity();
        envC_ERROR.setId("my-env-C");
        envC_ERROR.setOrganizationId(ORGANIZATION_ID);
        envC_ERROR.setName("ENV C");

        when(environmentService.findByOrganization(ORGANIZATION_ID)).thenReturn(Arrays.asList(envA, envB, envC_ERROR));
        when(objectMapper.writeValueAsString(envA)).thenReturn("envA");
        when(objectMapper.writeValueAsString(envB)).thenReturn("envB");
        when(objectMapper.writeValueAsString(envC_ERROR)).thenThrow(new JsonProcessingException("") {});

        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);

        when(installationService.get()).thenReturn(installationEntity);

        BridgeCommand command = new BridgeCommand();
        command.setOperation(BridgeOperation.LIST_ENVIRONMENT.name());
        command.setId(COMMAND_ID);
        command.setInstallationId(INSTALLATION_ID);
        command.setOrganizationId(ORGANIZATION_ID);
        command.setEnvironmentId(ENVIRONMENT_ID);

        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.awaitTerminalEvent();
        obs.assertValue(
            reply -> {
                if (
                    reply.getCommandId().equals(command.getId()) &&
                    reply.getCommandStatus().equals(CommandStatus.SUCCEEDED) &&
                    BridgeMultiReply.class.isInstance(reply)
                ) {
                    BridgeMultiReply multiReply = ((BridgeMultiReply) reply);
                    if (multiReply.getReplies() != null && multiReply.getReplies().size() == 3) {
                        for (BridgeSimpleReply simpleReply : multiReply.getReplies()) {
                            if (
                                simpleReply.getEnvironmentId().equals(envA.getId()) && simpleReply.getCommandStatus() == CommandStatus.ERROR
                            ) {
                                return false;
                            }
                            if (
                                simpleReply.getEnvironmentId().equals(envB.getId()) && simpleReply.getCommandStatus() == CommandStatus.ERROR
                            ) {
                                return false;
                            }
                            if (
                                simpleReply.getEnvironmentId().equals(envC_ERROR.getId()) &&
                                simpleReply.getCommandStatus() == CommandStatus.SUCCEEDED
                            ) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        );
    }

    @Test
    public void shouldNotListEnvironments() {
        // Given
        when(environmentService.findByOrganization(ORGANIZATION_ID)).thenThrow(new TechnicalManagementException());

        BridgeCommand command = new BridgeCommand();
        command.setOrganizationId(ORGANIZATION_ID);

        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.awaitTerminalEvent();
        obs.assertValue(
            reply ->
                reply.getCommandId().equals(command.getId()) &&
                reply.getCommandStatus().equals(CommandStatus.ERROR) &&
                reply.getMessage().equals("No environment available for organization: " + ORGANIZATION_ID)
        );
    }
}
