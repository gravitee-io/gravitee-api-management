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
package io.gravitee.repository.mongodb.management.internal.node;

import io.gravitee.repository.mongodb.management.internal.model.MonitoringMongo;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringMongoRepositoryImpl implements NodeMonitoringMongoRepositoryCustom {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<MonitoringMongo> findByTypeAndTimeFrame(String type, long from, long to) {
        final Query query = new Query();
        query.addCriteria(Criteria.where(FIELD_TYPE).is(type));

        final Criteria updatedAtCriteria = Criteria.where(FIELD_UPDATED_AT).gte(new Date(from));
        if (to > from) {
            updatedAtCriteria.lte(new Date(to));
        }

        query.addCriteria(updatedAtCriteria);

        return mongoTemplate.find(query, MonitoringMongo.class);
    }
}
