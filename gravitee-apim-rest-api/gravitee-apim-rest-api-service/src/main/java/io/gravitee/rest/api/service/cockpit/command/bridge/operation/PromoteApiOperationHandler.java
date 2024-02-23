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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.legacy.bridge.BridgeSimpleReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PromoteApiOperationHandler implements BridgeOperationHandler {

    private final PromotionService promotionService;
    private final InstallationService installationService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String bridgeOperation) {
        return Objects.equals(BridgeOperation.PROMOTE_API.name(), bridgeOperation);
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
        BridgeCommandPayload commandPayload = bridgeCommand.getPayload();

        final PromotionEntity promotionEntity;

        try {
            promotionEntity = objectMapper.readValue(commandPayload.content(), PromotionEntity.class);
        } catch (JsonProcessingException e) {
            String errorDetails =
                "Problem while deserializing promotion request for environment [%s]".formatted(commandPayload.environmentId());
            log.warn(errorDetails, e);
            return Single.just(new BridgeReply(bridgeCommand.getId(), errorDetails));
        }

        promotionEntity.setStatus(PromotionEntityStatus.TO_BE_VALIDATED);

        return Single
            .fromCallable(() -> promotionService.createOrUpdate(promotionEntity))
            .subscribeOn(Schedulers.io())
            .map(promotion -> {
                try {
                    return new BridgeReply(
                        bridgeCommand.getId(),
                        new BridgeReplyPayload(
                            List.of(
                                BridgeReplyPayload.BridgeReplyContent
                                    .builder()
                                    .environmentId(commandPayload.target().environmentId())
                                    .organizationId(commandPayload.organizationId())
                                    .installationId(installationService.get().getId())
                                    .content(objectMapper.writeValueAsString(promotion))
                                    .build()
                            )
                        )
                    );
                } catch (JsonProcessingException e) {
                    String errorDetails =
                        "Problem while serializing promotion request for environment [%s]".formatted(
                                bridgeCommand.getPayload().environmentId()
                            );
                    log.warn(errorDetails);
                    return new BridgeReply(bridgeCommand.getId(), errorDetails);
                }
            })
            .onErrorReturn(throwable -> {
                String errorDetails =
                    "Problem while serializing promotion request for environment [%s]".formatted(
                            bridgeCommand.getPayload().environmentId()
                        );
                log.warn(errorDetails);
                return new BridgeReply(bridgeCommand.getId(), errorDetails);
            });
    }
}
