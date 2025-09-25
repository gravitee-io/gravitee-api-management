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
package io.gravitee.rest.api.service.cockpit.command.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.hello.HelloReply;
import io.gravitee.cockpit.api.command.v1.hello.HelloReplyPayload;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.OrganizationService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class HelloReplyAdapterTest {

    @Mock
    private InstallationService installationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private OrganizationService organizationService;

    private HelloReplyAdapter cut;

    @BeforeEach
    public void beforeEach() {
        cut = new HelloReplyAdapter(installationService, environmentService, organizationService);
    }

    @Test
    void produceType() {
        assertEquals(CockpitCommandType.HELLO.name(), cut.supportType());
    }

    @Test
    void handleReply_shouldUpdateDefaultEnvironmentCockpitId() throws InterruptedException {
        HelloReply helloReply = new HelloReply(
            "commandId",
            HelloReplyPayload.builder().defaultEnvironmentCockpitId("env#cockpit-1").build()
        );

        String defaultEnvId = "DEFAULT";
        EnvironmentEntity defaultEnvironment = new EnvironmentEntity();
        defaultEnvironment.setId(defaultEnvId);
        defaultEnvironment.setOrganizationId("org#1");

        when(installationService.getOrInitialize()).thenReturn(new InstallationEntity());
        when(environmentService.getDefaultOrInitialize()).thenReturn(defaultEnvironment);

        cut
            .adapt(null, helloReply)
            .test()
            .await()
            .assertValue(helloReplyResponse -> {
                assertEquals(helloReply.getCommandId(), helloReplyResponse.getCommandId());
                assertEquals(helloReply.getPayload().getTargetId(), helloReplyResponse.getPayload().getTargetId());
                return true;
            });

        verify(environmentService).createOrUpdate(
            eq(defaultEnvironment.getOrganizationId()),
            eq(defaultEnvId),
            argThat(env -> env.getCockpitId().equals("env#cockpit-1"))
        );
    }

    @Test
    void handleReply_shouldUpdateDefaultOrganizationCockpitId() throws InterruptedException {
        HelloReply helloReply = new HelloReply(
            "commandId",
            HelloReplyPayload.builder().defaultOrganizationCockpitId("org#cockpit-1").build()
        );
        Flow flow = new Flow();
        flow.setName("My-Flow");

        String defaultOrgId = "DEFAULT";
        OrganizationEntity defaultOrganization = new OrganizationEntity();
        defaultOrganization.setId(defaultOrgId);
        defaultOrganization.setFlows(Collections.singletonList(flow));
        defaultOrganization.setFlowMode(FlowMode.DEFAULT);

        when(installationService.getOrInitialize()).thenReturn(new InstallationEntity());
        when(organizationService.getDefaultOrInitialize()).thenReturn(defaultOrganization);

        cut
            .adapt(null, helloReply)
            .test()
            .await()
            .assertValue(helloReplyResponse -> {
                assertEquals(helloReply.getCommandId(), helloReplyResponse.getCommandId());
                assertEquals(helloReply.getPayload().getTargetId(), helloReplyResponse.getPayload().getTargetId());
                return true;
            });

        verify(organizationService).updateOrganization(
            argThat(organizationId -> organizationId.equals(defaultOrgId)),
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
    void handleReply_shouldUpdateCockpitInstallationStatusAndId_butKeepAlreadyExistingInstallationInformations()
        throws InterruptedException {
        // mock already existing installation with informations
        InstallationEntity installation = new InstallationEntity();
        installation.setAdditionalInformation(
            new HashMap<>(
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

        HelloReply helloReply = new HelloReply(
            "commandId",
            HelloReplyPayload.builder().installationId("new-installation-id").installationStatus("new-installation-status").build()
        );

        cut
            .adapt(null, helloReply)
            .test()
            .await()
            .assertValue(helloReplyResponse -> {
                assertEquals(helloReply.getCommandId(), helloReplyResponse.getCommandId());
                assertEquals(helloReply.getPayload().getTargetId(), helloReplyResponse.getPayload().getTargetId());
                return true;
            });

        verify(installationService, times(1)).setAdditionalInformation(
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
        );
    }
}
