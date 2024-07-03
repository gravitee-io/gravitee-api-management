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
package io.gravitee.repository.mongodb.management.internal.key;

import static com.mongodb.client.model.Aggregates.lookup;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.unset;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyMongoRepositoryImpl implements ApiKeyMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    public List<ApiKeyMongo> search(ApiKeyCriteria filter, final Sortable sortable) {
        List<Bson> pipeline = new ArrayList<>();

        if (!filter.isIncludeRevoked()) {
            pipeline.add(match(eq("revoked", false)));
        }
        if (!filter.isIncludeFederated()) {
            pipeline.add(match(or(eq("federated", false), eq("federated", null))));
        }

        // set range query
        if (filter.getFrom() > 0 && filter.getTo() > 0) {
            pipeline.add(match(and(gte("updatedAt", new Date(filter.getFrom())), lte("updatedAt", new Date(filter.getTo())))));
        } else if (filter.getFrom() > 0) {
            pipeline.add(match(gte("updatedAt", new Date(filter.getFrom()))));
        } else if (filter.getTo() > 0) {
            pipeline.add(match(lte("updatedAt", new Date(filter.getTo()))));
        }

        if (filter.getExpireAfter() > 0) {
            Bson expireAfterAt = gte("expireAt", new Date(filter.getExpireAfter()));
            if (filter.isIncludeWithoutExpiration()) {
                pipeline.add(match(or(eq("expireAt", null), expireAfterAt)));
            } else {
                pipeline.add(match(expireAfterAt));
            }
        }
        if (filter.getExpireBefore() > 0) {
            Bson expireBeforeAt = lte("expireAt", new Date(filter.getExpireBefore()));
            if (filter.isIncludeWithoutExpiration()) {
                pipeline.add(match(or(eq("expireAt", null), expireBeforeAt)));
            } else {
                pipeline.add(match(expireBeforeAt));
            }
        }

        if (!CollectionUtils.isEmpty(filter.getSubscriptions())) {
            pipeline.add(match(in("subscriptions", filter.getSubscriptions())));
        }

        if (!CollectionUtils.isEmpty(filter.getEnvironments())) {
            pipeline.add(match(in("environmentId", filter.getEnvironments())));
        }

        if (sortable != null) {
            if (sortable.order().equals(Order.ASC)) {
                pipeline.add(sort(Sorts.ascending(FieldUtils.toCamelCase(sortable.field()))));
            } else {
                pipeline.add(sort(Sorts.descending(FieldUtils.toCamelCase(sortable.field()))));
            }
        } else {
            pipeline.add(sort(Sorts.descending("updatedAt")));
        }

        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public List<ApiKeyMongo> findByKeyAndApi(String key, String api) {
        List<Bson> pipeline = List.of(
            match(eq("key", key)),
            lookup(tablePrefix + "subscriptions", "subscriptions", "_id", "sub"),
            unwind("$sub"),
            match(eq("sub.api", api))
        );

        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public List<ApiKeyMongo> findByPlan(String plan) {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(lookup(tablePrefix + "subscriptions", "subscriptions", "_id", "sub"));
        pipeline.add(unwind("$sub"));
        pipeline.add(match(eq("sub.plan", plan)));

        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public UpdateResult addSubscription(String id, String subscriptionId) {
        return mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class))
            .updateOne(eq("_id", id), push("subscriptions", subscriptionId));
    }

    private List<ApiKeyMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<ApiKeyMongo> apiKeys = new ArrayList<>();
        for (Document doc : aggregate) {
            apiKeys.add(mongoTemplate.getConverter().read(ApiKeyMongo.class, doc));
        }
        return apiKeys;
    }
}
