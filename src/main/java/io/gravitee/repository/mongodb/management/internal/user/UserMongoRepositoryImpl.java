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
package io.gravitee.repository.mongodb.management.internal.user;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class UserMongoRepositoryImpl implements UserMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<UserMongo> search(UserCriteria criteria, Pageable pageable) {
        Query query = new Query();
        if (criteria != null) {
            if (criteria.getStatuses() != null && criteria.getStatuses().length > 0) {
                query.addCriteria(where("status").in(criteria.getStatuses()));
            }

            if (criteria.hasNoStatus()) {
                query.addCriteria(where("status").exists(false));
            }
            
            if (criteria.getEnvironment() != null) {
                query.addCriteria(where("environment").is(criteria.getEnvironment()));
            }
        }
        query.with(new Sort(Sort.Direction.ASC, "lastname", "firstname"));
        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<UserMongo> users = mongoTemplate.find(query, UserMongo.class);
        long total = mongoTemplate.count(query, UserMongo.class);

        return new Page<>(
                users, (pageable != null) ? pageable.pageNumber() : 0,
                users.size(), total);
    }
}


