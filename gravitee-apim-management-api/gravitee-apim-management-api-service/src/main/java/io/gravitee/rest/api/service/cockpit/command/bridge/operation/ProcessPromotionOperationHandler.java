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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.Single;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ProcessPromotionOperationHandler implements BridgeOperationHandler {

    private final Logger logger = LoggerFactory.getLogger(ProcessPromotionOperationHandler.class);

    private final PromotionService promotionService;
    private final InstallationService installationService;
    private final ObjectMapper objectMapper;

    public ProcessPromotionOperationHandler(
        PromotionService promotionService,
        InstallationService installationService,
        ObjectMapper objectMapper
    ) {
        this.promotionService = promotionService;
        this.installationService = installationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(String bridgeOperation) {
        return Objects.equals(BridgeOperation.PROCESS_API_PROMOTION.name(), bridgeOperation);
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
        BridgeSimpleReply reply = new BridgeSimpleReply();
        reply.setCommandId(bridgeCommand.getId());

        final PromotionEntity promotionEntity;

        try {
            promotionEntity = objectMapper.readValue(bridgeCommand.getPayload().getContent(), PromotionEntity.class);
        } catch (JsonProcessingException e) {
            logger.warn("Problem while deserializing promotion for environment {}", bridgeCommand.getEnvironmentId());
            reply.setCommandStatus(CommandStatus.ERROR);
            reply.setMessage("Problem while deserializing promotion for environment [" + bridgeCommand.getEnvironmentId() + "]");
            return Single.just(reply);
        }

        PromotionEntity promotion = promotionService.createOrUpdate(promotionEntity);

        reply.setCommandStatus(CommandStatus.SUCCEEDED);
        reply.setOrganizationId(bridgeCommand.getOrganizationId());
        reply.setEnvironmentId(bridgeCommand.getTarget().getEnvironmentId());
        reply.setInstallationId(installationService.get().getId());

        try {
            reply.setPayload(objectMapper.writeValueAsString(promotion));
        } catch (JsonProcessingException e) {
            logger.warn("Problem while serializing promotion for environment {}", promotion.getId());
            reply.setCommandStatus(CommandStatus.ERROR);
            reply.setMessage("Problem while serializing promotion for environment [" + bridgeCommand.getEnvironmentId() + "]");
            return Single.just(reply);
        }

        return Single.just(reply);
    }
}
