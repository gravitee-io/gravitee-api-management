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
package io.gravitee.rest.api.service.cockpit.command.producer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.hello.HelloCommand;
import io.gravitee.cockpit.api.command.hello.HelloPayload;
import io.gravitee.cockpit.api.command.hello.HelloReply;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.observers.TestObserver;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
public class HelloCommandProducerTest {

    private static final String HOSTNAME = "test.gravitee.io";
    private static final String CUSTOM_VALUE = "customValue";
    private static final String CUSTOM_KEY = "customKey";
    private static final String INSTALLATION_ID = "installation#1";

    @Mock
    private InstallationService installationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private Node node;

    private HelloCommandProducer cut;

    @Before
    public void before() {
        cut = new HelloCommandProducer(node, installationService, environmentService, organizationService);
    }

    @Test
    public void produceType() {
        assertEquals(Command.Type.HELLO_COMMAND, cut.produceType());
    }

    @Test
    public void produce() {
        final InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
        installationEntity.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        when(node.hostname()).thenReturn(HOSTNAME);
        when(installationService.getOrInitialize()).thenReturn(installationEntity);

        final HelloCommand command = new HelloCommand();
        final HelloPayload payload = new HelloPayload();
        payload.setNode(new io.gravitee.cockpit.api.command.Node());
        command.setPayload(payload);
        final TestObserver<HelloCommand> obs = cut.prepare(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            helloCommand -> {
                assertEquals(CUSTOM_VALUE, helloCommand.getPayload().getAdditionalInformation().get(CUSTOM_KEY));
                assertTrue(helloCommand.getPayload().getAdditionalInformation().containsKey("UI_URL"));
                assertTrue(helloCommand.getPayload().getAdditionalInformation().containsKey("API_URL"));

                assertEquals(HOSTNAME, helloCommand.getPayload().getNode().getHostname());
                assertEquals(GraviteeContext.getDefaultOrganization(), helloCommand.getPayload().getDefaultOrganizationId());
                assertEquals(GraviteeContext.getDefaultEnvironment(), helloCommand.getPayload().getDefaultEnvironmentId());
                assertEquals(INSTALLATION_ID, helloCommand.getPayload().getNode().getInstallationId());
                assertEquals(HOSTNAME, helloCommand.getPayload().getNode().getHostname());

                return true;
            }
        );
    }

    @Test(expected = TechnicalManagementException.class)
    public void produceWithException() {
        when(installationService.getOrInitialize()).thenThrow(new TechnicalManagementException());
        final TestObserver<HelloCommand> obs = cut.prepare(new HelloCommand()).test();
    }

    @Test
    public void handleReply_shouldUpdateDefaultEnvironmentCockpitId() {
        HelloReply helloReply = new HelloReply();
        helloReply.setCommandStatus(CommandStatus.SUCCEEDED);
        helloReply.setDefaultEnvironmentCockpitId("env#cockpit-1");

        String defaultEnvId = "DEFAULT";
        EnvironmentEntity defaultEnvironment = new EnvironmentEntity();
        defaultEnvironment.setId(defaultEnvId);
        defaultEnvironment.setOrganizationId("org#1");

        when(installationService.getOrInitialize()).thenReturn(new InstallationEntity());
        when(environmentService.findById(defaultEnvId)).thenReturn(defaultEnvironment);

        cut.handleReply(helloReply);

        verify(environmentService)
            .createOrUpdate(
                eq(defaultEnvironment.getOrganizationId()),
                eq(defaultEnvId),
                argThat(env -> env.getCockpitId().equals("env#cockpit-1"))
            );
    }

    @Test
    public void handleReply_shouldUpdateDefaultOrganizationCockpitId() {
        HelloReply helloReply = new HelloReply();
        helloReply.setCommandStatus(CommandStatus.SUCCEEDED);
        helloReply.setDefaultOrganizationCockpitId("org#cockpit-1");

        Flow flow = new Flow();
        flow.setName("My-Flow");

        String defaultOrgId = "DEFAULT";
        OrganizationEntity defaultOrganization = new OrganizationEntity();
        defaultOrganization.setId(defaultOrgId);
        defaultOrganization.setFlows(Collections.singletonList(flow));
        defaultOrganization.setFlowMode(FlowMode.DEFAULT);

        when(installationService.getOrInitialize()).thenReturn(new InstallationEntity());
        when(organizationService.findById(defaultOrgId)).thenReturn(defaultOrganization);

        cut.handleReply(helloReply);

        verify(organizationService)
            .createOrUpdate(
                argThat(executionContext -> executionContext.getOrganizationId().equals(defaultOrgId)),
                argThat(
                    org ->
                        org.getCockpitId().equals("org#cockpit-1") &&
                        FlowMode.DEFAULT.equals(org.getFlowMode()) &&
                        org.getFlows() != null &&
                        org.getFlows().size() == 1 &&
                        org.getFlows().get(0).getName().equals("My-Flow")
                )
            );
    }

    @Test
    public void handleReply_shouldUpdateCockpitInstallationStatusAndId_butKeepAlreadyExistingInstallationInformations() {
        // mock already existing installation with informations
        InstallationEntity installation = new InstallationEntity();
        installation.setAdditionalInformation(
            new HashMap(
                Map.of(
                    "key1",
                    "value1",
                    "key2",
                    "value2",
                    "COCKPIT_INSTALLATION_STATUS",
                    "old-installation-status",
                    "COCKPIT_INSTALLATION_ID",
                    "old-installation-id"
                )
            )
        );
        when(installationService.getOrInitialize()).thenReturn(installation);

        HelloReply helloReply = new HelloReply();
        helloReply.setInstallationId("new-installation-id");
        helloReply.setInstallationStatus("new-installation-status");
        helloReply.setCommandStatus(CommandStatus.SUCCEEDED);

        cut.handleReply(helloReply);

        verify(installationService, times(1))
            .setAdditionalInformation(
                eq(
                    Map.of(
                        "key1",
                        "value1",
                        "key2",
                        "value2",
                        "COCKPIT_INSTALLATION_STATUS",
                        "new-installation-status",
                        "COCKPIT_INSTALLATION_ID",
                        "new-installation-id"
                    )
                )
            );
    }
}
