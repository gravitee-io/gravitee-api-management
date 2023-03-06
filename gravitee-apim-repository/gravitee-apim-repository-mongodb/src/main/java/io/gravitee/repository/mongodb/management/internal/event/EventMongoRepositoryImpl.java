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
package io.gravitee.repository.mongodb.management.internal.event;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventMongoRepositoryImpl implements EventMongoRepositoryCustom {

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

    private List<Criteria> buildDBCriteria(EventCriteria criteria) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (criteria.getTypes() != null && !criteria.getTypes().isEmpty()) {
            criteriaList.add(Criteria.where("type").in(criteria.getTypes()));
        }

        if (criteria.getProperties() != null && !criteria.getProperties().isEmpty()) {
            // set criteria query
            criteria
                .getProperties()
                .forEach((k, v) -> {
                    if (v instanceof Collection) {
                        criteriaList.add(Criteria.where("properties." + k).in((Collection) v));
                    } else {
                        criteriaList.add(Criteria.where("properties." + k).is(v));
                    }
                });
        }

        // set range query
        if (criteria.getFrom() != 0 && criteria.getTo() != 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        } else if (criteria.getFrom() != 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())));
        }

        if (criteria.getEnvironments() != null && !criteria.getEnvironments().isEmpty()) {
            criteriaList.add(Criteria.where("environments").in(criteria.getEnvironments()));
        }

        return criteriaList;
    }
}
