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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class BridgeCommandFactoryTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";
    private static final String ENVIRONMENT_ID = "my-environment-id";

    @Mock
    private InstallationService installationService;

    private BridgeCommandFactory bridgeCommandFactory;

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @BeforeEach
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
        final BridgeCommand listEnvironmentCommand = bridgeCommandFactory.createListEnvironmentCommand(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        // Then
        assertThat(listEnvironmentCommand).isNotNull();
        assertThat(listEnvironmentCommand.getPayload().environmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(listEnvironmentCommand.getPayload().organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(listEnvironmentCommand.getPayload().installationId()).isEqualTo(INSTALLATION_ID);
        assertThat(listEnvironmentCommand.getPayload().operation()).isEqualTo(BridgeOperation.LIST_ENVIRONMENT.name());

        final BridgeCommandPayload.BridgeTarget bridgeTarget = listEnvironmentCommand.getPayload().target();
        assertThat(bridgeTarget).isNotNull();
        assertThat(bridgeTarget.scopes()).isNotNull();
        assertThat(bridgeTarget.scopes().size()).isEqualTo(1);
        assertThat(bridgeTarget.scopes().get(0)).isEqualTo("APIM");
    }

    @Test
    public void shouldCreatePromoteApiCommand() {
        PromotionEntity promotionEntity = new PromotionEntity();
        promotionEntity.setTargetEnvCockpitId("env#target");

        final BridgeCommand promoteApiCommand = bridgeCommandFactory.createPromoteApiCommand(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            "env#target",
            "{ \"id\": \"test\"}"
        );

        assertThat(promoteApiCommand).isNotNull();
        assertThat(promoteApiCommand.getPayload().environmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(promoteApiCommand.getPayload().organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(promoteApiCommand.getPayload().installationId()).isEqualTo(INSTALLATION_ID);
        assertThat(promoteApiCommand.getPayload().operation()).isEqualTo(BridgeOperation.PROMOTE_API.name());
        assertThat(promoteApiCommand.getPayload().content()).isEqualTo("{ \"id\": \"test\"}");

        final BridgeCommandPayload.BridgeTarget bridgeTarget = promoteApiCommand.getPayload().target();
        assertThat(bridgeTarget.environmentId()).isEqualTo("env#target");
    }
}
