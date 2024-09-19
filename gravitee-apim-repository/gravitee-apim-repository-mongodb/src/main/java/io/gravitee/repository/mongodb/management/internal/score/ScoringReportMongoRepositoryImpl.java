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
package io.gravitee.repository.mongodb.management.internal.score;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.model.Accumulators;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.mongodb.management.internal.model.ScoringReportMongo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ScoringReportMongoRepositoryImpl implements ScoringReportMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ScoringReportMongo> findLatestReports(Collection<String> apiIds) {
        Query query = new Query().addCriteria(Criteria.where("apiId").in(apiIds));
        return mongoTemplate.find(query, ScoringReportMongo.class);
    }

    @Override
    public ScoringEnvironmentSummary getScoringEnvironmentSummary(String environmentId) {
        List<Bson> aggregations = new ArrayList<>();
        aggregations.add(match(eq("environmentId", environmentId)));
        aggregations.add(
            group(
                "$environmentId",
                sum("errors", "$summary.errors"),
                sum("warnings", "$summary.warnings"),
                sum("infos", "$summary.infos"),
                sum("hints", "$summary.hints")
            )
        );

        var result = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ScoringReportMongo.class)).aggregate(aggregations).first();

        if (result == null) {
            return ScoringEnvironmentSummary.builder().environmentId(environmentId).build();
        }

        return ScoringEnvironmentSummary
            .builder()
            .environmentId(environmentId)
            .errors(result.getLong("errors"))
            .warnings(result.getLong("warnings"))
            .infos(result.getLong("infos"))
            .hints(result.getLong("hints"))
            .build();
    }
}
