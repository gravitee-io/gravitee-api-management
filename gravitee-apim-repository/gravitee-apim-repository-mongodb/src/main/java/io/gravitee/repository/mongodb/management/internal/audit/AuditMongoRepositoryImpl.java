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
package io.gravitee.repository.mongodb.management.internal.audit;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.AuditMongo;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuditMongoRepositoryImpl implements AuditMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<AuditMongo> search(AuditCriteria filter, Pageable pageable) {
        final Query query = new Query();
        if (filter.getReferences() != null && !filter.getReferences().isEmpty()) {
            filter
                .getReferences()
                .forEach(
                    (referenceType, referenceIds) -> {
                        if (referenceIds != null && !referenceIds.isEmpty()) {
                            query.addCriteria(where("referenceType").is(referenceType).andOperator(where("referenceId").in(referenceIds)));
                        } else {
                            query.addCriteria(where("referenceType").is(referenceType));
                        }
                    }
                );
        }

        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            query.addCriteria(where("createdAt").gte(new Date(filter.getFrom())).lte(new Date(filter.getTo())));
        } else {
            if (filter.getFrom() != 0) {
                query.addCriteria(where("createdAt").gte(new Date(filter.getFrom())));
            }
            if (filter.getTo() != 0) {
                query.addCriteria(where("createdAt").lte(new Date(filter.getTo())));
            }
        }

        if (filter.getEvents() != null && !filter.getEvents().isEmpty()) {
            query.addCriteria(where("event").in(filter.getEvents()));
        }

        if (filter.getEnvironmentIds() != null && !filter.getEnvironmentIds().isEmpty()) {
            query.addCriteria(where("environmentId").in(filter.getEnvironmentIds()));
        }

        if (filter.getOrganizationId() != null) {
            query.addCriteria(where("organizationId").is(filter.getOrganizationId()));
        }

        long total = mongoTemplate.count(query, AuditMongo.class);

        query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AuditMongo> audits = mongoTemplate.find(query, AuditMongo.class);

        return new Page<>(audits, pageable.pageNumber(), audits.size(), total);
    }
}
