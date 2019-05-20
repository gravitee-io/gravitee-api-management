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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventMongoRepositoryImpl implements EventMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<EventMongo> search(EventCriteria filter, Pageable pageable) {
        Query query = new Query();

        if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
            query.addCriteria(Criteria.where("type").in(filter.getTypes()));
        }

        if (filter.getProperties() != null && !filter.getProperties().isEmpty()) {
            // set criteria query
            filter.getProperties().forEach((k, v) -> {
                if (v instanceof Collection) {
                    query.addCriteria(Criteria.where("properties." + k).in((Collection) v));
                } else {
                    query.addCriteria(Criteria.where("properties." + k).is(v));
                }
            });
        }

        // set range query
        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            query.addCriteria(Criteria.where("updatedAt").gte(new Date(filter.getFrom())).lt(new Date(filter.getTo())));
        }

        // set environment
        if (filter.getEnvironment() != null) {
            query.addCriteria(Criteria.where("environment").is(filter.getEnvironment()));
        }
        
        // set sort by updated at
        query.with(new Sort(Sort.Direction.DESC, "updatedAt"));

        // set pageable
        if (pageable != null) {
            query.with(new PageRequest(pageable.pageNumber(), pageable.pageSize()));
        }

        List<EventMongo> events = mongoTemplate.find(query, EventMongo.class);
        long total = mongoTemplate.count(query, EventMongo.class);

        return new Page<>(
                events, (pageable != null) ? pageable.pageNumber() : 0,
                events.size(), total);
    }
}
