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
package io.gravitee.repository.mongodb.management.internal.sharedpolicygrouphistory;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.SharedPolicyGroupHistoryCriteria;
import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupHistoryMongo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;

public class SharedPolicyGroupHistoryMongoRepositoryImpl implements SharedPolicyGroupHistoryMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<SharedPolicyGroupHistoryMongo> search(SharedPolicyGroupHistoryCriteria criteria, PageRequest pageRequest) {
        Objects.requireNonNull(criteria, "SharedPolicyGroupHistoryCriteria must not be null");
        Objects.requireNonNull(pageRequest, "PageRequest must not be null");

        final Query query = new Query();

        if (criteria.getEnvironmentId() != null) {
            query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
        }
        if (criteria.getSharedPolicyGroupId() != null) {
            query.addCriteria(where("id").is(criteria.getSharedPolicyGroupId()));
        }
        if (criteria.getLifecycleState() != null) {
            query.addCriteria(where("lifecycleState").is(criteria.getLifecycleState().name()));
        }

        final long total = mongoTemplate.count(query, SharedPolicyGroupHistoryMongo.class);
        if (total == 0) {
            return new Page<>(List.of(), pageRequest.getPageNumber(), pageRequest.getPageSize(), 0);
        }

        query.with(pageRequest);

        final List<SharedPolicyGroupHistoryMongo> sharedPolicyGroups = mongoTemplate.find(query, SharedPolicyGroupHistoryMongo.class);

        return new Page<>(sharedPolicyGroups, pageRequest.getPageNumber(), sharedPolicyGroups.size(), total);
    }

    @Override
    public Page<SharedPolicyGroupHistoryMongo> searchLatestBySharedPolicyGroupId(String environmentId, int page, int size) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        var collectionName = mongoTemplate.getCollectionName(SharedPolicyGroupHistoryMongo.class);

        // Main aggregation operations
        final List<AggregationOperation> mainAggregationOperations = new ArrayList<>();
        // Add Criteria
        mainAggregationOperations.add(Aggregation.match(where("environmentId").is(environmentId)));
        // Keep only the fields we need
        //      _id: mongo id,
        //      id: shared policy group id,
        //      name, deployedAt of the shared policy group
        mainAggregationOperations.add(Aggregation.project(Aggregation.fields("_id", "id", "name", "deployedAt")));
        // Sort by updatedAt
        mainAggregationOperations.add(Aggregation.sort(Sort.Direction.DESC, "deployedAt"));
        // Then group by id and save the first element of the group
        mainAggregationOperations.add(Aggregation.group("id").first("$$ROOT").as("partialSpgh"));
        // Sort by name
        mainAggregationOperations.add(Aggregation.sort(Sort.Direction.ASC, "partialSpgh.name"));

        // First aggregation to get the total
        var aggregationOperationsForTotal = new ArrayList<>(mainAggregationOperations);
        aggregationOperationsForTotal.add(Aggregation.count().as("count"));

        var aggregationResultsTotal = mongoTemplate
            .aggregate(Aggregation.newAggregation(aggregationOperationsForTotal), collectionName, Document.class)
            .getUniqueMappedResult();
        long total = aggregationResultsTotal != null ? ((Integer) aggregationResultsTotal.get("count")).longValue() : 0;

        if (total == 0) {
            return new Page<>(List.of(), page, size, 0);
        }

        // Second aggregation to get the data with paging
        final List<AggregationOperation> aggregationResultsWithPaging = new ArrayList<>(mainAggregationOperations);
        // For paging
        var pageRequest = PageRequest.of(page, size);
        aggregationResultsWithPaging.add(Aggregation.skip(pageRequest.getOffset()));
        aggregationResultsWithPaging.add(Aggregation.limit(pageRequest.getPageSize()));
        // Lookup against shared policy group history collection again to retrieve full shared policy group history with payload but limited to the page size to preserve mongodb memory.
        aggregationResultsWithPaging.add(Aggregation.lookup(collectionName, "partialSpgh._id", "_id", "lookup_spgh"));
        aggregationResultsWithPaging.add(Aggregation.unwind("lookup_spgh"));
        aggregationResultsWithPaging.add(Aggregation.replaceRoot("lookup_spgh"));

        var aggregationResults = mongoTemplate.aggregate(
            Aggregation.newAggregation(aggregationResultsWithPaging),
            collectionName,
            SharedPolicyGroupHistoryMongo.class
        );

        List<SharedPolicyGroupHistoryMongo> sharedPolicyGroups = aggregationResults.getMappedResults();

        return new Page<>(sharedPolicyGroups, pageRequest.getPageNumber(), pageRequest.getPageSize(), total);
    }

    @Override
    public void deleteBySharedPolicyGroupId(String sharedPolicyGroupId) {
        Objects.requireNonNull(sharedPolicyGroupId, "sharedPolicyGroupId must not be null");
        var query = new Query(where("id").is(sharedPolicyGroupId));
        mongoTemplate.remove(query, SharedPolicyGroupHistoryMongo.class);
    }
}
