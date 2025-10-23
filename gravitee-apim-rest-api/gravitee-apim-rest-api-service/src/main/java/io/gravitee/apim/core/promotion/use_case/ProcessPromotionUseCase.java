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
package io.gravitee.apim.core.promotion.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

@UseCase
public class ProcessPromotionUseCase {

    private final PromotionCrudService promotionCrudService;
    private final ApiCrudService apiCrudService;
    private final CockpitPromotionServiceProvider cockpitPromotionServiceProvider;

    public ProcessPromotionUseCase(
        PromotionCrudService promotionCrudService,
        ApiCrudService apiCrudService,
        CockpitPromotionServiceProvider cockpitPromotionServiceProvider
    ) {
        this.promotionCrudService = promotionCrudService;
        this.apiCrudService = apiCrudService;
        this.cockpitPromotionServiceProvider = cockpitPromotionServiceProvider;
    }

    public record Input(String promotionId, boolean isAccepted) {}

    public record Output(Promotion promotion) {}

    public Output execute(Input input) {
        var promotion = promotionCrudService.getById(input.promotionId);
        var api = apiCrudService.get(promotion.getApiId());
        var processedPromotion = switch (api.getDefinitionVersion()) {
            case V2 -> cockpitPromotionServiceProvider.process(input.promotionId, input.isAccepted);
            default -> throw new TechnicalManagementException("Only V2 and V4 API definition are supported");
        };

        return new Output(processedPromotion);
    }
}
