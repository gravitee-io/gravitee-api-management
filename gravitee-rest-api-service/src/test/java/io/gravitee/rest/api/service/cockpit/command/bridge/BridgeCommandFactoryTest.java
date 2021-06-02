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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.cockpit.api.command.bridge.BridgeTarget;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.CockpitCommandService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.cockpit.services.CockpitService;
import io.gravitee.rest.api.service.cockpit.services.CockpitServiceImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeCommandFactoryTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";
    private static final String ENVIRONMENT_ID = "my-environment-id";

    @Mock
    private InstallationService installationService;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateListeEnvironmentsCommand() {
        // Given
        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);

        when(installationService.getOrInitialize()).thenReturn(installationEntity);

        BridgeCommandFactory bridgeCommandFactory = new BridgeCommandFactory(installationService);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        // When
        final BridgeCommand listEnvironmentCommand = bridgeCommandFactory.createListEnvironmentCommand();

        // Then
        assertThat(listEnvironmentCommand).isNotNull();
        assertThat(listEnvironmentCommand.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(listEnvironmentCommand.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(listEnvironmentCommand.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(listEnvironmentCommand.getOperation()).isEqualTo(BridgeOperation.LIST_ENVIRONMENT.name());
        assertThat(listEnvironmentCommand.getTimeoutMillis()).isEqualTo(3000L);

        final BridgeTarget bridgeTarget = listEnvironmentCommand.getTarget();
        assertThat(bridgeTarget).isNotNull();
        assertThat(bridgeTarget.getScopes()).isNotNull();
        assertThat(bridgeTarget.getScopes().size()).isEqualTo(1);
        assertThat(bridgeTarget.getScopes().get(0)).isEqualTo("APIM");
    }
}
