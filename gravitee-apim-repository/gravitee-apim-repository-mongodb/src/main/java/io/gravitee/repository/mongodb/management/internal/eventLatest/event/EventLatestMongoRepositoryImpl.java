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
package io.gravitee.repository.mongodb.management.internal.eventLatest.event;

import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.model.EventLatestMongo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventLatestMongoRepositoryImpl implements EventLatestMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<EventLatestMongo> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
        final String collectionName = mongoTemplate.getCollectionName(EventLatestMongo.class);

        Aggregation aggregation;
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        if (group != null) {
            aggregationOperations.add(Aggregation.match(Criteria.where("properties." + group.getValue()).exists(true)));
        }
        if (criteria != null) {
            List<Criteria> criteriaList = buildDBCriteria(criteria);
            if (!criteriaList.isEmpty()) {
                aggregationOperations.add(Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0]))));
            }
        }

        // Project only useful field to avoid memory consumption during pipeline execution on mongodb side (this excludes the payload from sort and group and avoid 'Command failed with error 292').
        aggregationOperations.add(Aggregation.project(Aggregation.fields("_id", "updatedAt", "type", "properties")));

        // Sort.
        aggregationOperations.add(Aggregation.sort(Sort.Direction.ASC, "updatedAt", "_id"));

        // Pagination
        if (page != null && size != null && size > 0) {
            aggregationOperations.add(Aggregation.skip(page * size));
        }

        if (size != null) {
            aggregationOperations.add(Aggregation.limit(size));
        }

        // Lookup against events collection again to retrieve full events with payload but limited to the page size to preserve mongodb memory.
        aggregationOperations.add(Aggregation.lookup(collectionName, "_id", "_id", "lookup_events"));
        aggregationOperations.add(Aggregation.unwind("lookup_events"));
        aggregationOperations.add(Aggregation.replaceRoot("lookup_events"));

        aggregation = Aggregation.newAggregation(aggregationOperations);

        final AggregationResults<EventLatestMongo> events = mongoTemplate.aggregate(aggregation, collectionName, EventLatestMongo.class);

        return events.getMappedResults();
    }

    protected List<Criteria> buildDBCriteria(final EventCriteria criteria) {
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
                        criteriaList.add(Criteria.where("properties." + k).in((Collection) v));
                    } else {
                        criteriaList.add(Criteria.where("properties." + k).is(v));
                    }
                });
        }

        // set range query
        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())).lte(new Date(criteria.getTo())));
        } else if (criteria.getFrom() > 0) {
            criteriaList.add(Criteria.where("updatedAt").gte(new Date(criteria.getFrom())));
        } else if (criteria.getTo() > 0) {
            criteriaList.add(Criteria.where("updatedAt").lte(new Date(criteria.getTo())));
        }

        if (!isEmpty(criteria.getEnvironments())) {
            criteriaList.add(Criteria.where("environments").in(criteria.getEnvironments()));
        }

        return criteriaList;
    }
}
