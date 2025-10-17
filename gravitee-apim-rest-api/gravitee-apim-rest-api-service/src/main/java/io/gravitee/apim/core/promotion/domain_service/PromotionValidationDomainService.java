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
package io.gravitee.apim.core.promotion.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.promotion.exception.PromotionAlreadyInProgressException;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionQuery;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@DomainService
@Slf4j
public class PromotionValidationDomainService {

    private final PromotionQueryService promotionQueryService;

    public PromotionValidationDomainService(PromotionQueryService promotionQueryService) {
        this.promotionQueryService = promotionQueryService;
    }

    public void validate(String apiId, String targetEnvCockpitId) {
        var promotionQuery = PromotionQuery.builder()
            .apiId(apiId)
            .statuses(Set.of(PromotionStatus.CREATED, PromotionStatus.TO_BE_VALIDATED))
            .targetEnvCockpitIds(Set.of(targetEnvCockpitId))
            .build();

        try {
            List<Promotion> inProgressPromotions = promotionQueryService.search(promotionQuery).getContent();
            if (!inProgressPromotions.isEmpty()) {
                Promotion promotion = inProgressPromotions.getFirst();
                throw new PromotionAlreadyInProgressException(promotion.getId());
            }
        } catch (TechnicalManagementException e) {
            log.error("An error occurs while trying to validate a promotion", e);
        }
    }
}
