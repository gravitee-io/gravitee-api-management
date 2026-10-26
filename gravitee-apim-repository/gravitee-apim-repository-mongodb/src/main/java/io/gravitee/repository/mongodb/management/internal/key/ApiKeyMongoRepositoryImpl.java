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
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.addEachToSet;
import static com.mongodb.client.model.Updates.addToSet;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.max;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyMongoRepositoryImpl implements ApiKeyMongoRepositoryCustom {

    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String UPDATED_AT = "updatedAt";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    public List<ApiKeyMongo> search(ApiKeyCriteria filter, final Sortable sortable) {
        List<Bson> pipeline = buildFilterPipeline(filter);

        if (sortable != null) {
            if (sortable.order().equals(Order.ASC)) {
                pipeline.add(sort(Sorts.ascending(FieldUtils.toCamelCase(sortable.field()))));
            } else {
                pipeline.add(sort(Sorts.descending(FieldUtils.toCamelCase(sortable.field()))));
            }
        } else {
            pipeline.add(sort(Sorts.descending("updatedAt")));
        }

        return runAggregate(pipeline);
    }

    @Override
    public List<ApiKeyMongo> searchUnordered(ApiKeyCriteria filter) {
        return runAggregate(buildFilterPipeline(filter));
    }

    private List<Bson> buildFilterPipeline(ApiKeyCriteria filter) {
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

        return pipeline;
    }

    private List<ApiKeyMongo> runAggregate(List<Bson> pipeline) {
        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public List<ApiKeyMongo> searchAfter(ApiKeyCriteria criteria, ApiKeyCursor after, int pageSize, boolean sortByUpdatedAt) {
        Criteria mongoCriteria = new Criteria();
        List<Criteria> clauses = new ArrayList<>();

        if (!CollectionUtils.isEmpty(criteria.getEnvironments())) {
            clauses.add(Criteria.where("environmentId").in(criteria.getEnvironments()));
        }
        if (!CollectionUtils.isEmpty(criteria.getSubscriptions())) {
            clauses.add(Criteria.where("subscriptions").in(criteria.getSubscriptions()));
        }
        if (!criteria.isIncludeRevoked()) {
            clauses.add(Criteria.where("revoked").is(false));
        }
        if (!criteria.isIncludeFederated()) {
            clauses.add(new Criteria().orOperator(Criteria.where("federated").is(false), Criteria.where("federated").is(null)));
        }
        if (criteria.getFrom() > 0) {
            clauses.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())));
        }
        if (criteria.getTo() > 0) {
            clauses.add(Criteria.where("updatedAt").lte(new Date(criteria.getTo())));
        }
        if (criteria.getExpireAfter() > 0) {
            Criteria expireAfter = Criteria.where("expireAt").gte(new Date(criteria.getExpireAfter()));
            if (criteria.isIncludeWithoutExpiration()) {
                clauses.add(new Criteria().orOperator(Criteria.where("expireAt").is(null), expireAfter));
            } else {
                clauses.add(expireAfter);
            }
        }
        if (criteria.getExpireBefore() > 0) {
            Criteria expireBefore = Criteria.where("expireAt").lte(new Date(criteria.getExpireBefore()));
            if (criteria.isIncludeWithoutExpiration()) {
                clauses.add(new Criteria().orOperator(Criteria.where("expireAt").is(null), expireBefore));
            } else {
                clauses.add(expireBefore);
            }
        }
        if (after != null) {
            if (sortByUpdatedAt) {
                Date marker = new Date(after.updatedAt());
                clauses.add(
                    new Criteria().orOperator(
                        Criteria.where("updatedAt").gt(marker),
                        new Criteria().andOperator(Criteria.where("updatedAt").is(marker), Criteria.where("_id").gt(after.id()))
                    )
                );
            } else {
                clauses.add(Criteria.where("_id").gt(after.id()));
            }
        }

        if (!clauses.isEmpty()) {
            mongoCriteria = mongoCriteria.andOperator(clauses.toArray(new Criteria[0]));
        }

        Sort sort = sortByUpdatedAt ? Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("_id")) : Sort.by(Sort.Order.asc("_id"));
        Query query = new Query(mongoCriteria).with(sort).limit(pageSize);

        return mongoTemplate.find(query, ApiKeyMongo.class);
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
    public List<ApiKeyMongo> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        List<Bson> pipeline = List.of(
            match(eq("key", key)),
            lookup(tablePrefix + "subscriptions", "subscriptions", "_id", "sub"),
            unwind("$sub"),
            match(and(eq("sub.referenceId", referenceId), eq("sub.referenceType", referenceType)))
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
        return keys().updateOne(eq("_id", id), combine(addToSet(SUBSCRIPTIONS, subscriptionId), set("updatedAt", new Date())));
    }

    @Override
    public UpdateResult addSubscriptions(String id, Collection<String> subscriptionIds) {
        return keys().updateOne(
            eq("_id", id),
            combine(addEachToSet(SUBSCRIPTIONS, List.copyOf(subscriptionIds)), set("updatedAt", new Date()))
        );
    }

    /**
     * Writes every mapped field of the key, as a full save would, except {@code subscriptions}: links are only changed by
     * {@link #addSubscription(String, String)}, so an update made from an outdated copy of the key cannot drop them.
     * Fields that are {@code null} are removed, as a full save would do. {@code updatedAt} never moves backwards: gateways
     * synchronise keys by {@code updatedAt}, so an outdated copy must not undo the move made by a concurrent
     * {@link #addSubscription(String, String)}. It keeps its stored value when that is more recent, or when the given key has none.
     */
    @Override
    public UpdateResult updateAllButSubscriptions(ApiKeyMongo apiKey) {
        Document mapped = new Document();
        mongoTemplate.getConverter().write(apiKey, mapped);

        List<Bson> updates = new ArrayList<>();
        for (MongoPersistentProperty property : mongoTemplate
            .getConverter()
            .getMappingContext()
            .getRequiredPersistentEntity(ApiKeyMongo.class)) {
            String field = property.getFieldName();
            if (property.isIdProperty() || SUBSCRIPTIONS.equals(field)) {
                continue;
            }
            Object value = mapped.get(field);
            if (UPDATED_AT.equals(field)) {
                if (value != null) {
                    updates.add(max(field, value));
                }
                continue;
            }
            updates.add(value == null ? unset(field) : set(field, value));
        }
        return keys().updateOne(eq("_id", apiKey.getId()), combine(updates));
    }

    private MongoCollection<Document> keys() {
        return mongoTemplate.getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class));
    }

    private List<ApiKeyMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<ApiKeyMongo> apiKeys = new ArrayList<>();
        for (Document doc : aggregate) {
            apiKeys.add(mongoTemplate.getConverter().read(ApiKeyMongo.class, doc));
        }
        return apiKeys;
    }
}
