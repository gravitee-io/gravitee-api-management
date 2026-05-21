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
package io.gravitee.gamma.repository.mongodb;

import com.mongodb.client.MongoClient;
import io.gravitee.gamma.repository.config.TestRepositoryInitializer;
import io.gravitee.gamma.repository.mongodb.upgrade.upgrader.common.IndexMongoUpgrader;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MongoTestRepositoryInitializer.class);

    private static final String DATABASE_NAME = "test";

    private final MongoClient mongoClient;

    @Autowired
    public MongoTestRepositoryInitializer(MongoClient mongoClient, ApplicationContext applicationContext) {
        this.mongoClient = mongoClient;
        LOG.info("Running MongoDB index upgraders");

        applicationContext
            .getBeansOfType(IndexMongoUpgrader.class)
            .values()
            .forEach(upgrader -> {
                try {
                    upgrader.upgrade();
                } catch (Exception e) {
                    LOG.error("Failed to run index upgrader: {}", upgrader.getClass().getName(), e);
                }
            });
    }

    @Override
    public void setUp() {}

    @Override
    public void tearDown() {
        LOG.debug("Deleting all documents...");
        final MongoTemplate mt = new MongoTemplate(mongoClient, DATABASE_NAME);
        mt
            .getDb()
            .listCollectionNames()
            .forEach(collection -> mt.getDb().getCollection(collection).deleteMany(new BsonDocument()));
    }
}
