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
package io.gravitee.apim.infra.domain_service.promotion;

import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.infra.adapter.PromotionAdapter;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.cockpit.services.CockpitPromotionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.promotion.PromotionService;
import org.springframework.stereotype.Service;

@Service
public class CockpitPromotionServiceProviderImpl implements CockpitPromotionServiceProvider {

    private final CockpitPromotionService cockpitPromotionService;
    private final PromotionService promotionService;

    public CockpitPromotionServiceProviderImpl(CockpitPromotionService cockpitPromotionService, PromotionService promotionService) {
        this.cockpitPromotionService = cockpitPromotionService;
        this.promotionService = promotionService;
    }

    @Override
    public CockpitReplyStatus requestPromotion(String organizationId, String environmentId, Promotion promotion) {
        var cockpitReply = cockpitPromotionService.requestPromotion(
            new ExecutionContext(organizationId, environmentId),
            PromotionAdapter.INSTANCE.toRestApiModel(promotion)
        );
        return cockpitReply.getStatus() == io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus.SUCCEEDED
            ? CockpitReplyStatus.SUCCEEDED
            : CockpitReplyStatus.ERROR;
    }

    @Override
    public Promotion createPromotion(String apiId, PromotionRequest promotionRequest, String userId) {
        PromotionEntity created = promotionService.create(
            GraviteeContext.getExecutionContext(),
            GraviteeContext.getCurrentEnvironment(),
            apiId,
            PromotionAdapter.INSTANCE.toRestApiModel(promotionRequest),
            userId
        );
        return PromotionAdapter.INSTANCE.toCoreModel(created);
    }

    @Override
    public Promotion process(String promotionId, boolean isAccepted) {
        var processed = promotionService.processPromotion(GraviteeContext.getExecutionContext(), promotionId, isAccepted);
        return PromotionAdapter.INSTANCE.toCoreModel(processed);
    }
}
