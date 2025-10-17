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
package io.gravitee.apim.infra.query_service.promotion;

import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.apim.infra.adapter.PromotionAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.api.search.PromotionCriteria;
import io.gravitee.repository.management.model.PromotionStatus;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PromotionQueryServiceImpl implements PromotionQueryService {

    private final PromotionRepository promotionRepository;

    public PromotionQueryServiceImpl(@Lazy PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public Page<Promotion> search(PromotionQuery promotionQuery) throws TechnicalManagementException {
        final PromotionCriteria.Builder criteria = new PromotionCriteria.Builder();
        if (promotionQuery.apiId() != null && !promotionQuery.apiId().isEmpty()) {
            criteria.apiId(promotionQuery.apiId());
        }

        if (!CollectionUtils.isEmpty(promotionQuery.statuses())) {
            criteria.statuses(
                promotionQuery
                    .statuses()
                    .stream()
                    .map(status -> PromotionStatus.valueOf(status.name()))
                    .collect(Collectors.toList())
            );
        }

        if (!CollectionUtils.isEmpty(promotionQuery.targetEnvCockpitIds())) {
            criteria.targetEnvCockpitIds(promotionQuery.targetEnvCockpitIds().toArray(new String[0]));
        }

        if (promotionQuery.targetApiExists() != null) {
            criteria.targetApiExists(promotionQuery.targetApiExists());
        }

        try {
            return promotionRepository.search(criteria.build(), null, null).map(PromotionAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }
}
