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
package io.gravitee.repository.mongodb.management.internal.key;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyMongoRepositoryImpl implements ApiKeyMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApiKeyMongo> search(ApiKeyCriteria filter) {
        Query query = new Query();

        if (!filter.isIncludeRevoked()) {
            query.addCriteria(Criteria.where("revoked").is(false));
        }

        if (filter.getPlans() != null) {
            query.addCriteria(Criteria.where("plan").in(filter.getPlans()));
        }

        // set range query
        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            query.addCriteria(Criteria.where("updatedAt").gte(new Date(filter.getFrom())).lt(new Date(filter.getTo())));
        }

        if (filter.getExpireAfter() > 0 || filter.getExpireBefore() > 0) {
            // Need to mutualize the instantiation of this filter otherwise mongo driver is throwing an error, when
            // using multiple `Criteria.where("expireAt").xxx` with the same query
            Criteria expireAtCriteria = Criteria.where("expireAt");

            if (filter.getExpireAfter() > 0) {
                expireAtCriteria = expireAtCriteria.gte(new Date(filter.getExpireAfter()));
            }
            if (filter.getExpireBefore() > 0) {
                expireAtCriteria = expireAtCriteria.lte(new Date(filter.getExpireBefore()));
            }

            query.addCriteria(expireAtCriteria);
        }

        query.with(new Sort(Sort.Direction.DESC, "updatedAt"));

        List<ApiKeyMongo> events = mongoTemplate.find(query, ApiKeyMongo.class);
        long total = mongoTemplate.count(query, ApiKeyMongo.class);

        return new Page<>(events, 0, 0, total);
    }
}
