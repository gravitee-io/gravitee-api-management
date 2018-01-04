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
package io.gravitee.repository.mongodb.management.internal.plan;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionMongoRepositoryImpl implements SubscriptionMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<SubscriptionMongo> search(SubscriptionCriteria criteria, Pageable pageable) {
        Query query = new Query();

        if (criteria.getClientId() != null) {
            query.addCriteria(Criteria.where("clientId").is(criteria.getClientId()));
        }

        if (criteria.getApis() != null && ! criteria.getApis().isEmpty()) {
            if (criteria.getApis().size() == 1) {
                query.addCriteria(Criteria.where("api").is(criteria.getApis().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("api").in(criteria.getApis()));
            }
        }

        if (criteria.getPlans() != null && ! criteria.getPlans().isEmpty()) {
            if (criteria.getPlans().size() == 1) {
                query.addCriteria(Criteria.where("plan").is(criteria.getPlans().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("plan").in(criteria.getPlans()));
            }
        }

        if (criteria.getStatuses() != null && ! criteria.getStatuses().isEmpty()) {
            if (criteria.getStatuses().size() == 1) {
                query.addCriteria(Criteria.where("status").is(criteria.getStatuses().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("status").in(criteria.getStatuses()));
            }
        }

        if (criteria.getApplications() != null && ! criteria.getApplications().isEmpty()) {
            if (criteria.getApplications().size() == 1) {
                query.addCriteria(Criteria.where("application").is(criteria.getApplications().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("application").in(criteria.getApplications()));
            }
        }

        if (criteria.getFrom() != 0 && criteria.getTo() != 0) {
            query.addCriteria(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        }

        // set sort by created at
        query.with(new Sort(Sort.Direction.DESC, "createdAt"));

        // set pageable
        if (pageable != null) {
            query.with(new PageRequest(pageable.pageNumber(), pageable.pageSize()));
        }

        List<SubscriptionMongo> subscriptions = mongoTemplate.find(query, SubscriptionMongo.class);
        long total = mongoTemplate.count(query, SubscriptionMongo.class);

        return new Page<>(
                subscriptions, (pageable != null) ? pageable.pageNumber() : 0,
                (pageable != null) ? pageable.pageSize() : 0, total);
    }
}
