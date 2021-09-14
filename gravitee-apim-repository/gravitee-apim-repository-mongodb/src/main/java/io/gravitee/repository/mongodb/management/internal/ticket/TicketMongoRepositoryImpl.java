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
package io.gravitee.repository.mongodb.management.internal.ticket;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.mongodb.management.internal.model.TicketMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class TicketMongoRepositoryImpl implements TicketMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<TicketMongo> search(TicketCriteria criteria, Sortable sortable, Pageable pageable) {
        Query query = new Query();

        if (criteria != null) {
            if (StringUtils.isNotEmpty(criteria.getFromUser())) {
                query.addCriteria(where("fromUser").is(criteria.getFromUser()));
            }

            if (StringUtils.isNotEmpty(criteria.getApi())) {
                query.addCriteria(where("api").is(criteria.getApi()));
            }

            if (StringUtils.isNotEmpty(criteria.getApplication())) {
                query.addCriteria(where("application").is(criteria.getApplication()));
            }

            if (sortable != null && StringUtils.isNotEmpty(sortable.field())) {
                query.with(
                    Sort.by(
                        sortable.order().equals(Order.ASC) ? Sort.Direction.ASC : Sort.Direction.DESC,
                        FieldUtils.toCamelCase(sortable.field())
                    )
                );
            }
        }

        long total = mongoTemplate.count(query, TicketMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        // Use a collation strength of 2 to use case insensitive comparison rules.
        query.collation(Collation.of(Locale.ENGLISH).strength(Collation.ComparisonLevel.secondary()));

        List<TicketMongo> tickets = mongoTemplate.find(query, TicketMongo.class);

        return new Page<>(tickets, (pageable != null) ? pageable.pageNumber() : 0, tickets.size(), total);
    }
}
