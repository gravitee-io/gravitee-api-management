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
package io.gravitee.repository.mongodb.management.internal.promotion;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PromotionCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.PromotionMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class PromotionMongoRepositoryImpl implements PromotionMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public PromotionMongoRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<PromotionMongo> search(PromotionCriteria criteria, Sortable sortable, Pageable pageable) {
        Query query = new Query();

        if (criteria != null) {
            if (!isEmpty(criteria.getTargetEnvCockpitIds())) {
                query.addCriteria(where("targetEnvCockpitId").in(criteria.getTargetEnvCockpitIds()));
            }

            if (criteria.getStatuses() != null) {
                List<String> statusNames = criteria.getStatuses().stream().map(Enum::name).collect(Collectors.toList());
                query.addCriteria(where("status").in(statusNames));
            }

            if (criteria.getTargetApiExists() != null) {
                query.addCriteria(where("targetApiId").exists(criteria.getTargetApiExists()));
            }

            if (!StringUtils.isEmpty(criteria.getApiId())) {
                query.addCriteria(where("apiId").is(criteria.getApiId()));
            }
        }

        if (sortable != null && isNotEmpty(sortable.field())) {
            query.with(
                Sort.by(
                    sortable.order().equals(Order.ASC) ? Sort.Direction.ASC : Sort.Direction.DESC,
                    FieldUtils.toCamelCase(sortable.field())
                )
            );
        }

        // Keep the count before adding pagination param to ensure the result is correct
        long total = mongoTemplate.count(query, PromotionMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<PromotionMongo> promotions = mongoTemplate.find(query, PromotionMongo.class);

        return new Page<>(promotions, pageable != null ? pageable.pageNumber() : 0, promotions.size(), total);
    }
}
