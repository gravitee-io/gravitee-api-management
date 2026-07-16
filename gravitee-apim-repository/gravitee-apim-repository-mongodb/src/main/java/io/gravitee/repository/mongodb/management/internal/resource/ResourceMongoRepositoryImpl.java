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
package io.gravitee.repository.mongodb.management.internal.resource;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.ResourceMongo;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@AllArgsConstructor
public class ResourceMongoRepositoryImpl implements ResourceMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ResourceMongo> search(String referenceType, String referenceId, String query, Pageable pageable) {
        final Query mongoQuery = new Query();
        mongoQuery.addCriteria(where("referenceType").is(referenceType));
        mongoQuery.addCriteria(where("referenceId").is(referenceId));
        if (query != null && !query.isBlank()) {
            mongoQuery.addCriteria(new Criteria().orOperator(where("name").regex(query, "i"), where("type").regex(query, "i")));
        }
        mongoQuery.with(Sort.by(Sort.Direction.ASC, "createdAt"));

        long total = mongoTemplate.count(mongoQuery, ResourceMongo.class);
        if (total == 0) {
            return new Page<>(List.of(), (pageable != null) ? pageable.pageNumber() : 0, 0, 0);
        }

        if (pageable != null) {
            mongoQuery.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<ResourceMongo> resources = mongoTemplate.find(mongoQuery, ResourceMongo.class);
        return new Page<>(resources, (pageable != null) ? pageable.pageNumber() : 0, resources.size(), total);
    }
}
