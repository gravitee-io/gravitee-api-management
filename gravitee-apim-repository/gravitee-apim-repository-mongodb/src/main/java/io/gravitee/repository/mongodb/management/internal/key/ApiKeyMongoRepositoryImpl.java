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

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Updates.push;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
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
        // if filter contains no plans, do the search from keys collection as there is no need to join subscriptions collection
        // if filter contains a period from/to, do the search from keys collection as subscriptions lookup will be optimized
        if (CollectionUtils.isEmpty(filter.getPlans()) || filter.getFrom() != 0 || filter.getTo() != 0) {
            return getListFromAggregate(searchFromKeysJoiningSubscription(filter));
        }
        // elsewhere, do the search from the subscription collection, joining the keys collection
        return getListFromSubscriptionAggregate(searchFromSubscriptionJoiningKeys(filter));
    }

    private AggregateIterable<Document> searchFromSubscriptionJoiningKeys(ApiKeyCriteria filter) {
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

        return mongoTemplate.getCollection(mongoTemplate.getCollectionName(SubscriptionMongo.class)).aggregate(pipeline);
    }

    private AggregateIterable<Document> searchFromKeysJoiningSubscription(ApiKeyCriteria filter) {
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

        if (!CollectionUtils.isEmpty(filter.getPlans())) {
            pipeline.add(lookup(tablePrefix + "subscriptions", "subscriptions", "_id", "sub"));
            pipeline.add(unwind("$sub"));
            pipeline.add(match(in("sub.plan", filter.getPlans())));
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

    private List<ApiKeyMongo> getListFromSubscriptionAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<ApiKeyMongo> apiKeys = new ArrayList<>();
        for (Document doc : aggregate) {
            apiKeys.add(mongoTemplate.getConverter().read(ApiKeyMongo.class, doc.get("keys", Document.class)));
        }
        return apiKeys;
    }
}
