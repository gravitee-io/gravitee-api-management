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
package io.gravitee.repository.mongodb.management.internal.portalpage;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.repository.management.model.ExpandsViewContext;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class PortalPageMongoRepositoryImpl implements PortalPageMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<PortalPageMongo> findPortalPagesByIdWithExpand(List<String> ids, List<ExpandsViewContext> expands) {
        Query query = new Query(where("_id").in(ids));

        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (expands != null) {
            expands.forEach(e -> selected.add(e.getValue()));
        }
        query.fields().include("_id");
        for (String field : selected) {
            query.fields().include(field);
        }
        return mongoTemplate.find(query, PortalPageMongo.class);
    }
}
