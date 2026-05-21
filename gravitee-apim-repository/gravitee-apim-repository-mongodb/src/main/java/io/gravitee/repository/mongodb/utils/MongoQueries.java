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
package io.gravitee.repository.mongodb.utils;

import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.AggregateIterable;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoQueries {

    private final long countMaxTimeMs;

    public MongoQueries(@Value("${management.mongodb.countMaxTimeMs:5000}") long countMaxTimeMs) {
        this.countMaxTimeMs = countMaxTimeMs;
    }

    public <T> long countOrTimeout(MongoTemplate mongoTemplate, Query query, Class<T> type) {
        query.maxTimeMsec(countMaxTimeMs);
        try {
            return mongoTemplate.count(query, type);
        } catch (DataAccessException e) {
            if (e.getCause() instanceof MongoExecutionTimeoutException) {
                log.warn("Count timed out after {}ms on collection {}", countMaxTimeMs, type.getSimpleName());
                return -1L;
            }
            throw e;
        }
    }

    public long countAggregationOrTimeout(AggregateIterable<Document> aggregation, String fieldName, String collectionName) {
        try {
            Document first = aggregation.maxTime(countMaxTimeMs, TimeUnit.MILLISECONDS).first();
            return first != null ? first.getInteger(fieldName, 0) : 0;
        } catch (MongoExecutionTimeoutException e) {
            log.warn("Count timed out after {}ms on collection {}", countMaxTimeMs, collectionName);
            return -1L;
        }
    }
}
