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
import com.mongodb.client.MongoClients;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@ComponentScan(basePackages = { "io.gravitee.gamma.repository.mongodb" })
@EnableMongoRepositories(basePackages = "io.gravitee.gamma.repository.mongodb.internal.authorization")
public class MongoTestRepositoryConfiguration extends AbstractMongoClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MongoTestRepositoryConfiguration.class);

    private static final String DATABASE_NAME = "test";

    @Value("${mongoVersion:6.0}")
    private String mongoVersion;

    @Inject
    private MongoDBContainer mongoDBContainer;

    @Bean(destroyMethod = "stop")
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer mongoDb = new MongoDBContainer(DockerImageName.parse("mongo:" + mongoVersion));
        mongoDb.start();
        LOG.info("Started MongoDB test container on {}", mongoDb.getReplicaSetUrl());
        return mongoDb;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoDBContainer.getReplicaSetUrl());
    }

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }

    @Bean(name = "indexManagementReactiveMongoTemplate")
    public ReactiveMongoOperations indexReactiveMongoOperations() {
        return new ReactiveMongoTemplate(
            com.mongodb.reactivestreams.client.MongoClients.create(mongoDBContainer.getReplicaSetUrl()),
            DATABASE_NAME
        );
    }

    @Primary
    @Bean(name = "managementMongoTemplate")
    public MongoOperations managementMongoTemplate(MongoDatabaseFactory mongoDbFactory, MappingMongoConverter converter) {
        return new MongoTemplate(mongoDbFactory, converter);
    }
}
