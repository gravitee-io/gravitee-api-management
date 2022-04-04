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
import com.mongodb.client.MongoClients;
import io.gravitee.repository.mongodb.common.AbstractRepositoryConfiguration;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ComponentScan("io.gravitee.repository.mongodb.management")
@EnableMongoRepositories
public class MongoTestRepositoryConfiguration extends AbstractRepositoryConfiguration {

    @Value("${environment.mongoVersion:4.4.6}")
    private String mongoVersion;

    @Inject
    private MongoDBContainer mongoDBContainer;

    @Bean(destroyMethod = "stop")
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer mongoDb = new MongoDBContainer(DockerImageName.parse("mongo:" + mongoVersion));
        mongoDb.start();
        return mongoDb;
    }

    @Override
    protected String getDatabaseName() {
        return "test";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoDBContainer.getReplicaSetUrl());
    }

    @Bean(name = "managementMongoTemplate")
    public MongoOperations mongoOperations(MongoClient mongoClient) {
        try {
            return new MongoTemplate(mongoClient, getDatabaseName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
