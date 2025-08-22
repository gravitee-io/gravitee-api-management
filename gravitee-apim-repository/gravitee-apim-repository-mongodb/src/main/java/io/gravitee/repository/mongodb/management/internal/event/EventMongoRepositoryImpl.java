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

import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    public static final String BACKUP_COLLECTION = "events_history";
    public static final String UPDATED_AT_FIELD = "updatedAt";
    public static final String ENVIRONMENTS_FIELD = "environments";
    public static final String ORGANIZATIONS_FIELD = "organizations";

    private String backupCollection = BACKUP_COLLECTION;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String collectionPrefix;

    @PostConstruct
    void setup() {
        backupCollection = collectionPrefix + BACKUP_COLLECTION;
    }

    @Override
    public Page<EventMongo> search(EventCriteria criteria, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = buildDBCriteria(criteria);
        criteriaList.forEach(query::addCriteria);

        // set sort by updated at
        query.with(Sort.by(Sort.Direction.DESC, UPDATED_AT_FIELD, "_id"));

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
            update.set(ENVIRONMENTS_FIELD, event.getEnvironments());
        }
        if (event.getOrganizations() != null) {
            update.set(ORGANIZATIONS_FIELD, event.getOrganizations());
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
            update.set(UPDATED_AT_FIELD, event.getUpdatedAt());
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
            criteriaList.add(Criteria.where(UPDATED_AT_FIELD).gte(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        } else if (criteria.getFrom() > 0) {
            criteriaList.add(Criteria.where(UPDATED_AT_FIELD).gte(new Date(criteria.getFrom())));
        } else if (criteria.getTo() > 0) {
            criteriaList.add(Criteria.where(UPDATED_AT_FIELD).lt(new Date(criteria.getTo())));
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
                Criteria.where(ORGANIZATIONS_FIELD).exists(false),
                Criteria.where(ORGANIZATIONS_FIELD).isNull(),
                Criteria.where(ORGANIZATIONS_FIELD).is(Collections.emptyList()),
                Criteria.where(ORGANIZATIONS_FIELD).in(criteria.getOrganizations())
            );
    }

    private static Criteria buildEnvironmentsCriteria(EventCriteria criteria) {
        return new Criteria()
            .orOperator(
                Criteria.where(ENVIRONMENTS_FIELD).exists(false),
                Criteria.where(ENVIRONMENTS_FIELD).isNull(),
                Criteria.where(ENVIRONMENTS_FIELD).is(Collections.emptyList()),
                Criteria.where(ENVIRONMENTS_FIELD).in(criteria.getEnvironments())
            );
    }

    @Override
    public Stream<EventRepository.EventToClean> findEventsToClean(String environmentId) {
        var criteria = new Criteria().andOperator(Criteria.where(ENVIRONMENTS_FIELD).is(environmentId));
        var query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Use MongoDB streaming to avoid loading all events into memory
        return mongoTemplate
            .stream(query, EventMongo.class)
            .map(event -> {
                // Determine the structured group based on event type and properties
                EventType eventType = Arrays
                    .stream(EventType.values())
                    .filter(e -> e.name().equals(event.getType()))
                    .findFirst()
                    .orElse(null);
                EventRepository.EventToCleanGroup group = EventRepository.EventGroupKeyHelper.determineGroup(
                    eventType,
                    event.getProperties()
                );
                return group != null ? new EventRepository.EventToClean(event.getId(), group) : null;
            })
            .filter(Objects::nonNull) // Filter out events that can't be grouped
            .filter(eventToClean -> eventToClean.group() != null); // Filter out events that can't be grouped
    }
}
