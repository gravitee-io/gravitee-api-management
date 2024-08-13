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
package io.gravitee.repository.mongodb.management.internal.api;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.ascending;
import static java.util.Collections.emptyList;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Accumulators;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.*;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApiMongo> search(ApiCriteria criteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        final Query query = buildQuery(apiFieldFilter, criteria == null ? emptyList() : List.of(criteria));

        Sort sort;
        if (sortable == null) {
            sort = Sort.by(ASC, "name");
        } else {
            Sort.Direction sortOrder = sortable.order().equals(Order.ASC) ? ASC : Sort.Direction.DESC;
            sort = Sort.by(sortOrder, FieldUtils.toCamelCase(sortable.field()));
        }

        long total = mongoTemplate.count(query, ApiMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sort));
        }

        List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);

        return new Page<>(apis, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }

    @Override
    public Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable) {
        Objects.requireNonNull(pageable, "Pageable must not be null");

        final Query query = new Query();
        // Only fetch the id
        query.fields().include("_id");
        fillQuery(query, apiCriteria);

        Sort sort;
        if (sortable == null) {
            sort = Sort.by(ASC, "name");
        } else {
            Sort.Direction sortOrder = sortable.order().equals(Order.ASC) ? ASC : Sort.Direction.DESC;
            sort = Sort.by(sortOrder, FieldUtils.toCamelCase(sortable.field()));
        }

        // Get total count before adding pagination to the query
        long total = mongoTemplate.count(query, ApiMongo.class);

        // set pageable
        query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sort));

        List<String> apisIds = mongoTemplate.find(query, ApiMongo.class).stream().map(ApiMongo::getId).collect(Collectors.toList());

        return new Page<>(apisIds, pageable.pageNumber(), apisIds.size(), total);
    }

    private Query buildQuery(ApiFieldFilter apiFieldFilter, List<ApiCriteria> orApiCriteria) {
        final Query query = new Query();

        if (apiFieldFilter != null) {
            if (apiFieldFilter.isDefinitionExcluded()) {
                query.fields().exclude("definition");
            }
            if (apiFieldFilter.isPictureExcluded()) {
                query.fields().exclude("picture");
                query.fields().exclude("background");
            }
        }

        fillQuery(query, orApiCriteria);

        return query;
    }

    private Query fillQuery(Query query, List<ApiCriteria> orCriteria) {
        if (orCriteria != null && orCriteria.size() > 0) {
            List<List<Criteria>> convertedCriteria = orCriteria
                .stream()
                .filter(Objects::nonNull)
                .map(apiCriteria -> {
                    List<Criteria> criteria = new ArrayList<>();
                    if (apiCriteria.getIds() != null && !apiCriteria.getIds().isEmpty()) {
                        criteria.add(where("id").in(apiCriteria.getIds()));
                    }
                    if (apiCriteria.getGroups() != null && !apiCriteria.getGroups().isEmpty()) {
                        criteria.add(where("groups").in(apiCriteria.getGroups()));
                    }
                    if (apiCriteria.getEnvironmentId() != null) {
                        criteria.add(where("environmentId").is(apiCriteria.getEnvironmentId()));
                    }
                    if (apiCriteria.getEnvironments() != null && !apiCriteria.getEnvironments().isEmpty()) {
                        criteria.add(where("environmentId").in(apiCriteria.getEnvironments()));
                    }
                    if (apiCriteria.getLabel() != null && !apiCriteria.getLabel().isEmpty()) {
                        criteria.add(where("labels").in(apiCriteria.getLabel()));
                    }
                    if (apiCriteria.getName() != null && !apiCriteria.getName().isEmpty()) {
                        criteria.add(where("name").is(apiCriteria.getName()));
                    }
                    if (apiCriteria.getState() != null) {
                        criteria.add(where("lifecycleState").is(apiCriteria.getState()));
                    }
                    if (apiCriteria.getVersion() != null && !apiCriteria.getVersion().isEmpty()) {
                        criteria.add(where("version").is(apiCriteria.getVersion()));
                    }
                    if (apiCriteria.getCategory() != null && !apiCriteria.getCategory().isEmpty()) {
                        criteria.add(where("categories").in(apiCriteria.getCategory()));
                    }
                    if (apiCriteria.getVisibility() != null) {
                        criteria.add(where("visibility").is(apiCriteria.getVisibility()));
                    }
                    if (apiCriteria.getLifecycleStates() != null && !apiCriteria.getLifecycleStates().isEmpty()) {
                        criteria.add(where("apiLifecycleState").in(apiCriteria.getLifecycleStates()));
                    }
                    if (apiCriteria.getCrossId() != null && !apiCriteria.getCrossId().isEmpty()) {
                        criteria.add(where("crossId").is(apiCriteria.getCrossId()));
                    }
                    if (apiCriteria.getDefinitionVersion() != null && !apiCriteria.getDefinitionVersion().isEmpty()) {
                        criteria.add(where("definitionVersion").in(apiCriteria.getDefinitionVersion()));
                    }
                    return criteria;
                })
                .filter(criteria -> criteria.size() > 0)
                .collect(Collectors.toList());

            if (convertedCriteria.size() > 1) {
                query.addCriteria(
                    new Criteria()
                        .orOperator(
                            convertedCriteria.stream().map(criteria -> new Criteria().andOperator(criteria)).collect(Collectors.toList())
                        )
                );
            } else if (convertedCriteria.size() == 1) {
                convertedCriteria.get(0).forEach(query::addCriteria);
            }
        }
        return query;
    }

    @Override
    public Map<String, Integer> listCategories(ApiCriteria apiCriteria) {
        List<Bson> aggregations = new ArrayList<>();
        if (apiCriteria.getIds() != null && !apiCriteria.getIds().isEmpty()) {
            aggregations.add(match(in("_id", apiCriteria.getIds())));
        }
        aggregations.add(unwind("$categories"));
        aggregations.add(group("$categories", Accumulators.sum("totalApis", 1)));
        aggregations.add(sort(ascending("_id")));

        AggregateIterable<Document> distinctCategories = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiMongo.class))
            .aggregate(aggregations);

        Map<String, Integer> categories = new LinkedHashMap<>();
        distinctCategories.forEach(document -> {
            String category = document.getString("_id");
            Integer totalApis = document.getInteger("totalApis");
            categories.put(category, totalApis);
        });
        return categories;
    }
}
