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
package io.gravitee.repository.mongodb.management.internal.asyncjob;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.integration.IntegrationMongoRepositoryCustom;
import io.gravitee.repository.mongodb.management.internal.model.AsyncJobMongo;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AsyncJobMongoRepositoryImpl implements AsyncJobMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<AsyncJobMongo> search(AsyncJobRepository.SearchCriteria criteria, Pageable pageable) {
        var query = toQuery(criteria).with(Sort.by(Sort.Direction.DESC, "updatedAt"));

        long total = mongoTemplate.count(query, AsyncJobMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        var result = mongoTemplate.find(query, AsyncJobMongo.class);
        return new Page<>(result, (pageable != null) ? pageable.pageNumber() : 0, result.size(), total);
    }

    private Query toQuery(AsyncJobRepository.SearchCriteria criteria) {
        Query query = new Query().addCriteria(Criteria.where("environmentId").is(criteria.environmentId()));
        criteria.initiatorId().ifPresent(initiatorId -> query.addCriteria(Criteria.where("initiatorId").is(initiatorId)));
        criteria.type().ifPresent(type -> query.addCriteria(Criteria.where("type").is(type)));
        criteria.status().ifPresent(status -> query.addCriteria(Criteria.where("status").is(status)));
        criteria.sourceId().ifPresent(sourceId -> query.addCriteria(Criteria.where("sourceId").is(sourceId)));
        return query;
    }
}
