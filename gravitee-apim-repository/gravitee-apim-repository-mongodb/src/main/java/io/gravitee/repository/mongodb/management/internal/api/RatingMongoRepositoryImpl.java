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
package io.gravitee.repository.mongodb.management.internal.api;

import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

import com.mongodb.client.AggregateIterable;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.RatingSummary;
import io.gravitee.repository.mongodb.management.internal.model.RatingMongo;
import java.util.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingMongoRepositoryImpl implements RatingMongoRepositoryCustom {

    private static final String NUMBER_OF_RATINGS = "numberOfRatings";
    private static final String AVERAGE_RATE = "averageRate";
    private static final String LAST_UPDATED_AT = "lastUpdatedAt";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Map<String, RatingSummary> findSummariesByCriteria(RatingCriteria ratingCriteria) {
        AggregateIterable<Document> ratings = getSummariesByCriteria(ratingCriteria);

        Map<String, RatingSummary> summaries = new LinkedHashMap<>();
        ratings.forEach(
            document -> {
                String referenceId = document.getString("_id");
                Double averageRate = document.getDouble(AVERAGE_RATE);
                Integer numberOfRatings = document.getInteger(NUMBER_OF_RATINGS);
                RatingSummary summary = new RatingSummary();
                summary.setApi(referenceId);
                summary.setAverageRate(averageRate);
                summary.setNumberOfRatings(numberOfRatings);
                summaries.put(referenceId, summary);
            }
        );

        return summaries;
    }

    private AggregateIterable<Document> getSummariesByCriteria(RatingCriteria ratingCriteria) {
        List<Bson> aggregations = new ArrayList<>();
        aggregations.add(match(eq("referenceType", ratingCriteria.getReferenceType().name())));
        if (ratingCriteria.getReferenceIds() != null && !ratingCriteria.getReferenceIds().isEmpty()) {
            aggregations.add(match(in("referenceId", ratingCriteria.getReferenceIds())));
        }
        aggregations.add(match(gt("rate", ratingCriteria.getGt())));
        aggregations.add(group("$referenceId", avg(AVERAGE_RATE, "$rate"), sum(NUMBER_OF_RATINGS, 1), last(LAST_UPDATED_AT, "$updatedAt")));
        aggregations.add(sort(descending(AVERAGE_RATE, NUMBER_OF_RATINGS, LAST_UPDATED_AT)));

        AggregateIterable<Document> ratings = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(RatingMongo.class))
            .aggregate(aggregations);
        return ratings;
    }

    @Override
    public Set<String> computeRanking(RatingCriteria ratingCriteria) {
        AggregateIterable<Document> ratings = getSummariesByCriteria(ratingCriteria);
        Set<String> ranking = new LinkedHashSet<>();
        ratings.forEach(
            document -> {
                String referenceId = document.getString("_id");
                ranking.add(referenceId);
            }
        );
        return ranking;
    }
}
