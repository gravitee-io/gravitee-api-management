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
package io.gravitee.repository.mongodb.management.internal.guardians;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.mongodb.management.internal.model.GuardianMongo;
import io.gravitee.repository.mongodb.utils.MongoQueries;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author GraviteeSource Team
 */
public class GuardianMongoRepositoryImpl implements GuardianMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoQueries mongoQueries;

    @Override
    public void updateUpdatedAt(String environmentId, String guardianId, Date updatedAt) {
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        Objects.requireNonNull(guardianId, "GuardianId must not be null");
        Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");
        mongoTemplate.updateFirst(
            Query.query(where("environmentId").is(environmentId).and("_id").is(guardianId)),
            new Update().max("updatedAt", updatedAt),
            GuardianMongo.class
        );
    }

    @Override
    public Page<GuardianMongo> search(GuardianCriteria criteria, PageRequest pageRequest) {
        Objects.requireNonNull(criteria, "GuardianCriteria must not be null");
        Objects.requireNonNull(pageRequest, "PageRequest must not be null");

        final Query query = new Query();

        if (criteria.getEnvironmentId() != null) {
            query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
        }
        if (criteria.getQuery() != null) {
            // Quoted, not raw: the JDBC side matches this with LIKE, so the query has to be literal
            // text here too or the same request would answer differently per backend - and an
            // unbalanced metacharacter would make MongoDB reject the query outright.
            query.addCriteria(where("name").regex(Pattern.quote(criteria.getQuery()), "i"));
        }
        if (criteria.getLifecycleState() != null) {
            query.addCriteria(where("lifecycleState").is(criteria.getLifecycleState().name()));
        }

        final long total = mongoQueries.countOrTimeout(mongoTemplate, query, GuardianMongo.class);
        if (total == 0) {
            // 0 elements on the page, not the page size the caller asked for: the JDBC side
            // returns 0 here and the adapter passes this straight into the core Page.
            return new Page<>(List.of(), pageRequest.getPageNumber(), 0, 0);
        }

        query.with(pageRequest);
        final List<GuardianMongo> guardians = mongoTemplate.find(query, GuardianMongo.class);

        return new Page<>(guardians, pageRequest.getPageNumber(), guardians.size(), total);
    }
}
