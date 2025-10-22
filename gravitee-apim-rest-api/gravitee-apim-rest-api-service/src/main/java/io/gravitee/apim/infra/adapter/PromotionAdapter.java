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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PromotionAdapter {
    PromotionAdapter INSTANCE = Mappers.getMapper(PromotionAdapter.class);

    Promotion toCoreModel(io.gravitee.repository.management.model.Promotion promotion);

    PromotionAuthor toCoreModel(io.gravitee.repository.management.model.PromotionAuthor promotionAuthor);

    Promotion toCoreModel(PromotionEntity promotionEntity);

    PromotionAuthor toCoreModel(PromotionEntityAuthor promotionEntityAuthor);

    io.gravitee.repository.management.model.Promotion toRepository(Promotion promotion);

    io.gravitee.repository.management.model.PromotionAuthor toRepository(PromotionAuthor promotionAuthor);

    PromotionEntity toRestApiModel(Promotion promotion);

    PromotionEntityAuthor toRestApiModel(PromotionAuthor promotionAuthor);

    PromotionEntityStatus toRestApiModel(PromotionStatus promotionStatus);

    PromotionRequestEntity toRestApiModel(PromotionRequest promotionRequest);
}
