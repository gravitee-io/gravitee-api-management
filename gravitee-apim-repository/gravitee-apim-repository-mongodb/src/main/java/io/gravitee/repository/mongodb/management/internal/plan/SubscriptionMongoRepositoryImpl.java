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
package io.gravitee.repository.mongodb.management.internal.plan;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionMongoRepositoryImpl implements SubscriptionMongoRepositoryCustom {

    private static final String NUMBER_OF_SUBSCRIPTIONS = "numberOfSubscriptions";
    private static final String LAST_UPDATED_AT = "updatedAt";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    public Page<SubscriptionMongo> search(SubscriptionCriteria criteria, final Sortable sortable, final Pageable pageable) {
        List<Bson> dataPipeline = new ArrayList<>();

        if (criteria.getClientId() != null) {
            dataPipeline.add(match(eq("clientId", criteria.getClientId())));
        }

        if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
            if (criteria.getIds().size() == 1) {
                dataPipeline.add(match(eq("_id", criteria.getIds().iterator().next())));
            } else {
                dataPipeline.add(match(in("_id", criteria.getIds())));
            }
        }

        if (criteria.getReferenceType() != null && criteria.getReferenceIds() != null && !criteria.getReferenceIds().isEmpty()) {
            dataPipeline.add(match(eq("referenceType", criteria.getReferenceType().name())));
            if (criteria.getReferenceIds().size() == 1) {
                dataPipeline.add(match(eq("referenceId", criteria.getReferenceIds().iterator().next())));
            } else {
                dataPipeline.add(match(in("referenceId", criteria.getReferenceIds())));
            }
        }

        if (!CollectionUtils.isEmpty(criteria.getEnvironments())) {
            dataPipeline.add(match(in("environmentId", criteria.getEnvironments())));
        }

        if (criteria.getPlans() != null && !criteria.getPlans().isEmpty()) {
            if (criteria.getPlans().size() == 1) {
                dataPipeline.add(match(eq("plan", criteria.getPlans().iterator().next())));
            } else {
                dataPipeline.add(match(in("plan", criteria.getPlans())));
            }
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            if (criteria.getStatuses().size() == 1) {
                dataPipeline.add(match(eq("status", criteria.getStatuses().iterator().next())));
            } else {
                dataPipeline.add(match(in("status", criteria.getStatuses())));
            }
        }

        if (criteria.getApplications() != null && !criteria.getApplications().isEmpty()) {
            if (criteria.getApplications().size() == 1) {
                dataPipeline.add(match(eq("application", criteria.getApplications().iterator().next())));
            } else {
                dataPipeline.add(match(in("application", criteria.getApplications())));
            }
        }

        // set range query
        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            dataPipeline.add(match(and(gte("updatedAt", new Date(criteria.getFrom())), lt("updatedAt", new Date(criteria.getTo())))));
        } else if (criteria.getFrom() > 0) {
            dataPipeline.add(match(gte("updatedAt", new Date(criteria.getFrom()))));
        } else if (criteria.getTo() > 0) {
            dataPipeline.add(match(lt("updatedAt", new Date(criteria.getTo()))));
        }

        if (criteria.getEndingAtAfter() > 0) {
            Bson endingAfterAt = gte("endingAt", new Date(criteria.getEndingAtAfter()));
            if (criteria.isIncludeWithoutEnd()) {
                dataPipeline.add(match(or(eq("endingAt", null), endingAfterAt)));
            } else {
                dataPipeline.add(match(endingAfterAt));
            }
        }
        if (criteria.getEndingAtBefore() > 0) {
            Bson endingBeforeAt = lt("endingAt", new Date(criteria.getEndingAtBefore()));
            if (criteria.isIncludeWithoutEnd()) {
                dataPipeline.add(match(or(eq("endingAt", null), endingBeforeAt)));
            } else {
                dataPipeline.add(match(endingBeforeAt));
            }
        }

        if (!isEmpty(criteria.getPlanSecurityTypes())) {
            dataPipeline.add(lookup(tablePrefix + "plans", "plan", "_id", "subscribedPlan"));
            dataPipeline.add(unwind("$subscribedPlan"));
            dataPipeline.add(match(in("subscribedPlan.security", criteria.getPlanSecurityTypes())));
        }

        if (!isEmpty(criteria.getExcludedApis())) {
            dataPipeline.add(match(nin("referenceId", criteria.getExcludedApis())));
        }

        // set sortable
        if (sortable != null) {
            if (sortable.order().equals(Order.ASC)) {
                dataPipeline.add(sort(Sorts.ascending(FieldUtils.toCamelCase(sortable.field()))));
            } else {
                dataPipeline.add(sort(Sorts.descending(FieldUtils.toCamelCase(sortable.field()))));
            }
        } else {
            dataPipeline.add(sort(Sorts.descending("createdAt")));
        }

        dataPipeline.add(Aggregates.project(Projections.exclude("_class")));

        Integer totalCount = null;
        // if pageable, count total subscriptions matching criterias
        if (pageable != null) {
            List<Bson> countPipeline = new ArrayList<>(dataPipeline);
            countPipeline.add(count("totalCount"));
            AggregateIterable<Document> countAggregate = mongoTemplate
                .getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class))
                .aggregate(countPipeline);
            if (countAggregate.first() != null) {
                totalCount = countAggregate.first().getInteger("totalCount", 0);
            }
            dataPipeline.add(skip(pageable.pageNumber() * pageable.pageSize()));
            dataPipeline.add(limit(pageable.pageSize()));
        }

        AggregateIterable<Document> dataAggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class))
            .aggregate(dataPipeline);
        return buildSubscriptionsPage(pageable, dataAggregate, totalCount);
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) {
        List<Bson> aggregations = new ArrayList<>();
        String group = "$api";
        if (criteria.getApplications() != null && !criteria.getApplications().isEmpty()) {
            aggregations.add(match(in("application", criteria.getApplications())));
            group = "$application";
        } else if (
            criteria.getReferenceType() != null &&
            criteria.getReferenceType() == SubscriptionReferenceType.API &&
            criteria.getReferenceIds() != null &&
            !criteria.getReferenceIds().isEmpty()
        ) {
            aggregations.add(match(in("referenceId", criteria.getReferenceIds())));
            aggregations.add(match(eq("referenceType", SubscriptionReferenceType.API.name())));
        } else {
            aggregations.add(match(ne("api", null)));
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            aggregations.add(match(in("status", criteria.getStatuses())));
        }

        aggregations.add(group(group, sum(NUMBER_OF_SUBSCRIPTIONS, 1)));

        if (Order.DESC.equals(order)) {
            aggregations.add(sort(descending(NUMBER_OF_SUBSCRIPTIONS, LAST_UPDATED_AT)));
        } else {
            aggregations.add(sort(ascending(NUMBER_OF_SUBSCRIPTIONS, LAST_UPDATED_AT)));
        }

        AggregateIterable<Document> subscriptions = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class))
            .aggregate(aggregations);

        Set<String> references = new LinkedHashSet<>();
        subscriptions.forEach(document -> {
            String referenceId = document.getString("_id");
            references.add(referenceId);
        });
        return references;
    }

    private Page<SubscriptionMongo> buildSubscriptionsPage(
        Pageable pageable,
        AggregateIterable<Document> dataAggregate,
        Integer totalCount
    ) {
        List<SubscriptionMongo> subscriptions = new ArrayList<>();

        MongoConverter converter = mongoTemplate.getConverter();
        for (Document doc : dataAggregate) {
            subscriptions.add(converter.read(SubscriptionMongo.class, doc));
        }

        return new Page<>(
            subscriptions,
            pageable != null ? pageable.pageNumber() : 0,
            subscriptions.size(),
            totalCount == null ? subscriptions.size() : totalCount
        );
    }
}
