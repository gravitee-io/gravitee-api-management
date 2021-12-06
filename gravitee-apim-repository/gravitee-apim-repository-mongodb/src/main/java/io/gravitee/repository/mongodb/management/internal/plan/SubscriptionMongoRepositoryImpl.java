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

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Sorts.descending;
import static java.util.stream.Collectors.toList;

import com.mongodb.client.AggregateIterable;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import java.util.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionMongoRepositoryImpl implements SubscriptionMongoRepositoryCustom {

    private static final String NUMBER_OF_SUBSCRIPTIONS = "numberOfSubscriptions";
    private static final String LAST_UPDATED_AT = "updatedAt";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<SubscriptionMongo> search(SubscriptionCriteria criteria, Pageable pageable) {
        Query query = new Query();

        if (criteria.getClientId() != null) {
            query.addCriteria(Criteria.where("clientId").is(criteria.getClientId()));
        }

        if (criteria.getApis() != null && !criteria.getApis().isEmpty()) {
            if (criteria.getApis().size() == 1) {
                query.addCriteria(Criteria.where("api").is(criteria.getApis().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("api").in(criteria.getApis()));
            }
        }

        if (criteria.getPlans() != null && !criteria.getPlans().isEmpty()) {
            if (criteria.getPlans().size() == 1) {
                query.addCriteria(Criteria.where("plan").is(criteria.getPlans().iterator().next()));
            } else {
                query.addCriteria(Criteria.where("plan").in(criteria.getPlans()));
            }
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            Criteria statusCriteria = Criteria.where("status");
            if (criteria.getStatuses().size() == 1) {
                query.addCriteria(statusCriteria.is(criteria.getStatuses().iterator().next()));
            } else {
                query.addCriteria(statusCriteria.in(criteria.getStatuses()));
            }
        }

        if (criteria.getApplications() != null && !criteria.getApplications().isEmpty()) {
            Criteria applicationCriteria = Criteria.where("application");
            if (criteria.getApplications().size() == 1) {
                query.addCriteria(applicationCriteria.is(criteria.getApplications().iterator().next()));
            } else {
                query.addCriteria(applicationCriteria.in(criteria.getApplications()));
            }
        }

        if (criteria.getFrom() > 0 || criteria.getTo() > 0) {
            // Need to mutualize the instantiation of this criteria otherwise mongo drive is throwing an error, when
            // using multiple `Criteria.where("updatedAt").xxx` with the same query
            Criteria updatedAtCriteria = Criteria.where("updatedAt");

            if (criteria.getFrom() > 0) {
                updatedAtCriteria = updatedAtCriteria.gte(new Date(criteria.getFrom()));
            }
            if (criteria.getTo() > 0) {
                updatedAtCriteria = updatedAtCriteria.lte(new Date(criteria.getTo()));
            }

            query.addCriteria(updatedAtCriteria);
        }

        if (criteria.getEndingAtAfter() > 0 || criteria.getEndingAtBefore() > 0) {
            // Need to mutualize the instantiation of this criteria otherwise mongo drive is throwing an error, when
            // using multiple `Criteria.where("endingAt").xxx` with the same query
            Criteria endingAtCriteria = Criteria.where("endingAt");

            if (criteria.getEndingAtAfter() > 0) {
                endingAtCriteria = endingAtCriteria.gte(new Date(criteria.getEndingAtAfter()));
            }
            if (criteria.getEndingAtBefore() > 0) {
                endingAtCriteria = endingAtCriteria.lte(new Date(criteria.getEndingAtBefore()));
            }

            query.addCriteria(endingAtCriteria);
        }

        // set sort by created at
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        Long total = null;

        // set pageable
        if (pageable != null) {
            total = mongoTemplate.count(query, SubscriptionMongo.class);
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<SubscriptionMongo> subscriptions = mongoTemplate.find(query, SubscriptionMongo.class);

        return new Page<>(
            subscriptions,
            (pageable != null) ? pageable.pageNumber() : 0,
            subscriptions.size(),
            total == null ? subscriptions.size() : total
        );
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria) {
        List<Bson> aggregations = new ArrayList<>();
        String group = "$api";
        if (criteria.getApplications() != null && !criteria.getApplications().isEmpty()) {
            aggregations.add(match(in("application", criteria.getApplications())));
            group = "$application";
        } else if (criteria.getApis() != null && !criteria.getApis().isEmpty()) {
            aggregations.add(match(in("api", criteria.getApis())));
        } else {
            aggregations.add(match(ne("api", null)));
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            aggregations.add(match(in("status", criteria.getStatuses().stream().map(Enum::name).collect(toList()))));
        }

        aggregations.add(group(group, sum(NUMBER_OF_SUBSCRIPTIONS, 1)));
        aggregations.add(sort(descending(NUMBER_OF_SUBSCRIPTIONS, LAST_UPDATED_AT)));

        AggregateIterable<Document> subscriptions = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class))
            .aggregate(aggregations);

        Set<String> references = new LinkedHashSet<>();
        subscriptions.forEach(
            document -> {
                String referenceId = document.getString("_id");
                references.add(referenceId);
            }
        );
        return references;
    }
}
