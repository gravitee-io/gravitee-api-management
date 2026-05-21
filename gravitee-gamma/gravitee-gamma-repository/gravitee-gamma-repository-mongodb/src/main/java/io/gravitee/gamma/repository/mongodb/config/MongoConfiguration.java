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
package io.gravitee.gamma.repository.mongodb.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "io.gravitee.gamma.repository.mongodb.internal")
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    @Autowired
    private Environment environment;

    @Override
    protected String getDatabaseName() {
        String uri = environment.getProperty("management.mongodb.uri");
        if (uri != null && !uri.isEmpty()) {
            String withoutCreds = uri.replaceAll("://.*@", "://");
            String path = URI.create(withoutCreds).getPath();
            if (path != null && path.length() > 1) {
                return path.substring(1);
            }
        }
        return environment.getProperty("management.mongodb.dbname", "gravitee");
    }

    @Override
    public MongoClient mongoClient() {
        String uri = environment.getProperty("management.mongodb.uri");
        if (uri != null && !uri.isEmpty()) {
            return MongoClients.create(uri);
        }
        String host = environment.getProperty("management.mongodb.host", "localhost");
        int port = environment.getProperty("management.mongodb.port", Integer.class, 27017);
        return MongoClients.create("mongodb://" + host + ":" + port);
    }

    @Bean(name = "indexManagementReactiveMongoTemplate")
    public ReactiveMongoOperations indexReactiveMongoOperations() {
        String uri = environment.getProperty("management.mongodb.uri");
        com.mongodb.reactivestreams.client.MongoClient reactiveClient;
        if (uri != null && !uri.isEmpty()) {
            reactiveClient = com.mongodb.reactivestreams.client.MongoClients.create(uri);
        } else {
            String host = environment.getProperty("management.mongodb.host", "localhost");
            int port = environment.getProperty("management.mongodb.port", Integer.class, 27017);
            reactiveClient = com.mongodb.reactivestreams.client.MongoClients.create("mongodb://" + host + ":" + port);
        }
        return new ReactiveMongoTemplate(reactiveClient, getDatabaseName());
    }
}
