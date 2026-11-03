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
package io.gravitee.repository.mongodb.management.internal.notification;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationMongo;
import io.gravitee.repository.mongodb.utils.MongoQueries;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PortalNotificationMongoRepositoryImpl implements PortalNotificationMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final MongoQueries mongoQueries;

    @Override
    public Page<PortalNotificationMongo> search(PortalNotificationSearchCriteria criteria, Pageable pageable) {
        var query = scope(criteria.getUser(), criteria.getEnvironmentId());
        if (criteria.getRead() != null) {
            query.addCriteria(criteria.getRead() ? Criteria.where("readAt").ne(null) : Criteria.where("readAt").is(null));
        }
        if (criteria.getApiId() != null) {
            query.addCriteria(Criteria.where("apiId").is(criteria.getApiId()));
        }
        if (criteria.getApplicationId() != null) {
            query.addCriteria(Criteria.where("applicationId").is(criteria.getApplicationId()));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"));

        long total = mongoQueries.countOrTimeout(mongoTemplate, query, PortalNotificationMongo.class);
        query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));

        var result = mongoTemplate.find(query, PortalNotificationMongo.class);
        return new Page<>(result, pageable.pageNumber(), result.size(), total);
    }

    @Override
    public PortalNotificationReferences findDistinctReferences(String user, String environmentId) {
        return new PortalNotificationReferences(distinct(user, environmentId, "apiId"), distinct(user, environmentId, "applicationId"));
    }

    @Override
    public boolean markAsRead(String user, String environmentId, String id, Date readAt) {
        var unread = scope(user, environmentId).addCriteria(Criteria.where("_id").is(id)).addCriteria(Criteria.where("readAt").is(null));
        if (mongoTemplate.updateFirst(unread, Update.update("readAt", readAt), PortalNotificationMongo.class).getModifiedCount() > 0) {
            return true;
        }
        // A notification already read keeps its first read date and still counts as found. A plain update rather than
        // an aggregation-pipeline one, which not every MongoDB-compatible engine supports.
        return mongoTemplate.exists(scope(user, environmentId).addCriteria(Criteria.where("_id").is(id)), PortalNotificationMongo.class);
    }

    @Override
    public void markAllAsRead(String user, String environmentId, Date readAt) {
        var query = scope(user, environmentId).addCriteria(Criteria.where("readAt").is(null));
        mongoTemplate.updateMulti(query, Update.update("readAt", readAt), PortalNotificationMongo.class);
    }

    @Override
    public boolean delete(String user, String environmentId, String id) {
        var query = scope(user, environmentId).addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, PortalNotificationMongo.class).getDeletedCount() > 0;
    }

    @Override
    public void deleteAll(String user, String environmentId) {
        mongoTemplate.remove(scope(user, environmentId), PortalNotificationMongo.class);
    }

    private Set<String> distinct(String user, String environmentId, String field) {
        var values = new HashSet<>(
            mongoTemplate.findDistinct(scope(user, environmentId), field, PortalNotificationMongo.class, String.class)
        );
        values.removeIf(Objects::isNull);
        return values;
    }

    private static Query scope(String user, String environmentId) {
        return new Query()
            .addCriteria(Criteria.where("user").is(user))
            .addCriteria(
                new Criteria().orOperator(Criteria.where("environmentId").is(environmentId), Criteria.where("environmentId").is(null))
            );
    }
}
