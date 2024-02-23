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
package io.gravitee.rest.api.service.cockpit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgeSimpleReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.cockpit.command.CockpitCommandService;
import io.gravitee.rest.api.service.cockpit.command.bridge.BridgeCommandFactory;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CockpitPromotionServiceTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";

    @Mock
    private BridgeCommandFactory bridgeCommandFactory;

    @Mock
    private CockpitCommandService cockpitCommandService;

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private CockpitPromotionService cockpitPromotionService;

    @Before
    public void setUp() {
        cockpitPromotionService = new CockpitPromotionServiceImpl(bridgeCommandFactory, cockpitCommandService, objectMapper);
    }

    @Test
    public void shouldNotListEnvironments() {
        // Given
        BridgeReply bridgeReply = new BridgeReply("commandId", "error");

        when(cockpitCommandService.send(any())).thenReturn(bridgeReply);

        // When
        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = cockpitPromotionService.listPromotionTargets(
            ORGANIZATION_ID,
            GraviteeContext.getCurrentEnvironment()
        );

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

        BridgeReplyPayload.BridgeReplyContent envAContent = BridgeReplyPayload.BridgeReplyContent
            .builder()
            .environmentId(envA.getId())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .content(objectMapper.writeValueAsString(envA))
            .build();

        EnvironmentEntity envB = new EnvironmentEntity();
        envB.setId("my-env-B");
        envB.setOrganizationId(ORGANIZATION_ID);
        envB.setName("ENV B");

        BridgeReplyPayload.BridgeReplyContent envBContent = BridgeReplyPayload.BridgeReplyContent
            .builder()
            .environmentId(envB.getId())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .content(objectMapper.writeValueAsString(envB))
            .build();

        BridgeReply environmentsMultiReply = new BridgeReply("commandId", new BridgeReplyPayload(List.of(envAContent, envBContent)));

        when(cockpitCommandService.send(any())).thenReturn(environmentsMultiReply);

        // When
        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = cockpitPromotionService.listPromotionTargets(
            ORGANIZATION_ID,
            GraviteeContext.getCurrentEnvironment()
        );

        // Then
        assertThat(listCockpitReply).isNotNull();
        assertThat(listCockpitReply.getStatus()).isEqualTo(CockpitReplyStatus.SUCCEEDED);
        final List<PromotionTargetEntity> environmentEntities = listCockpitReply.getReply();
        assertThat(environmentEntities).isNotNull();
        assertThat(environmentEntities).isNotEmpty();
        assertThat(environmentEntities.size()).isEqualTo(2);
    }

    @Test
    public void shouldNotProcessPromotionCommandError() {
        BridgeReply reply = new BridgeReply("commandId", "error");
        when(cockpitCommandService.send(any())).thenReturn(reply);

        final CockpitReply<PromotionEntity> result = cockpitPromotionService.processPromotion(
            GraviteeContext.getExecutionContext(),
            new PromotionEntity()
        );

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CockpitReplyStatus.ERROR);
        final PromotionEntity entity = result.getReply();
        assertThat(entity).isNull();
    }

    @Test
    public void shouldProcessPromotion() {
        BridgeReply reply = new BridgeReply("commandid", new BridgeReplyPayload(List.of()));
        when(cockpitCommandService.send(any())).thenReturn(reply);

        final PromotionEntity promotionEntity = new PromotionEntity();

        final CockpitReply<PromotionEntity> result = cockpitPromotionService.processPromotion(
            GraviteeContext.getExecutionContext(),
            promotionEntity
        );

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CockpitReplyStatus.SUCCEEDED);
        final PromotionEntity entity = result.getReply();
        assertThat(entity).isNotNull();
    }
}
