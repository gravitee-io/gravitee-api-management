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
package io.gravitee.repository.mongodb.management.internal.application;

import static io.gravitee.repository.management.api.ApplicationRepository.SORTABLE_FIELDS;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMongoRepositoryImpl implements ApplicationMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApplicationMongo> search(final ApplicationCriteria criteria, final Pageable pageable, Sortable sortable) {
        final Query query = new Query();

        query.fields().exclude("background");
        query.fields().exclude("picture");

        if (criteria != null) {
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                query.addCriteria(where("id").in(criteria.getIds()));
            }
            if (criteria.getEnvironmentIds() != null) {
                query.addCriteria(where("environmentId").in(criteria.getEnvironmentIds()));
            }
            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                query.addCriteria(where("name").regex(criteria.getName(), "i"));
            }
            if (criteria.getStatus() != null) {
                query.addCriteria(where("status").is(criteria.getStatus()));
            }
            if (criteria.getGroups() != null && !criteria.getGroups().isEmpty()) {
                query.addCriteria(where("groups").in(criteria.getGroups()));
            }
        }

        Sort.Direction direction = toSortDirection(sortable);
        Sort.Order order;
        if (sortable == null || !SORTABLE_FIELDS.contains(sortable.field())) {
            order = new Sort.Order(direction, "name");
        } else {
            order = new Sort.Order(direction, FieldUtils.toCamelCase(sortable.field()));
        }

        query.with(Sort.by(order));

        long total = mongoTemplate.count(query, ApplicationMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<ApplicationMongo> apps = mongoTemplate.find(query, ApplicationMongo.class);

        return new Page<>(apps, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }

    private Sort.Direction toSortDirection(Sortable sortable) {
        if (sortable != null) {
            return Order.DESC.equals(sortable.order()) ? Sort.Direction.DESC : ASC;
        }
        return Sort.Direction.ASC;
    }

    @Override
    public Set<ApplicationMongo> findByIds(Collection<String> ids, Sortable sortable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(ids));
        if (sortable != null && StringUtils.isNotEmpty(sortable.field())) {
            query.with(Sort.by(Order.DESC.equals(sortable.order()) ? Sort.Direction.DESC : ASC, FieldUtils.toCamelCase(sortable.field())));
        }
        List<ApplicationMongo> applications = mongoTemplate.find(query, ApplicationMongo.class);
        return new LinkedHashSet<>(applications);
    }
}
