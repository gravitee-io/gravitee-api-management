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
import static com.mongodb.client.model.Filters.in;

import com.mongodb.client.AggregateIterable;
import io.gravitee.repository.mongodb.management.internal.model.PlanMongo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    public List<PlanMongo> findByApiInAndEnvironments(final List<String> apis, final Set<String> environments) {
        if (CollectionUtils.isEmpty(apis)) {
            return Collections.emptyList();
        }
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(match(in("api", apis)));
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
        mongoTemplate.updateFirst(query, update, PlanMongo.class);
    }

    private List<PlanMongo> getListFromAggregate(AggregateIterable<Document> aggregate) {
        ArrayList<PlanMongo> planMongos = new ArrayList<>();
        for (Document doc : aggregate) {
            planMongos.add(mongoTemplate.getConverter().read(PlanMongo.class, doc));
        }
        return planMongos;
    }
}
