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
package io.gravitee.rest.api.service.cockpit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.cockpit.command.CockpitCommandService;
import io.gravitee.rest.api.service.cockpit.command.bridge.BridgeCommandFactory;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CockpitServiceTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";

    @Mock
    private BridgeCommandFactory bridgeCommandFactory;

    @Mock
    private CockpitCommandService cockpitCommandService;

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private CockpitService cockpitService;

    @Before
    public void setup() {
        cockpitService = new CockpitServiceImpl(bridgeCommandFactory, cockpitCommandService, objectMapper);
    }

    @Test
    public void shouldNotListEnvironments() {
        // Given
        BridgeMultiReply environmentsMultiReply = new BridgeMultiReply();
        environmentsMultiReply.setCommandStatus(CommandStatus.ERROR);

        when(cockpitCommandService.send(any())).thenReturn(environmentsMultiReply);

        // When
        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = cockpitService.listPromotionTargets(ORGANIZATION_ID);

        // Then
        assertThat(listCockpitReply).isNotNull();
        assertThat(listCockpitReply.getStatus()).isEqualTo(CockpitReplyStatus.ERROR);
        final List<PromotionTargetEntity> environmentEntities = listCockpitReply.getReply();
        assertThat(environmentEntities).isNotNull();
        assertThat(environmentEntities).isEmpty();
    }

    @Test
    public void shouldListEnvironmentsFromSuccessfulReplies() throws JsonProcessingException {
        // Given
        EnvironmentEntity envA = new EnvironmentEntity();
        envA.setId("my-env-A");
        envA.setOrganizationId(ORGANIZATION_ID);
        envA.setName("ENV A");

        BridgeSimpleReply envASimpleReply = new BridgeSimpleReply();
        envASimpleReply.setCommandStatus(CommandStatus.SUCCEEDED);
        envASimpleReply.setInstallationId(INSTALLATION_ID);
        envASimpleReply.setOrganizationId(ORGANIZATION_ID);
        envASimpleReply.setEnvironmentId(envA.getId());
        envASimpleReply.setPayload(objectMapper.writeValueAsString(envA));

        EnvironmentEntity envB = new EnvironmentEntity();
        envB.setId("my-env-B");
        envB.setOrganizationId(ORGANIZATION_ID);
        envB.setName("ENV B");

        BridgeSimpleReply envBSimpleReply = new BridgeSimpleReply();
        envBSimpleReply.setCommandStatus(CommandStatus.SUCCEEDED);
        envBSimpleReply.setInstallationId(INSTALLATION_ID);
        envBSimpleReply.setOrganizationId(ORGANIZATION_ID);
        envBSimpleReply.setEnvironmentId(envB.getId());
        envBSimpleReply.setPayload(objectMapper.writeValueAsString(envB));

        EnvironmentEntity envC_ERROR = new EnvironmentEntity();
        envC_ERROR.setId("my-env-C");
        envC_ERROR.setOrganizationId(ORGANIZATION_ID);
        envC_ERROR.setName("ENV C");

        BridgeSimpleReply envCSimpleReply = new BridgeSimpleReply();
        envCSimpleReply.setCommandStatus(CommandStatus.ERROR);
        envCSimpleReply.setInstallationId(INSTALLATION_ID);
        envCSimpleReply.setOrganizationId(ORGANIZATION_ID);
        envCSimpleReply.setEnvironmentId(envC_ERROR.getId());
        envCSimpleReply.setMessage("Problem while serializing environment: " + envC_ERROR.getId());

        BridgeMultiReply environmentsMultiReply = new BridgeMultiReply();
        environmentsMultiReply.setCommandStatus(CommandStatus.SUCCEEDED);
        environmentsMultiReply.setReplies(Arrays.asList(envASimpleReply, envBSimpleReply, envCSimpleReply));

        when(cockpitCommandService.send(any())).thenReturn(environmentsMultiReply);

        // When
        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = cockpitService.listPromotionTargets(ORGANIZATION_ID);

        // Then
        assertThat(listCockpitReply).isNotNull();
        assertThat(listCockpitReply.getStatus()).isEqualTo(CockpitReplyStatus.SUCCEEDED);
        final List<PromotionTargetEntity> environmentEntities = listCockpitReply.getReply();
        assertThat(environmentEntities).isNotNull();
        assertThat(environmentEntities).isNotEmpty();
        assertThat(environmentEntities.size()).isEqualTo(2);
    }
}
