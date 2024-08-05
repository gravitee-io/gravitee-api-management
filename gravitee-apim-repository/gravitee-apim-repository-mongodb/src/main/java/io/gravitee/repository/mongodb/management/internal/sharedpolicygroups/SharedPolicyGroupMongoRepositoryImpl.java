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
package io.gravitee.repository.mongodb.management.internal.sharedpolicygroups;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.SharedPolicyGroupCriteria;
import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupMongo;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class SharedPolicyGroupMongoRepositoryImpl implements SharedPolicyGroupMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<SharedPolicyGroupMongo> search(SharedPolicyGroupCriteria criteria, PageRequest pageRequest) {
        Objects.requireNonNull(criteria, "SharedPolicyGroupCriteria must not be null");
        Objects.requireNonNull(pageRequest, "PageRequest must not be null");

        final Query query = new Query();

        if (criteria.getEnvironmentId() != null) {
            query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
        }
        if (criteria.getName() != null) {
            query.addCriteria(where("name").regex(criteria.getName(), "i"));
        }
        if (criteria.getLifecycleState() != null) {
            query.addCriteria(where("lifecycleState").is(criteria.getLifecycleState().name()));
        }

        final long total = mongoTemplate.count(query, SharedPolicyGroupMongo.class);
        if (total == 0) {
            return new Page<>(List.of(), pageRequest.getPageNumber(), pageRequest.getPageSize(), 0);
        }

        query.with(pageRequest);

        final List<SharedPolicyGroupMongo> sharedPolicyGroups = mongoTemplate.find(query, SharedPolicyGroupMongo.class);

        return new Page<>(sharedPolicyGroups, pageRequest.getPageNumber(), sharedPolicyGroups.size(), total);
    }
}
