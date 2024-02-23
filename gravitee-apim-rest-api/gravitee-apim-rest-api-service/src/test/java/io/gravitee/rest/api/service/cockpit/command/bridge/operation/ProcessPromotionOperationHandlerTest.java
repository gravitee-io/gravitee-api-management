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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.rxjava3.observers.TestObserver;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessPromotionOperationHandlerTest {

    private static final String INSTALLATION_ID = "my-installation-id";
    private static final String ORGANIZATION_ID = "my-organization-id";
    private static final String ENVIRONMENT_ID = "my-environment-id";
    private static final String COMMAND_ID = "my-command-id";

    private ProcessPromotionOperationHandler cut;

    @Mock
    private PromotionService promotionService;

    @Mock
    private InstallationService installationService;

    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        cut = new ProcessPromotionOperationHandler(promotionService, installationService, objectMapper);
    }

    @Test
    public void handleOperation() {
        assertTrue(cut.canHandle(BridgeOperation.PROCESS_API_PROMOTION.name()));
    }

    @Test
    public void shouldHandlePromotionRequest() throws JsonProcessingException, InterruptedException {
        BridgeCommandPayload bridgeCommandPayload = BridgeCommandPayload
            .builder()
            .operation(BridgeOperation.PROMOTE_API.name())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .target(new BridgeCommandPayload.BridgeTarget(null, "source"))
            .build();
        BridgeCommand command = new BridgeCommand(bridgeCommandPayload);

        ArgumentCaptor<PromotionEntity> argument = ArgumentCaptor.forClass(PromotionEntity.class);

        when(objectMapper.readValue(command.getPayload().content(), PromotionEntity.class)).thenReturn(getAPromotionEntity());
        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
        when(installationService.get()).thenReturn(installationEntity);

        TestObserver<BridgeReply> obs = cut.handle(command).test();

        verify(promotionService, times(1)).createOrUpdate(argument.capture());

        Assertions.assertThat(argument.getValue().getStatus()).isEqualTo(PromotionEntityStatus.ACCEPTED);

        obs.await();
        obs.assertValue(reply -> {
            BridgeReplyPayload.BridgeReplyContent bridgeReplyContent = reply.getPayload().contents().get(0);
            return (
                reply.getCommandStatus().equals(CommandStatus.SUCCEEDED) &&
                bridgeReplyContent.organizationId().equals(ORGANIZATION_ID) &&
                bridgeReplyContent.environmentId().equals("source") &&
                bridgeReplyContent.installationId().equals(INSTALLATION_ID) &&
                reply.getCommandId().equals(command.getId())
            );
        });
    }

    @Test
    public void shouldHandlePromotionRequestIfCannotReadPromotionEntity() throws JsonProcessingException, InterruptedException {
        BridgeCommandPayload payload = BridgeCommandPayload
            .builder()
            .operation(BridgeOperation.PROMOTE_API.name())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .target(new BridgeCommandPayload.BridgeTarget(null, "source"))
            .build();
        BridgeCommand command = new BridgeCommand(payload);

        when(objectMapper.readValue(command.getPayload().content(), PromotionEntity.class)).thenThrow(JsonMappingException.class);

        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.await();
        obs.assertValue(reply ->
            reply.getCommandId().equals(command.getId()) &&
            reply.getCommandStatus().equals(CommandStatus.ERROR) &&
            reply.getErrorDetails().equals("Problem while deserializing promotion for environment [" + ENVIRONMENT_ID + "]")
        );
    }

    @Test
    public void shouldHandlePromotionRequestIfCannotWritePayload() throws JsonProcessingException, InterruptedException {
        BridgeCommandPayload bridgeCommandPayload = BridgeCommandPayload
            .builder()
            .operation(BridgeOperation.PROMOTE_API.name())
            .installationId(INSTALLATION_ID)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .target(new BridgeCommandPayload.BridgeTarget(null, "source"))
            .build();
        BridgeCommand command = new BridgeCommand(bridgeCommandPayload);

        when(objectMapper.readValue(command.getPayload().content(), PromotionEntity.class)).thenReturn(getAPromotionEntity());
        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
        when(installationService.get()).thenReturn(installationEntity);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(promotionService.createOrUpdate(any())).thenReturn(getAPromotionEntity());

        // When
        TestObserver<BridgeReply> obs = cut.handle(command).test();

        // Then
        obs.await();
        obs.assertValue(reply ->
            reply.getCommandId().equals(command.getId()) &&
            reply.getCommandStatus().equals(CommandStatus.ERROR) &&
            reply.getErrorDetails().equals("Problem while serializing promotion for environment [" + ENVIRONMENT_ID + "]")
        );
    }

    private PromotionEntity getAPromotionEntity() {
        final PromotionEntity promotionEntity = new PromotionEntity();
        promotionEntity.setSourceEnvCockpitId("sourceEnvId");
        promotionEntity.setTargetEnvCockpitId("targetEnvId");
        promotionEntity.setApiDefinition("definition");
        promotionEntity.setStatus(PromotionEntityStatus.ACCEPTED);
        return promotionEntity;
    }
}
