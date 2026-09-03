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

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.gravitee.repository.mongodb.management.internal.model.PerformanceTargetMongo;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PerformanceTargetMongoRepositoryImpl implements PerformanceTargetMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<String> removeApiId(String apiId) {
        var listingApi = query(where("apiIds").is(apiId));
        var targetIds = mongoTemplate.findDistinct(listingApi, "_id", PerformanceTargetMongo.class, String.class);
        if (!targetIds.isEmpty()) {
            mongoTemplate.updateMulti(listingApi, new Update().pull("apiIds", apiId), PerformanceTargetMongo.class);
        }
        return targetIds;
    }
}
