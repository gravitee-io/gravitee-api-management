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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ApiMongo> search(final ApiCriteria criteria, final Pageable pageable,
                                 final ApiFieldExclusionFilter apiFieldExclusionFilter) {
        final Query query = new Query();

        if (apiFieldExclusionFilter != null) {
            if (apiFieldExclusionFilter.isDefinition()) {
                query.fields().exclude("definition");
            }
            if (apiFieldExclusionFilter.isPicture()) {
                query.fields().exclude("picture");
            }
        }

        if (criteria != null) {
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                query.addCriteria(where("id").in(criteria.getIds()));
            }
            if (criteria.getGroups() != null && !criteria.getGroups().isEmpty()) {
                query.addCriteria(where("groups").in(criteria.getGroups()));
            }
            if (criteria.getEnvironment() != null) {
                query.addCriteria(where("environment").is(criteria.getEnvironment()));
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
            if (criteria.getView() != null && !criteria.getView().isEmpty()) {
                query.addCriteria(where("views").in(criteria.getView()));
            }
            if (criteria.getVisibility() != null) {
                query.addCriteria(where("visibility").is(criteria.getVisibility()));
            }
            if (criteria.getLifecycleStates() != null && !criteria.getLifecycleStates().isEmpty()) {
                query.addCriteria(where("apiLifecycleState").in(criteria.getLifecycleStates()));
            }
        }

        query.with(new Sort(ASC, "name"));

        if (pageable != null) {
            query.with(new PageRequest(pageable.pageNumber(), pageable.pageSize()));
        }

        List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
        long total = mongoTemplate.count(query, ApiMongo.class);

        return new Page<>(apis,
                pageable != null ? pageable.pageNumber() : 0,
                pageable != null ? pageable.pageSize() : 0,
                total);
    }
}
