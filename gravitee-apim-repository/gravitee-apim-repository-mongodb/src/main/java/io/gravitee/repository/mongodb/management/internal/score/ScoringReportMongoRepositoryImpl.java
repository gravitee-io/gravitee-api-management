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

import static com.mongodb.client.model.Accumulators.avg;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

import com.mongodb.client.model.Facet;
import com.mongodb.client.model.UnwindOptions;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ScoringEnvironmentApi;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.management.internal.model.ScoringReportMongo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public Page<ScoringEnvironmentApi> findEnvironmentLatestReports(String environmentId, Pageable pageable) {
        List<Bson> aggregations = new ArrayList<>();
        aggregations.add(match(eq("environmentId", environmentId)));
        aggregations.add(lookup(mongoTemplate.getCollectionName(ScoringReportMongo.class), "_id", "apiId", "scoringReport"));
        aggregations.add(unwind("$scoringReport", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        aggregations.add(
            project(
                new Document(
                    ofEntries(
                        entry("_id", "$scoringReport._id"),
                        entry("apiId", "$_id"),
                        entry("name", 1),
                        entry("updatedAt", 1),
                        entry("environmentId", 1),
                        entry("createdAt", "$scoringReport.createdAt"),
                        entry("pageId", "$scoringReport.pageId"),
                        entry("type", "$scoringReport.type"),
                        entry("summary", "$scoringReport.summary"),
                        entry("assets", "$scoringReport.assets")
                    )
                )
            )
        );
        aggregations.add(sort(new Document(ofEntries(entry("summary.score", -1)))));
        aggregations.add(
            facet(
                new Facet("data", Arrays.asList(skip((pageable.pageNumber()) * pageable.pageSize()), limit(pageable.pageSize()))),
                new Facet("totalCount", List.of(count("count")))
            )
        );

        var result = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ApiMongo.class)).aggregate(aggregations).first();
        List<ScoringEnvironmentApi> data = new ArrayList<>();
        long total = 0;

        if (result != null) {
            data =
                result
                    .getList("data", Document.class)
                    .stream()
                    .map(document -> {
                        var summary = Optional.ofNullable(document.get("summary", Document.class));
                        return ScoringEnvironmentApi
                            .builder()
                            .apiId(document.getString("apiId"))
                            .apiName(document.getString("name"))
                            .apiUpdatedAt(document.getDate("updatedAt"))
                            .reportId(document.getString("_id"))
                            .reportCreatedAt(document.getDate("createdAt"))
                            .score(summary.map(s -> s.getDouble("score")).orElse(null))
                            .errors(summary.map(s -> s.getLong("errors")).orElse(null))
                            .warnings(summary.map(s -> s.getLong("warnings")).orElse(null))
                            .infos(summary.map(s -> s.getLong("infos")).orElse(null))
                            .hints(summary.map(s -> s.getLong("hints")).orElse(null))
                            .build();
                    })
                    .toList();

            total = result.getList("totalCount", Document.class).get(0).getInteger("count");
        }

        return new Page<>(data, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }

    @Override
    public ScoringEnvironmentSummary getScoringEnvironmentSummary(String environmentId) {
        List<Bson> aggregations = new ArrayList<>();
        aggregations.add(match(eq("environmentId", environmentId)));
        aggregations.add(
            group(
                "$environmentId",
                avg("score", "$summary.score"),
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
            .score(BigDecimal.valueOf(result.getDouble("score")).setScale(2, RoundingMode.HALF_EVEN).doubleValue())
            .errors(result.getLong("errors"))
            .warnings(result.getLong("warnings"))
            .infos(result.getLong("infos"))
            .hints(result.getLong("hints"))
            .build();
    }
}
