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
package io.gravitee.repository.mongodb.management.internal.plan;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.mongodb.management.internal.model.PlanMongo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanMongoRepositoryImpl implements PlanMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<PlanMongo> findByReferenceIdsAndReferenceTypeAndEnvironments(
        List<String> ids,
        Plan.PlanReferenceType planReferenceType,
        Set<String> environments
    ) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(match(in("referenceId", ids)));
        pipeline.add(match(eq("referenceType", planReferenceType)));
        if (!CollectionUtils.isEmpty(environments)) {
            pipeline.add(match(in("environmentId", environments)));
        }

        AggregateIterable<Document> aggregate = mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(PlanMongo.class))
            .aggregate(pipeline);

        return getListFromAggregate(aggregate);
    }

    @Override
    public void updateOrder(String planId, int order) {
        Query query = Query.query(Criteria.where("_id").is(planId));
        Update update = new Update().set("order", order);
        mongoTemplate.updateMulti(query, update, PlanMongo.class);
    }

    @Override
    public void updateCrossIds(List<Plan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        var updates = plans
            .stream()
            .map(plan -> new UpdateOneModel<Document>(Filters.eq("_id", plan.getId()), Updates.set("crossId", plan.getCrossId())))
            .toList();

        mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(PlanMongo.class))
            .bulkWrite(updates, new BulkWriteOptions().ordered(false));
    }

    private List<PlanMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<PlanMongo> planMongos = new ArrayList<>();
        for (Document doc : aggregate) {
            planMongos.add(mongoTemplate.getConverter().read(PlanMongo.class, doc));
        }
        return planMongos;
    }
}
