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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index;

import com.mongodb.MongoCommandException;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public abstract class IndexUpgrader extends MongoUpgrader {

    private static final Logger LOG = LoggerFactory.getLogger(IndexUpgrader.class);

    protected abstract Index buildIndex();

    protected static BsonValue ascending() {
        return new BsonInt32(1);
    }

    protected static BsonValue descending() {
        return new BsonInt32(-1);
    }

    protected static BsonValue text(String fieldName) {
        return new BsonDocument(fieldName, new BsonString("text"));
    }

    @Override
    public boolean upgrade() {
        Index index = buildIndex();
        BsonDocument bson = index.bson();
        IndexOptions options = index.options();
        String collection = buildCollectionName(index.getCollection());
        String name = options.getName();
        try {
            LOG.info("creating index {} on collection {}", name, collection);
            template.getCollection(collection).createIndex(bson, options);
            LOG.debug("index {} has been created: {}", name, bson.toJson());
        } catch (MongoCommandException e) {
            String message = "unable to create index {} on collection {}: {}";
            LOG.warn(message, name, collection, e.getMessage());
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
