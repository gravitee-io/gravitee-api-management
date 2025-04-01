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
package io.gravitee.repository.mongodb.management.internal.domain;

import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.mongodb.management.internal.model.AccessPointMongo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

public class AccessPointMongoRepositoryImpl implements AccessPointMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<AccessPointMongo> search(AccessPointCriteria criteria, Long page, Long size) {
        final String collectionName = mongoTemplate.getCollectionName(AccessPointMongo.class);

        Aggregation aggregation;
        List<AggregationOperation> aggregationOperations = new ArrayList<>();

        if (criteria != null) {
            List<Criteria> criteriaList = buildDBCriteria(criteria);
            if (!criteriaList.isEmpty()) {
                aggregationOperations.add(Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0]))));
            }
        }

        aggregationOperations.add(Aggregation.sort(Sort.Direction.ASC, "updatedAt", "_id"));

        if (page != null && size != null && size > 0) {
            aggregationOperations.add(Aggregation.skip(page * size));
        }

        if (size != null) {
            aggregationOperations.add(Aggregation.limit(size));
        }

        aggregation = Aggregation.newAggregation(aggregationOperations);
        final AggregationResults<AccessPointMongo> results = mongoTemplate.aggregate(aggregation, collectionName, AccessPointMongo.class);
        return results.getMappedResults();
    }

    protected List<Criteria> buildDBCriteria(final AccessPointCriteria criteria) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (criteria.getReferenceType() != null) {
            criteriaList.add(Criteria.where("referenceType").is(criteria.getReferenceType().name()));
        }

        if (criteria.getTarget() != null) {
            criteriaList.add(Criteria.where("target").is(criteria.getTarget().name()));
        }

        if (criteria.getTargets() != null && !criteria.getTargets().isEmpty()) {
            criteriaList.add(Criteria.where("target").in(criteria.getTargets()));
        }

        if (criteria.getFrom() > 0 && criteria.getTo() > 0) {
            criteriaList.add(Criteria.where("updatedAt").gt(new Date(criteria.getFrom())).lt(new Date(criteria.getTo())));
        } else {
            if (criteria.getFrom() > 0) {
                criteriaList.add(Criteria.where("updatedAt").gt(new Date(criteria.getFrom())));
            }
            if (criteria.getTo() > 0) {
                criteriaList.add(Criteria.where("updatedAt").lt(new Date(criteria.getTo())));
            }
        }

        if (criteria.getReferenceIds() != null && !criteria.getReferenceIds().isEmpty()) {
            criteriaList.add(Criteria.where("referenceId").in(criteria.getReferenceIds()));
        }

        if (criteria.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(criteria.getStatus().name()));
        }

        return criteriaList;
    }
}
