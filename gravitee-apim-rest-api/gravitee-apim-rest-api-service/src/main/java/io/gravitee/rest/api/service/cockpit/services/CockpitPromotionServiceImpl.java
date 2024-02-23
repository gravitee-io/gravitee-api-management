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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.cockpit.command.CockpitCommandService;
import io.gravitee.rest.api.service.cockpit.command.bridge.BridgeCommandFactory;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CockpitPromotionServiceImpl implements CockpitPromotionService {

    private final BridgeCommandFactory bridgeCommandFactory;
    private final CockpitCommandService cockpitCommandService;
    private final ObjectMapper objectMapper;

    @Override
    public CockpitReply<List<PromotionTargetEntity>> listPromotionTargets(String organizationId, String environmentId) {
        final BridgeCommand listEnvironmentCommand = this.bridgeCommandFactory.createListEnvironmentCommand(organizationId, environmentId);
        BridgeReply bridgeReply = cockpitCommandService.send(listEnvironmentCommand);

        if (bridgeReply.getCommandStatus() != CommandStatus.SUCCEEDED) {
            log.warn("Problem while listing promotion targets through cockpit. \n {}", bridgeReply.getErrorDetails());
            return new CockpitReply<>(Collections.emptyList(), CockpitReplyStatus.ERROR);
        }

        final List<PromotionTargetEntity> environmentEntities = bridgeReply
            .getPayload()
            .contents()
            .stream()
            .filter(bridgeReplyContent -> bridgeReplyContent.content() != null)
            .map(bridgeReplyContent -> {
                try {
                    final EnvironmentEntity environmentEntity =
                        this.objectMapper.readValue(bridgeReplyContent.content(), EnvironmentEntity.class);

                    // Be careful with env and org ids, we need to use the one from the reply and not the payload
                    // because cockpit has updated them to handle the case were id is "DEFAULT"
                    return new PromotionTargetEntity(
                        environmentEntity,
                        bridgeReplyContent.organizationId(),
                        bridgeReplyContent.environmentId(),
                        bridgeReplyContent.installationId()
                    );
                } catch (JsonProcessingException e) {
                    log.warn(
                        "Problem while deserializing environment {} with payload {}",
                        bridgeReplyContent.environmentId(),
                        bridgeReplyContent.content()
                    );
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
        return new CockpitReply<>(environmentEntities, CockpitReplyStatus.SUCCEEDED);
    }

    @Override
    public CockpitReply<PromotionEntity> requestPromotion(ExecutionContext executionContext, PromotionEntity promotionEntity) {
        String serializedPromotion = null;
        try {
            serializedPromotion = objectMapper.writeValueAsString(promotionEntity);
        } catch (JsonProcessingException e) {
            log.warn("Problem while serializing promotion {}", promotionEntity.getId());
        }

        final BridgeCommand promoteApiCommand =
            this.bridgeCommandFactory.createPromoteApiCommand(
                    executionContext.getOrganizationId(),
                    executionContext.getEnvironmentId(),
                    promotionEntity.getTargetEnvCockpitId(),
                    serializedPromotion
                );
        BridgeReply bridgeReply = cockpitCommandService.send(promoteApiCommand);

        if (bridgeReply.getCommandStatus() != CommandStatus.SUCCEEDED) {
            log.warn("Problem while sending API promotion request through cockpit. \n {}", bridgeReply.getErrorDetails());
            return new CockpitReply<>(null, CockpitReplyStatus.ERROR);
        }

        return new CockpitReply<>(promotionEntity, CockpitReplyStatus.SUCCEEDED);
    }

    @Override
    public CockpitReply<PromotionEntity> processPromotion(ExecutionContext executionContext, PromotionEntity promotionEntity) {
        String serializedPromotion = null;
        try {
            serializedPromotion = objectMapper.writeValueAsString(promotionEntity);
        } catch (JsonProcessingException e) {
            log.warn("Problem while serializing promotion {}", promotionEntity.getId());
        }

        final BridgeCommand processPromotionCommand =
            this.bridgeCommandFactory.createProcessPromotionCommand(
                    executionContext.getOrganizationId(),
                    executionContext.getEnvironmentId(),
                    promotionEntity.getSourceEnvCockpitId(),
                    serializedPromotion
                );
        final BridgeReply bridgeReply = cockpitCommandService.send(processPromotionCommand);

        if (bridgeReply.getCommandStatus() != CommandStatus.SUCCEEDED) {
            log.warn("Problem while processing API promotion request through cockpit. \n {}", bridgeReply.getErrorDetails());
            return new CockpitReply<>(null, CockpitReplyStatus.ERROR);
        }

        return new CockpitReply<>(promotionEntity, CockpitReplyStatus.SUCCEEDED);
    }
}
