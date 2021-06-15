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
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeTarget;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeCommandFactoryTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";
    private static final String ENVIRONMENT_ID = "my-environment-id";

    @Mock
    private InstallationService installationService;

    private BridgeCommandFactory bridgeCommandFactory;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Before
    public void setup() {
        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);

        when(installationService.get()).thenReturn(installationEntity);
        bridgeCommandFactory = new BridgeCommandFactory(installationService);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @Test
    public void shouldCreateListEnvironmentsCommand() {
        // When
        final BridgeCommand listEnvironmentCommand = bridgeCommandFactory.createListEnvironmentCommand();

        // Then
        assertThat(listEnvironmentCommand).isNotNull();
        assertThat(listEnvironmentCommand.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(listEnvironmentCommand.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(listEnvironmentCommand.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(listEnvironmentCommand.getOperation()).isEqualTo(BridgeOperation.LIST_ENVIRONMENT.name());

        final BridgeTarget bridgeTarget = listEnvironmentCommand.getTarget();
        assertThat(bridgeTarget).isNotNull();
        assertThat(bridgeTarget.getScopes()).isNotNull();
        assertThat(bridgeTarget.getScopes().size()).isEqualTo(1);
        assertThat(bridgeTarget.getScopes().get(0)).isEqualTo("APIM");
    }

    @Test
    public void shouldCreatePromoteApiCommand() {
        PromotionEntity promotionEntity = new PromotionEntity();
        promotionEntity.setTargetEnvironmentId("env#target");

        final BridgeCommand promoteApiCommand = bridgeCommandFactory.createPromoteApiCommand("env#target", "{ \"id\": \"test\"}");

        assertThat(promoteApiCommand).isNotNull();
        assertThat(promoteApiCommand.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(promoteApiCommand.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(promoteApiCommand.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(promoteApiCommand.getOperation()).isEqualTo(BridgeOperation.PROMOTE_API.name());
        assertThat(promoteApiCommand.getPayload().getContent()).isEqualTo("{ \"id\": \"test\"}");

        final BridgeTarget bridgeTarget = promoteApiCommand.getTarget();
        assertThat(bridgeTarget.getEnvironmentId()).isEqualTo("env#target");
    }
}
