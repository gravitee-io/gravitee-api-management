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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.goodbye.GoodByeCommand;
import io.gravitee.exchange.api.command.goodbye.GoodByeReply;
import io.gravitee.exchange.api.command.goodbye.GoodByeReplyPayload;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoodByeCommandHandler implements CommandHandler<GoodByeCommand, GoodByeReply> {

    static final String DELETED_STATUS = "DELETED";
    private final InstallationService installationService;
    private final PromotionService promotionService;

    @Override
    public String supportType() {
        return GoodByeCommand.COMMAND_TYPE;
    }

    @Override
    public Single<GoodByeReply> handle(GoodByeCommand command) {
        final Map<String, String> additionalInformation = this.installationService.getOrInitialize().getAdditionalInformation();
        additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, DELETED_STATUS);

        rejectAllPromotionToValidate();

        try {
            this.installationService.setAdditionalInformation(additionalInformation);
            log.info("Installation status is [{}].", DELETED_STATUS);
            return Single.just(new GoodByeReply(command.getId(), new GoodByeReplyPayload()));
        } catch (Exception ex) {
            String errorDetails = "Error occurred when deleting installation.";
            log.info(errorDetails, ex);
            return Single.just(new GoodByeReply(command.getId(), errorDetails));
        }
    }

    private void rejectAllPromotionToValidate() {
        PromotionQuery promotionQuery = new PromotionQuery();
        promotionQuery.setStatuses(List.of(PromotionEntityStatus.TO_BE_VALIDATED));

        promotionService
            .search(promotionQuery, null, null)
            .getContent()
            .forEach(promotionEntity -> {
                promotionEntity.setStatus(PromotionEntityStatus.REJECTED);
                promotionService.createOrUpdate(promotionEntity);
            });
    }
}
