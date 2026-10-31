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

import static io.gravitee.repository.mongodb.management.internal.event.EventMongoRepositoryImpl.buildDBCriteria;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.model.EventLatestMongo;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class EventLatestMongoRepositoryImpl implements EventLatestMongoRepositoryCustom {

    private static final String UPDATED_AT = "updatedAt";
    private static final String ID = "_id";

    @Autowired
    private MongoTemplate mongoTemplate;

    private volatile boolean seekIndexFound;
    private volatile boolean seekIndexMissingReported;

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

    @Override
    public List<EventLatestMongo> searchAfter(EventCriteria criteria, Event.EventProperties group, EventLatestCursor after, int size) {
        if (after == null && !hasSeekIndex()) {
            if (!seekIndexMissingReported) {
                seekIndexMissingReported = true;
                log.warn("No index on (type, updatedAt, _id) found on events_latest, latest events are paged by offset until there is one");
            }
            throw new UnsupportedOperationException("No index on (type, updatedAt, _id) found on events_latest");
        }

        List<EventLatestMongo> page = new ArrayList<>();
        boolean undatedNotDone = after == null || after.updatedAt() == null;
        // Undated events have no position in (updatedAt, _id): they are paged first, by id.
        if (undatedNotDone && isUnbounded(criteria)) {
            Criteria undated = after == null
                ? Criteria.where(UPDATED_AT).is(null)
                : new Criteria().andOperator(Criteria.where(UPDATED_AT).is(null), Criteria.where(ID).gt(after.id()));
            page.addAll(find(criteria, group, undated, size));
        }
        if (page.size() < size) {
            page.addAll(find(criteria, group, undatedNotDone ? Criteria.where(UPDATED_AT).ne(null) : after(after), size - page.size()));
        }
        return page;
    }

    private List<EventLatestMongo> find(EventCriteria criteria, Event.EventProperties group, Criteria position, int limit) {
        List<Criteria> clauses = new ArrayList<>();
        if (group != null) {
            clauses.add(Criteria.where("properties." + group.getValue()).exists(true));
        }
        if (criteria != null) {
            clauses.addAll(buildDBCriteria(criteria));
        }
        clauses.add(position);
        Query query = new Query(new Criteria().andOperator(clauses))
            .with(Sort.by(Sort.Order.asc(UPDATED_AT), Sort.Order.asc(ID)))
            .limit(limit);
        return mongoTemplate.find(query, EventLatestMongo.class);
    }

    private static Criteria after(EventLatestCursor after) {
        Date marker = Date.from(after.updatedAt().toInstant(ZoneOffset.UTC));
        return new Criteria().andOperator(
            Criteria.where(UPDATED_AT).gte(marker),
            new Criteria().orOperator(
                Criteria.where(UPDATED_AT).gt(marker),
                new Criteria().andOperator(Criteria.where(UPDATED_AT).is(marker), Criteria.where(ID).gt(after.id()))
            )
        );
    }

    private static boolean isUnbounded(EventCriteria criteria) {
        return criteria == null || (criteria.getFrom() <= 0 && criteria.getTo() <= 0);
    }

    private boolean hasSeekIndex() {
        if (!seekIndexFound) {
            try {
                seekIndexFound = mongoTemplate
                    .indexOps(EventLatestMongo.class)
                    .getIndexInfo()
                    .stream()
                    .map(IndexInfo::getIndexFields)
                    .anyMatch(
                        fields ->
                            fields.size() >= 3 &&
                            isAscending(fields.get(0), "type") &&
                            isAscending(fields.get(1), UPDATED_AT) &&
                            isAscending(fields.get(2), ID)
                    );
            } catch (DataAccessException e) {
                log.debug("Unable to list the indexes of events_latest", e);
            }
        }
        return seekIndexFound;
    }

    private static boolean isAscending(IndexField field, String key) {
        return key.equals(field.getKey()) && field.getDirection() == Sort.Direction.ASC;
    }
}
