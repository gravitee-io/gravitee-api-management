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
package io.gravitee.repository.mongodb.management.internal.kafkaportranges;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.repository.mongodb.management.internal.model.KafkaPortRangeMongo;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class KafkaPortRangeMongoRepositoryImpl implements KafkaPortRangeMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<KafkaPortRangeMongo> findConflicting(
        String environmentId,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) {
        // The four conflict conditions (see KafkaPortRangeRepository javadoc), expressed as an $or
        // of Mongo criteria. Scoping to environmentId is added as a separate $and term.
        final Criteria brokerRangeOverlap = where("rangeStart").lte(rangeEnd).and("rangeEnd").gte(rangeStart);
        final Criteria newBootstrapInsideExistingRange = where("rangeStart").lte(bootstrapPort).and("rangeEnd").gte(bootstrapPort);
        final Criteria existingBootstrapInsideNewRange = where("bootstrapPort").gte(rangeStart).lte(rangeEnd);
        final Criteria bootstrapPortCollision = where("bootstrapPort").is(bootstrapPort);

        final Criteria conflict = new Criteria().orOperator(
            brokerRangeOverlap,
            newBootstrapInsideExistingRange,
            existingBootstrapInsideNewRange,
            bootstrapPortCollision
        );

        final Query query = new Query().addCriteria(where("environmentId").is(environmentId)).addCriteria(conflict);

        if (excludePlanId != null) {
            query.addCriteria(where("_id").ne(excludePlanId));
        }

        return mongoTemplate.find(query, KafkaPortRangeMongo.class);
    }
}
