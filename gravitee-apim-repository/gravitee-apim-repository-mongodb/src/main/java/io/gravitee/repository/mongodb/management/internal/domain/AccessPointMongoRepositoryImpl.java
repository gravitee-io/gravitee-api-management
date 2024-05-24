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
package io.gravitee.repository.mongodb.management.internal.domain;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.mongodb.management.internal.model.AccessPointMongo;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class AccessPointMongoRepositoryImpl implements AccessPointMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<AccessPointMongo> search(AccessPointCriteria criteria, Pageable pageable) {
        final Query query = new Query();

        if (criteria.getReferenceType() != null) {
            query.addCriteria(where("referenceType").is(criteria.getReferenceType().name()));
        }

        if (criteria.getTarget() != null) {
            query.addCriteria(where("target").is(criteria.getTarget().name()));
        }

        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            query.addCriteria(where("updatedAt").gte(new Date(criteria.getFrom())).lte(new Date(criteria.getTo())));
        } else {
            if (criteria.getFrom() > 0) {
                query.addCriteria(where("updatedAt").gte(new Date(criteria.getFrom())));
            }
            if (criteria.getTo() > 0) {
                query.addCriteria(where("updatedAt").lte(new Date(criteria.getTo())));
            }
        }

        if (criteria.getEnvironments() != null) {
            query.addCriteria(where("referenceId").in(criteria.getEnvironments()));
        }

        long total = mongoTemplate.count(query, AccessPointMongo.class);
        List<AccessPointMongo> accessPoints = mongoTemplate.find(query, AccessPointMongo.class);

        return new Page<>(accessPoints, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }
}
