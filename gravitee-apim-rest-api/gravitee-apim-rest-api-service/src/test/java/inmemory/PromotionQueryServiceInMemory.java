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
package inmemory;

import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionQuery;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PromotionQueryServiceInMemory implements PromotionQueryService, InMemoryAlternative<Promotion> {

    final List<Promotion> storage = new ArrayList<>();

    @Override
    public void initWith(List<Promotion> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Promotion> storage() {
        return Collections.unmodifiableList(storage);
    }

    @Override
    public Page<Promotion> search(PromotionQuery promotionQuery) throws TechnicalManagementException {
        if (promotionQuery == null) {
            return new Page<>(storage, 0, storage.size(), storage.size());
        }

        List<Promotion> matches = storage
            .stream()
            .filter(promotion -> {
                boolean matchesApiId = promotionQuery.getApiId() == null || promotion.getApiId().equals(promotionQuery.getApiId());

                boolean matchesStatuses =
                    promotionQuery.getStatuses() == null || promotionQuery.getStatuses().contains(promotion.getStatus());

                boolean matchesTargetEnvCockpitIds =
                    promotionQuery.getTargetEnvCockpitIds() == null ||
                    promotionQuery.getTargetEnvCockpitIds().contains(promotion.getTargetEnvCockpitId());

                boolean matchesTargetApiExists =
                    promotionQuery.getTargetApiExists() == null ||
                    (promotionQuery.getTargetApiExists() == (promotion.getTargetApiId() != null));

                return matchesApiId && matchesStatuses && matchesTargetEnvCockpitIds && matchesTargetApiExists;
            })
            .toList();

        return new Page<>(matches, 0, matches.size(), matches.size());
    }
}
