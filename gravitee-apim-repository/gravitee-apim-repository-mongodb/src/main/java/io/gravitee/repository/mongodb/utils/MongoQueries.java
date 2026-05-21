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
        // Copy so the caller's Query stays clean — subsequent find() reuses the same instance and must not inherit countMaxTimeMs.
        Query countQuery = Query.of(query).maxTimeMsec(countMaxTimeMs);
        try {
            return mongoTemplate.count(countQuery, type);
        } catch (RuntimeException e) {
            if (isExecutionTimeout(e)) {
                log.warn("Count timed out after {}ms on collection {}", countMaxTimeMs, mongoTemplate.getCollectionName(type));
                return -1L;
            }
            throw e;
        }
    }

    public long countAggregationOrTimeout(AggregateIterable<Document> aggregation, String fieldName, String collectionName) {
        try {
            Document first = aggregation.maxTime(countMaxTimeMs, TimeUnit.MILLISECONDS).first();
            if (first == null) {
                return 0L;
            }
            // $count can return Integer or Long depending on collection size; treat as Number to avoid ClassCastException.
            Number value = first.get(fieldName, Number.class);
            return value != null ? value.longValue() : 0L;
        } catch (RuntimeException e) {
            if (isExecutionTimeout(e)) {
                log.warn("Aggregation count timed out after {}ms on collection {}", countMaxTimeMs, collectionName);
                return -1L;
            }
            throw e;
        }
    }

    /**
     * MongoTemplate paths translate driver exceptions via Spring's MongoExceptionTranslator, wrapping
     * MongoExecutionTimeoutException inside DataAccessException. Raw-driver paths (AggregateIterable) throw
     * the unwrapped exception. Check both to stay agnostic to which API surface raised it.
     */
    private static boolean isExecutionTimeout(Throwable t) {
        return (
            t instanceof MongoExecutionTimeoutException ||
            (t instanceof DataAccessException && t.getCause() instanceof MongoExecutionTimeoutException)
        );
    }
}
