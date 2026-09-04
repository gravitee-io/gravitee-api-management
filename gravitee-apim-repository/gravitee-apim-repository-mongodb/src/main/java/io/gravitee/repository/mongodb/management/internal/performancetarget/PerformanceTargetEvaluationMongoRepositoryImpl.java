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
package io.gravitee.repository.mongodb.management.internal.performancetarget;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import io.gravitee.repository.mongodb.management.internal.model.PerformanceTargetEvaluationMongo;
import java.util.List;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PerformanceTargetEvaluationMongoRepositoryImpl implements PerformanceTargetEvaluationMongoRepositoryCustom {

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Order.desc("evaluatedAt"), Sort.Order.asc("_id"));

    private final MongoTemplate mongoTemplate;

    @Override
    public void unsetLatest(String targetId) {
        mongoTemplate.updateMulti(
            query(where("targetId").is(targetId).and("latest").is(true)),
            new Update().set("latest", false),
            PerformanceTargetEvaluationMongo.class
        );
    }

    @Override
    public Page<PerformanceTargetEvaluationMongo> findEnvironmentLatest(String environmentId, Pageable pageable) {
        return pageMostRecentFirst(query(where("environmentId").is(environmentId).and("latest").is(true)), pageable);
    }

    @Override
    public Page<PerformanceTargetEvaluationMongo> findByTargetId(String targetId, Pageable pageable) {
        return pageMostRecentFirst(query(where("targetId").is(targetId)), pageable);
    }

    private Page<PerformanceTargetEvaluationMongo> pageMostRecentFirst(Query query, Pageable pageable) {
        long total = mongoTemplate.count(query, PerformanceTargetEvaluationMongo.class);
        var content = mongoTemplate.find(
            query.with(MOST_RECENT_FIRST).skip((long) pageable.pageNumber() * pageable.pageSize()).limit(pageable.pageSize()),
            PerformanceTargetEvaluationMongo.class
        );
        return new Page<>(content, pageable.pageNumber(), content.size(), total);
    }

    @Override
    public List<String> pruneHistory(String targetId, int retention) {
        var beyondRetention = query(where("targetId").is(targetId)).with(MOST_RECENT_FIRST).skip(retention);
        beyondRetention.fields().include("_id");
        var pruned = mongoTemplate
            .find(beyondRetention, PerformanceTargetEvaluationMongo.class)
            .stream()
            .map(PerformanceTargetEvaluationMongo::getId)
            .toList();
        if (!pruned.isEmpty()) {
            mongoTemplate.remove(query(where("_id").in(pruned)), PerformanceTargetEvaluationMongo.class);
        }
        return pruned;
    }

    @Override
    public PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) {
        var countByStatus = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(PerformanceTargetEvaluationMongo.class))
            .aggregate(List.of(match(and(eq("environmentId", environmentId), eq("latest", true))), group("$status", sum("count", 1))));

        var summary = PerformanceTargetEnvironmentSummary.builder().environmentId(environmentId).build();
        for (Document bucket : countByStatus) {
            long count = ((Number) bucket.get("count")).longValue();
            switch (PerformanceTargetEvaluation.Status.valueOf(bucket.getString("_id"))) {
                case PASS -> summary.setPass(count);
                case BREACH -> summary.setBreach(count);
                case NOT_EVALUABLE -> summary.setNotEvaluable(count);
            }
        }
        return summary;
    }
}
