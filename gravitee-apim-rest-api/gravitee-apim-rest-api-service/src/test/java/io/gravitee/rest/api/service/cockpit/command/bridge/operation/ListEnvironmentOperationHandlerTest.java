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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgeSimpleReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Arrays;
import java.util.List;
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
    public void shouldListEnvironments() throws JsonProcessingException, InterruptedException {
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

        BridgeCommandPayload bridgeCommandPayload = BridgeCommandPayload.builder()
            .operation(BridgeOperation.LIST_ENVIRONMENT.name())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();

        BridgeCommand command = new BridgeCommand(bridgeCommandPayload);
        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.await();
        obs.assertValue(reply -> {
            if (reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED)) {
                BridgeReplyPayload replyPayload = reply.getPayload();
                List<BridgeReplyPayload.BridgeReplyContent> contents = replyPayload.contents();
                assertEquals(3, contents.size());
                for (BridgeReplyPayload.BridgeReplyContent replyContent : contents) {
                    if (replyContent.getEnvironmentId().equals(envA.getId()) && replyContent.isError()) {
                        return false;
                    }
                    if (replyContent.getEnvironmentId().equals(envB.getId()) && replyContent.isError()) {
                        return false;
                    }
                    if (replyContent.getEnvironmentId().equals(envC_ERROR.getId()) && !replyContent.isError()) {
                        return false;
                    }
                }
                assertTrue(contents.stream().anyMatch(bridgeReplyContent -> bridgeReplyContent.getEnvironmentId().equals(envA.getId())));
                assertTrue(contents.stream().anyMatch(bridgeReplyContent -> bridgeReplyContent.getEnvironmentId().equals(envB.getId())));
                assertTrue(
                    contents.stream().anyMatch(bridgeReplyContent -> bridgeReplyContent.getEnvironmentId().equals(envC_ERROR.getId()))
                );
                return true;
            }
            return false;
        });
    }

    @Test
    public void shouldNotListEnvironments() throws InterruptedException {
        // Given
        when(environmentService.findByOrganization(ORGANIZATION_ID)).thenThrow(new TechnicalManagementException());

        BridgeCommandPayload bridgeCommandPayload = BridgeCommandPayload.builder().organizationId(ORGANIZATION_ID).build();

        BridgeCommand command = new BridgeCommand(bridgeCommandPayload);

        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.await();
        obs.assertValue(
            reply ->
                reply.getCommandId().equals(command.getId()) &&
                reply.getCommandStatus().equals(CommandStatus.ERROR) &&
                reply.getErrorDetails().equals("No environment available for organization: " + ORGANIZATION_ID)
        );
    }
}
