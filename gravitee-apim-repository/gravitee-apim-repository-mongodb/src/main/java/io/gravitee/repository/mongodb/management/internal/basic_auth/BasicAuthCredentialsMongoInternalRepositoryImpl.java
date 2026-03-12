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
package io.gravitee.repository.mongodb.management.internal.basic_auth;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.management.api.search.BasicAuthCredentialsCriteria;
import io.gravitee.repository.mongodb.management.internal.model.BasicAuthCredentialsMongo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;

/**
 * @author GraviteeSource Team
 */
public class BasicAuthCredentialsMongoInternalRepositoryImpl implements BasicAuthCredentialsMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<BasicAuthCredentialsMongo> search(BasicAuthCredentialsCriteria criteria) {
        List<Bson> pipeline = new ArrayList<>();

        if (!criteria.isIncludeRevoked()) {
            pipeline.add(match(eq("revoked", false)));
        }

        if (!CollectionUtils.isEmpty(criteria.getEnvironments())) {
            pipeline.add(match(in("environmentId", criteria.getEnvironments())));
        }

        if (!CollectionUtils.isEmpty(criteria.getSubscriptions())) {
            pipeline.add(match(in("subscriptions", criteria.getSubscriptions())));
        }

        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            pipeline.add(match(and(gte("updatedAt", new Date(criteria.getFrom())), lte("updatedAt", new Date(criteria.getTo())))));
        } else if (criteria.getFrom() > 0) {
            pipeline.add(match(gte("updatedAt", new Date(criteria.getFrom()))));
        } else if (criteria.getTo() > 0) {
            pipeline.add(match(lte("updatedAt", new Date(criteria.getTo()))));
        }

        pipeline.add(sort(Sorts.descending("updatedAt")));

        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(BasicAuthCredentialsMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public UpdateResult addSubscription(String id, String subscriptionId) {
        return mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(BasicAuthCredentialsMongo.class))
            .updateOne(eq("_id", id), combine(push("subscriptions", subscriptionId), set("updatedAt", new Date())));
    }

    private List<BasicAuthCredentialsMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<BasicAuthCredentialsMongo> result = new ArrayList<>();
        for (Document doc : aggregate) {
            result.add(mongoTemplate.getConverter().read(BasicAuthCredentialsMongo.class, doc));
        }
        return result;
    }
}
