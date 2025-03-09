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
package io.gravitee.repository.mongodb.management.internal.event;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.out;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.OutOperation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class EventMongoRepositoryImpl implements EventMongoRepositoryCustom {

    public static final String EVENTS_COLLECTION = "events";
    public static final String BACKUP_COLLECTION = "events_history";
    public static final String TMP_EVENTS_COLLECTION = "tmp_events";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<EventMongo> search(EventCriteria criteria, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = buildDBCriteria(criteria);
        criteriaList.forEach(query::addCriteria);

        // set sort by updated at
        query.with(Sort.by(Sort.Direction.DESC, "updatedAt", "_id"));

        long total = mongoTemplate.count(query, EventMongo.class);

        // set pageable
        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<EventMongo> events = mongoTemplate.find(query, EventMongo.class);

        return new Page<>(events, (pageable != null) ? pageable.pageNumber() : 0, events.size(), total);
    }

    @Override
    public Event patch(Event event) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(event.getId()));
        Update update = new Update();
        if (event.getEnvironments() != null) {
            update.set("environments", event.getEnvironments());
        }
        if (event.getOrganizations() != null) {
            update.set("organizations", event.getOrganizations());
        }
        if (event.getType() != null) {
            update.set("type", event.getType());
        }
        if (event.getPayload() != null) {
            update.set("payload", event.getPayload());
        }
        if (event.getParentId() != null) {
            update.set("parentId", event.getParentId());
        }
        if (event.getUpdatedAt() != null) {
            update.set("updatedAt", event.getUpdatedAt());
        }
        if (event.getProperties() != null) {
            event.getProperties().forEach((property, value) -> update.set("properties." + property, value));
        }
        var updateResult = mongoTemplate.updateFirst(query, update, EventMongo.class);
        return updateResult.getModifiedCount() == 1 ? event : null;
    }

    @Override
    public long deleteAllByApi(String apiId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("properties." + Event.EventProperties.API_ID.getValue()).is(apiId));
        return mongoTemplate.remove(query, EventMongo.class).getDeletedCount();
    }

    public static List<Criteria> buildDBCriteria(EventCriteria criteria) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (!isEmpty(criteria.getTypes())) {
            criteriaList.add(Criteria.where("type").in(criteria.getTypes()));
        }
        if (!isEmpty(criteria.getProperties())) {
            // set criteria query
            criteria
                .getProperties()
                .forEach((k, v) -> {
                    if (v instanceof Collection) {
                        criteriaList.add(Criteria.where("properties." + k).in((Collection<?>) v));
                    } else {
                        criteriaList.add(Criteria.where("properties." + k).is(v));
                    }
                });
        }

        // set range query
        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        } else if (criteria.getFrom() > 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())));
        } else if (criteria.getTo() > 0) {
            criteriaList.add(Criteria.where("updatedAt").lt(new Date(criteria.getTo())));
        }

        if (!isEmpty(criteria.getEnvironments()) && !isEmpty(criteria.getOrganizations())) {
            criteriaList.add(new Criteria().andOperator(buildEnvironmentsCriteria(criteria), buildOrganizationsCriteria(criteria)));
        } else if (!isEmpty(criteria.getEnvironments())) {
            criteriaList.add(buildEnvironmentsCriteria(criteria));
        } else if (!isEmpty(criteria.getOrganizations())) {
            criteriaList.add(buildOrganizationsCriteria(criteria));
        }

        return criteriaList;
    }

    private static Criteria buildOrganizationsCriteria(EventCriteria criteria) {
        return new Criteria()
            .orOperator(
                Criteria.where("organizations").exists(false),
                Criteria.where("organizations").isNull(),
                Criteria.where("organizations").is(Collections.emptyList()),
                Criteria.where("organizations").in(criteria.getOrganizations())
            );
    }

    private static Criteria buildEnvironmentsCriteria(EventCriteria criteria) {
        return new Criteria()
            .orOperator(
                Criteria.where("environments").exists(false),
                Criteria.where("environments").isNull(),
                Criteria.where("environments").is(Collections.emptyList()),
                Criteria.where("environments").in(criteria.getEnvironments())
            );
    }

    @Override
    public void cleanupGatewayEvents(String environmentId, int keepRecordsCount) {
        var indexOps = mongoTemplate.indexOps(EVENTS_COLLECTION);
        indexOps.ensureIndex(
            new Index()
                .on("environments", Sort.Direction.ASC)
                .on("properties.api_id", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.ASC)
                .named("e1pa1u1esn")
        );
        indexOps.ensureIndex(new Index().on("type", Sort.Direction.ASC).on("environments", Sort.Direction.ASC).named("tsen1e1"));
        backupEvents();
        createWorkingCopy();

        mongoTemplate.remove(new Query(), EVENTS_COLLECTION);

        keepNonApiEvents(environmentId);

        keepLatestApiEvents(environmentId, keepRecordsCount);

        rebuildIndexes();
        dropWorkingCopy();
    }

    private void backupEvents() {
        mongoTemplate.dropCollection(BACKUP_COLLECTION);
        log.info("Backing up events to events_history");
        var agg = newAggregation(List.of(out(BACKUP_COLLECTION)));
        mongoTemplate.aggregate(agg, EVENTS_COLLECTION, Object.class);
    }

    private void createWorkingCopy() {
        mongoTemplate.dropCollection(TMP_EVENTS_COLLECTION);
        log.info("Creating working copy of events");
        var agg = newAggregation(List.of(out(TMP_EVENTS_COLLECTION)));
        mongoTemplate.aggregate(agg, EVENTS_COLLECTION, Object.class);

        long count = mongoTemplate.getCollection(TMP_EVENTS_COLLECTION).countDocuments();
        log.info("{} events copied to tmp_events", count);
    }

    private void keepNonApiEvents(String envId) {
        List<String> nonApiEventTypes = List.of(
            "GATEWAY_STARTED",
            "DEBUG_API",
            "GATEWAY_STOPPED",
            "PUBLISH_DICTIONARY",
            "UNPUBLISH_DICTIONARY",
            "START_DICTIONARY",
            "STOP_DICTIONARY",
            "PUBLISH_ORGANIZATION"
        );

        Criteria criteria = new Criteria()
            .orOperator(Criteria.where("type").in(nonApiEventTypes), Criteria.where("environments").ne(envId));

        var agg = newAggregation(List.of(match(criteria), new OutOperation(TMP_EVENTS_COLLECTION)));
        mongoTemplate.aggregate(agg, EVENTS_COLLECTION, Object.class);
    }

    private void keepLatestApiEvents(String envId, int history) {
        log.info("Selecting latest {} events for each API entity for environment {}", history, envId);

        var aggregation = newAggregation(
            match(Criteria.where("environments").is(envId).and("properties.api_id").exists(true)),
            sort(Sort.by(Sort.Direction.DESC, "properties.api_id", "updatedAt")),
            group("properties.api_id").first("$$ROOT").as("first").push("$$ROOT").as("events"),
            project().and("events").slice(history).as("events")
        );

        var results = mongoTemplate.aggregate(aggregation, TMP_EVENTS_COLLECTION, Document.class);

        results
            .getMappedResults()
            .forEach(doc -> {
                @SuppressWarnings("unchecked")
                List<Document> events = (List<Document>) doc.get("events");
                mongoTemplate.insert(events, EVENTS_COLLECTION);
                log.info("{} events inserted for API: {}", events.size(), ((Document) events.getFirst().get("properties")).get("api_id"));
            });
    }

    private void rebuildIndexes() {
        var indexOps = mongoTemplate.indexOps(EVENTS_COLLECTION);
        log.info("Dropping existing indexes");
        indexOps.dropAllIndexes();

        indexOps.ensureIndex(new Index().on("type", Sort.Direction.ASC).named("t1"));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.ASC).named("u1"));
        indexOps.ensureIndex(new Index().on("updatedAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC).named("u1i1"));
        indexOps.ensureIndex(new Index().on("properties.api_id", Sort.Direction.ASC).named("pa1"));
        indexOps.ensureIndex(new Index().on("properties.api_id", Sort.Direction.ASC).on("type", Sort.Direction.ASC).named("pa1t1"));
        indexOps.ensureIndex(new Index().on("properties.api_id", Sort.Direction.ASC).on("updatedAt", Sort.Direction.ASC).named("pa1u1"));
        indexOps.ensureIndex(new Index().on("type", Sort.Direction.ASC).on("updatedAt", Sort.Direction.ASC).named("t1u1"));

        log.info("Indexes rebuilt successfully");
    }

    private void dropWorkingCopy() {
        log.info("Dropping temporary collection");
        mongoTemplate.dropCollection(TMP_EVENTS_COLLECTION);
    }
}
