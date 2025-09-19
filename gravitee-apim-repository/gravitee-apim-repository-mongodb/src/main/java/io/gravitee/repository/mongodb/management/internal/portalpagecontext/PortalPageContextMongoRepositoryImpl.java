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
package io.gravitee.repository.mongodb.management.internal.portalpagecontext;

import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageContextMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class PortalPageContextMongoRepositoryImpl implements PortalPageContextMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void updateByPageId(PortalPageContextMongo item) throws TechnicalException {
        Query query = Query.query(Criteria.where("pageId").is(item.getPageId()));
        Update update = new Update().set("contextType", item.getContextType()).set("published", item.isPublished());
        try {
            UpdateResult result = mongoTemplate.updateFirst(query, update, PortalPageContextMongo.class);
            if (result.getMatchedCount() == 0) {
                throw new TechnicalException("Failed to update portal page context, no rows affected");
            }
        } catch (Exception e) {
            throw new TechnicalException("Failed to update portal page context", e);
        }
    }
}
