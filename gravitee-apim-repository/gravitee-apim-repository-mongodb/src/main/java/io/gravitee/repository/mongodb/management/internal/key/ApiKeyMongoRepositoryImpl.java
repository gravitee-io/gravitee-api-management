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

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.fields;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import java.util.*;
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
    public List<ApiKeyMongo> search(ApiKeyCriteria filter) {
        if (CollectionUtils.isEmpty(filter.getPlans())) {
            AggregateIterable<Document> aggregate = searchWithoutPlanFilter(filter);
            return getListFromAggregate(aggregate);
        }

        AggregateIterable<Document> aggregate = searchWithPlanFilter(filter);
        return getListFromSubscriptionAggregate(aggregate);
    }

    private AggregateIterable<Document> searchWithPlanFilter(ApiKeyCriteria filter) {
        List<Bson> pipeline = new ArrayList<>();

        pipeline.add(match(in("plan", filter.getPlans())));

        pipeline.add(lookup(tablePrefix + "keys", "_id", "subscriptions", "keys"));
        pipeline.add(unwind("$keys"));

        pipeline.add(project(fields(exclude("keys._class", "_class"))));

        if (!filter.isIncludeRevoked()) {
            pipeline.add(match(eq("keys.revoked", false)));
        }

        // set range query
        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            pipeline.add(match(and(gte("keys.updatedAt", new Date(filter.getFrom())), lte("keys.updatedAt", new Date(filter.getTo())))));
        }

        if (filter.getExpireAfter() > 0 && filter.getExpireBefore() > 0) {
            pipeline.add(
                match(
                    and(gte("keys.expireAt", new Date(filter.getExpireAfter())), lte("keys.expireAt", new Date(filter.getExpireBefore())))
                )
            );
        } else if (filter.getExpireAfter() > 0) {
            pipeline.add(match(gte("keys.expireAt", new Date(filter.getExpireAfter()))));
        } else if (filter.getExpireBefore() > 0) {
            pipeline.add(match(lte("keys.expireAt", new Date(filter.getExpireBefore()))));
        }

        pipeline.add(sort(Sorts.descending("keys.updatedAt")));

        return mongoTemplate.getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class)).aggregate(pipeline);
    }

    private AggregateIterable<Document> searchWithoutPlanFilter(ApiKeyCriteria filter) {
        List<Bson> pipeline = new ArrayList<>();

        if (!filter.isIncludeRevoked()) {
            pipeline.add(match(eq("revoked", false)));
        }

        // set range query
        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            pipeline.add(match(and(gte("updatedAt", new Date(filter.getFrom())), lte("updatedAt", new Date(filter.getTo())))));
        }

        if (filter.getExpireAfter() > 0 && filter.getExpireBefore() > 0) {
            pipeline.add(
                match(and(gte("expireAt", new Date(filter.getExpireAfter())), lte("expireAt", new Date(filter.getExpireBefore()))))
            );
        } else if (filter.getExpireAfter() > 0) {
            pipeline.add(match(gte("expireAt", new Date(filter.getExpireAfter()))));
        } else if (filter.getExpireBefore() > 0) {
            pipeline.add(match(lte("expireAt", new Date(filter.getExpireBefore()))));
        }

        pipeline.add(sort(Sorts.descending("updatedAt")));

        return mongoTemplate.getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class)).aggregate(pipeline);
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

    private List<ApiKeyMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<ApiKeyMongo> apiKeys = new ArrayList<>();
        for (Document doc : aggregate) {
            apiKeys.add(mongoTemplate.getConverter().read(ApiKeyMongo.class, doc));
        }
        return apiKeys;
    }

    private List<ApiKeyMongo> getListFromSubscriptionAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<ApiKeyMongo> apiKeys = new ArrayList<>();
        for (Document doc : aggregate) {
            apiKeys.add(mongoTemplate.getConverter().read(ApiKeyMongo.class, doc.get("keys", Document.class)));
        }
        return apiKeys;
    }
}
