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
package io.gravitee.repository.mongodb;

import com.mongodb.client.MongoClient;
import io.gravitee.repository.config.TestRepositoryInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MongoTestRepositoryInitializer.class);

    private final MongoClient mongoClient;

    @Autowired
    public MongoTestRepositoryInitializer(MongoClient mongoClient) {
        LOG.debug("Constructed");
        this.mongoClient = mongoClient;
        final MongoTemplate mt = new MongoTemplate(mongoClient, "test");
        LOG.debug("Dropping database...");
        mt.getDb().drop();
    }

    @Override
    public void setUp() {}

    @Override
    public void tearDown() {
        LOG.debug("Dropping database...");
        final MongoTemplate mt = new MongoTemplate(mongoClient, "test");
        mt.getDb().drop();
    }
}
