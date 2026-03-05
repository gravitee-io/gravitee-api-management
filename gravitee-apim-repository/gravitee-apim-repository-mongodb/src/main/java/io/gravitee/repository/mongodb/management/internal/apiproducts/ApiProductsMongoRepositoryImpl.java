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
package io.gravitee.repository.mongodb.management.internal.apiproducts;

import static java.util.Objects.requireNonNull;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiProductCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApiProductMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

public class ApiProductsMongoRepositoryImpl implements ApiProductsMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Set<ApiProductMongo> findByApiId(String apiId) {
        Query query = new Query(Criteria.where("apiIds").is(apiId));
        return new HashSet<>(mongoTemplate.find(query, ApiProductMongo.class));
    }

    @Override
    public Set<ApiProductMongo> findApiProductsByApiIds(Collection<String> apiIds) {
        if (CollectionUtils.isEmpty(apiIds)) {
            return Set.of();
        }
        Query query = new Query(Criteria.where("apiIds").in(apiIds));
        return new HashSet<>(mongoTemplate.find(query, ApiProductMongo.class));
    }

    @Override
    public Page<String> searchIds(List<ApiProductCriteria> apiProductCriteriaList, Pageable pageable, Sortable sortable) {
        requireNonNull(pageable, "Pageable must not be null");

        Query query = new Query();
        query.fields().include("_id");
        fillQuery(query, apiProductCriteriaList);

        Sort sort;
        if (sortable == null) {
            sort = Sort.by(ASC, "name");
        } else {
            Sort.Direction sortOrder = sortable.order() == Order.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(sortOrder, FieldUtils.toCamelCase(sortable.field()));
        }

        long total = mongoTemplate.count(query, ApiProductMongo.class);
        query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sort));

        List<String> ids = mongoTemplate
            .find(query, ApiProductMongo.class)
            .stream()
            .map(ApiProductMongo::getId)
            .collect(Collectors.toList());

        return new Page<>(ids, pageable.pageNumber(), ids.size(), total);
    }

    private void fillQuery(Query query, List<ApiProductCriteria> orCriteria) {
        if (orCriteria == null || orCriteria.isEmpty()) {
            return;
        }
        List<List<Criteria>> convertedCriteria = orCriteria
            .stream()
            .filter(Objects::nonNull)
            .map(criteria -> {
                List<Criteria> list = new ArrayList<>();
                if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                    list.add(where("id").in(criteria.getIds()));
                }
                if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                    list.add(where("name").is(criteria.getName()));
                }
                if (criteria.getVersion() != null && !criteria.getVersion().isEmpty()) {
                    list.add(where("version").is(criteria.getVersion()));
                }
                if (criteria.getEnvironmentId() != null && !criteria.getEnvironmentId().isEmpty()) {
                    list.add(where("environmentId").is(criteria.getEnvironmentId()));
                }
                if (criteria.getEnvironments() != null && !criteria.getEnvironments().isEmpty()) {
                    list.add(where("environmentId").in(criteria.getEnvironments()));
                }
                if (criteria.getApiIds() != null && !criteria.getApiIds().isEmpty()) {
                    list.add(where("apiIds").in(criteria.getApiIds()));
                }
                return list;
            })
            .filter(criteria -> !criteria.isEmpty())
            .collect(Collectors.toList());

        if (convertedCriteria.size() > 1) {
            query.addCriteria(
                new Criteria().orOperator(
                    convertedCriteria
                        .stream()
                        .map(criteria -> new Criteria().andOperator(criteria))
                        .collect(Collectors.toList())
                )
            );
        } else if (convertedCriteria.size() == 1) {
            convertedCriteria.get(0).forEach(query::addCriteria);
        }
    }
}
