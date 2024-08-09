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
package io.gravitee.repository.mongodb.management.internal.message;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.mongodb.management.internal.model.CommandMongo;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandMongoRepositoryCustomImpl implements CommandMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<CommandMongo> search(CommandCriteria criteria) {
        Query query = new Query();

        if (criteria.getTo() != null && !criteria.getTo().isEmpty()) {
            query.addCriteria(where("to").is(criteria.getTo()));
        }

        if (criteria.getTags() != null && criteria.getTags().length > 0) {
            query.addCriteria(where("tags").in(Arrays.asList(criteria.getTags())));
        }

        if (criteria.isNotExpired()) {
            query.addCriteria(where("expiredAt").gt(new Date()));
        }

        if (criteria.getNotAckBy() != null && !criteria.getNotAckBy().isEmpty()) {
            query.addCriteria(where("acknowledgments").nin(criteria.getNotAckBy()));
        }

        if (criteria.getNotFrom() != null && !criteria.getNotFrom().isEmpty()) {
            query.addCriteria(where("from").ne(criteria.getNotFrom()));
        }

        if (criteria.getOrganizationId() != null && !criteria.getOrganizationId().isEmpty()) {
            query.addCriteria(where("organizationId").is(criteria.getOrganizationId()));
        }

        if (criteria.getEnvironmentId() != null && !criteria.getEnvironmentId().isEmpty()) {
            query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
        }

        return mongoTemplate.find(query, CommandMongo.class);
    }
}
