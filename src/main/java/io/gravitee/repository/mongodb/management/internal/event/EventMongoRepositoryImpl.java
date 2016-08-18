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
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE
 */
public class EventMongoRepositoryImpl implements EventMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Collection<EventMongo> findByType(Collection<String> types) {
        Query query = new Query();
        query.addCriteria(Criteria.where("type").in(types));

        List<EventMongo> events = mongoTemplate.find(query, EventMongo.class);

        return events;
    }

    @Override
    public Collection<EventMongo> findByProperty(String key, String value) {
        Query query = new Query();
        query.addCriteria(Criteria.where("properties." + key).is(value));

        List<EventMongo> events = mongoTemplate.find(query, EventMongo.class);

        return events;
    }

    @Override
    public Page<EventMongo> search(Map<String, Object> values, long from, long to, int page, int size) {
        Query query = new Query();

        // set criteria query
        values.forEach((k, v) -> {
            if (v instanceof Collection) {
                query.addCriteria(Criteria.where(k).in((Collection) v));
            } else {
                query.addCriteria(Criteria.where(k).is(v));
            }
        });

        // set range query
        query.addCriteria(Criteria.where("updatedAt").gte(new Date(from)).lt(new Date(to)));

        // set sort by updated at
        query.with(new Sort(Sort.Direction.DESC, "updatedAt"));

        // set pageable
        query.with(new PageRequest(page, size));

        List<EventMongo> events = mongoTemplate.find(query, EventMongo.class);
        long total = mongoTemplate.count(query, EventMongo.class);

        Page<EventMongo> eventsPage = new Page<>(events, page, size, total);

        return eventsPage;
    }
}
