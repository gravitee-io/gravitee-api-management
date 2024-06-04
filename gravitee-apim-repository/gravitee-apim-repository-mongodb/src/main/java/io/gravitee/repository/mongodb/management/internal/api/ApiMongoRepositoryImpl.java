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
import static java.util.function.Predicate.not;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.StringUtils.hasText;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.AggregateIterable;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.*;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ApiMongo> search(ApiCriteria criteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        final Query query = buildQuery(apiFieldFilter, criteria == null ? List.of() : List.of(criteria));

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

    @VisibleForTesting
    static Query fillQuery(Query query, List<ApiCriteria> orCriteria) {
        if (head(orCriteria) == null) {
            return query;
        }
        List<List<Criteria>> convertedCriteria = orCriteria
            .stream()
            .filter(Objects::nonNull)
            .map(apiCriteria -> {
                List<Criteria> criteria = new ArrayList<>();
                if (head(apiCriteria.getIds()) != null) {
                    criteria.add(where("id").in(apiCriteria.getIds()));
                }
                if (head(apiCriteria.getGroups()) != null) {
                    criteria.add(where("groups").in(apiCriteria.getGroups()));
                }
                if (hasText(apiCriteria.getEnvironmentId())) {
                    criteria.add(where("environmentId").is(apiCriteria.getEnvironmentId()));
                }
                if (head(apiCriteria.getEnvironments()) != null) {
                    criteria.add(where("environmentId").in(apiCriteria.getEnvironments()));
                }
                if (hasText(apiCriteria.getLabel())) {
                    criteria.add(where("labels").in(apiCriteria.getLabel()));
                }
                if (hasText(apiCriteria.getName())) {
                    criteria.add(where("name").is(apiCriteria.getName()));
                }
                if (apiCriteria.getState() != null) {
                    criteria.add(where("lifecycleState").is(apiCriteria.getState()));
                }
                if (hasText(apiCriteria.getVersion())) {
                    criteria.add(where("version").is(apiCriteria.getVersion()));
                }
                if (hasText(apiCriteria.getCategory())) {
                    criteria.add(where("categories").in(apiCriteria.getCategory()));
                }
                if (apiCriteria.getVisibility() != null) {
                    criteria.add(where("visibility").is(apiCriteria.getVisibility()));
                }
                if (head(apiCriteria.getLifecycleStates()) != null) {
                    criteria.add(where("apiLifecycleState").in(apiCriteria.getLifecycleStates()));
                }
                if (hasText(apiCriteria.getCrossId())) {
                    criteria.add(where("crossId").is(apiCriteria.getCrossId()));
                }
                if (head(apiCriteria.getDefinitionVersion()) != null) {
                    criteria.add(where("definitionVersion").in(apiCriteria.getDefinitionVersion()));
                }
                if (hasText(apiCriteria.getIntegrationId())) {
                    criteria.add(where("integrationId").is(apiCriteria.getIntegrationId()));
                }
                if (hasText(apiCriteria.getFilterName())) {
                    criteria.add(where("name").regex(Pattern.quote(apiCriteria.getFilterName()), "i"));
                }
                return criteria;
            })
            .filter(not(List::isEmpty))
            .toList();

        return switch (convertedCriteria.size()) {
            case 1 -> {
                head(convertedCriteria).forEach(query::addCriteria);
                yield query;
            }
            case 0 -> query;
            default -> query.addCriteria(
                new Criteria().orOperator(convertedCriteria.stream().map(criteria -> new Criteria().andOperator(criteria)).toList())
            );
        };
    }

    private static <T> T head(Iterable<T> iterable) {
        return iterable == null || !iterable.iterator().hasNext() ? null : iterable.iterator().next();
    }

    @Override
    public Set<String> listCategories(ApiCriteria apiCriteria) {
        List<Bson> aggregations = new ArrayList<>();
        if (apiCriteria.getIds() != null && !apiCriteria.getIds().isEmpty()) {
            aggregations.add(match(in("_id", apiCriteria.getIds())));
        }
        aggregations.add(unwind("$categories"));
        aggregations.add(group("$categories"));
        aggregations.add(sort(ascending("_id")));

        AggregateIterable<Document> distinctCategories = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(ApiMongo.class))
            .aggregate(aggregations);

        Set<String> categories = new LinkedHashSet<>();
        distinctCategories.forEach(document -> {
            String category = document.getString("_id");
            categories.add(category);
        });
        return categories;
    }
}
