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
package io.gravitee.apim.infra.crud_service.promotion;

import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.infra.adapter.PromotionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class PromotionCrudServiceImpl implements PromotionCrudService {

    private final PromotionRepository promotionRepository;

    public PromotionCrudServiceImpl(@Lazy PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public Promotion create(Promotion promotion) {
        try {
            var created = promotionRepository.create(PromotionAdapter.INSTANCE.toRepository(promotion));
            return PromotionAdapter.INSTANCE.toCoreModel(created);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToCreateWithId(Promotion.class, promotion.getId(), e);
        }
    }

    @Override
    public Promotion update(Promotion promotion) {
        try {
            var updated = promotionRepository.update(PromotionAdapter.INSTANCE.toRepository(promotion));
            return PromotionAdapter.INSTANCE.toCoreModel(updated);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToUpdateWithId(Promotion.class, promotion.getId(), e);
        }
    }

    @Override
    public Promotion getById(String id) {
        try {
            log.debug("Get promotion by id : {}", id);
            return promotionRepository
                .findById(id)
                .map(PromotionAdapter.INSTANCE::toCoreModel)
                .orElseThrow(() -> new DbEntityNotFoundException(io.gravitee.repository.management.model.Promotion.class, id));
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToFindById(Promotion.class, id, e);
        }
    }
}
