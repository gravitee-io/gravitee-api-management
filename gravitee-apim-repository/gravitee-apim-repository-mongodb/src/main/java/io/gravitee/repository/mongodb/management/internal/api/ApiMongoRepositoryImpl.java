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

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.ascending;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.AggregateIterable;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApiMongo> search(
        final ApiCriteria criteria,
        final Pageable pageable,
        final ApiFieldExclusionFilter apiFieldExclusionFilter
    ) {
        final Query query = queryFromCriteria(criteria);

        if (apiFieldExclusionFilter != null) {
            if (apiFieldExclusionFilter.isDefinition()) {
                query.fields().exclude("definition");
            }
            if (apiFieldExclusionFilter.isPicture()) {
                query.fields().exclude("picture");
                query.fields().exclude("background");
            }
        }

        query.with(Sort.by(ASC, "name"));

        long total = mongoTemplate.count(query, ApiMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
        if (criteria != null && criteria.getContextPath() != null && !criteria.getContextPath().isEmpty()) {
            apis =
                apis
                    .stream()
                    .filter(
                        apiMongo -> {
                            try {
                                io.gravitee.definition.model.Api apiDefinition = new GraviteeMapper()
                                .readValue(apiMongo.getDefinition(), io.gravitee.definition.model.Api.class);
                                VirtualHost searchedVHost = new VirtualHost();
                                searchedVHost.setPath(criteria.getContextPath());
                                return apiDefinition.getProxy().getVirtualHosts().contains(searchedVHost);
                            } catch (JsonProcessingException e) {
                                logger.error("Problem occured while parsing api definition", e);
                                return false;
                            }
                        }
                    )
                    .collect(Collectors.toList());
        }

        return new Page<>(apis, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : apis.size(), total);
    }

    @Override
    public List<ApiMongo> search(ApiCriteria criteria, ApiFieldInclusionFilter apiFieldInclusionFilter) {
        Query query = queryFromCriteria(criteria);
        query.fields().include(apiFieldInclusionFilter.includedFields());
        return mongoTemplate.find(query, ApiMongo.class);
    }

    private Query queryFromCriteria(ApiCriteria criteria) {
        Query query = new Query();
        if (criteria != null) {
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                query.addCriteria(where("id").in(criteria.getIds()));
            }
            if (criteria.getGroups() != null && !criteria.getGroups().isEmpty()) {
                query.addCriteria(where("groups").in(criteria.getGroups()));
            }
            if (criteria.getEnvironmentId() != null) {
                query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
            }
            if (criteria.getEnvironments() != null && !criteria.getEnvironments().isEmpty()) {
                query.addCriteria(where("environmentId").in(criteria.getEnvironments()));
            }
            if (criteria.getLabel() != null && !criteria.getLabel().isEmpty()) {
                query.addCriteria(where("labels").in(criteria.getLabel()));
            }
            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                query.addCriteria(where("name").is(criteria.getName()));
            }
            if (criteria.getState() != null) {
                query.addCriteria(where("lifecycleState").is(criteria.getState()));
            }
            if (criteria.getVersion() != null && !criteria.getVersion().isEmpty()) {
                query.addCriteria(where("version").is(criteria.getVersion()));
            }
            if (criteria.getCategory() != null && !criteria.getCategory().isEmpty()) {
                query.addCriteria(where("categories").in(criteria.getCategory()));
            }
            if (criteria.getVisibility() != null) {
                query.addCriteria(where("visibility").is(criteria.getVisibility()));
            }
            if (criteria.getLifecycleStates() != null && !criteria.getLifecycleStates().isEmpty()) {
                query.addCriteria(where("apiLifecycleState").in(criteria.getLifecycleStates()));
            }
        }
        return query;
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
        distinctCategories.forEach(
            document -> {
                String category = document.getString("_id");
                categories.add(category);
            }
        );
        return categories;
    }
}
