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
package io.gravitee.repository.mongodb.management.internal.api;

import com.mongodb.client.result.DeleteResult;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.AlertEventMongo;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertEventMongoRepositoryImpl implements AlertEventMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<AlertEventMongo> search(AlertEventCriteria criteria, Pageable pageable) {
        Query query = new Query();

        if (criteria.getAlert() != null) {
            query.addCriteria(Criteria.where("alert").is(criteria.getAlert()));
        }

        // set range query
        if (criteria.getFrom() != 0 && criteria.getTo() != 0) {
            query.addCriteria(Criteria.where("createdAt").gte(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        }

        // set sort by updated at
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(query, AlertEventMongo.class);

        // set pageable
        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<AlertEventMongo> events = mongoTemplate.find(query, AlertEventMongo.class);

        return new Page<>(events, (pageable != null) ? pageable.pageNumber() : 0, events.size(), total);
    }

    @Override
    public void deleteAll(String alertId) {
        Query query = new Query().addCriteria(Criteria.where("alert").is(alertId));
        mongoTemplate.remove(query, AlertEventMongo.class);
    }
}
