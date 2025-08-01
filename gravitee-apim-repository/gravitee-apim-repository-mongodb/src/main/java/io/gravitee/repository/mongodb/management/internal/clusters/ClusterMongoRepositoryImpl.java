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
package io.gravitee.repository.mongodb.management.internal.clusters;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.mongodb.management.internal.model.ClusterMongo;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class ClusterMongoRepositoryImpl implements ClusterMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ClusterMongo> search(ClusterCriteria criteria, PageRequest pageRequest) {
        final Query query = new Query();

        final long total = mongoTemplate.count(query, ClusterMongo.class);
        if (total == 0) {
            return new Page<>(List.of(), pageRequest.getPageNumber(), pageRequest.getPageSize(), 0);
        }

        query.with(pageRequest);

        final List<ClusterMongo> clusters = mongoTemplate.find(query, ClusterMongo.class);

        return new Page<>(clusters, pageRequest.getPageNumber(), clusters.size(), total);
    }
}
