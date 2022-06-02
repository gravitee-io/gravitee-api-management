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
package io.gravitee.repository.mongodb.management;

import com.mongodb.client.MongoClient;
import io.gravitee.repository.Scope;
import io.gravitee.repository.mongodb.common.AbstractRepositoryConfiguration;
import io.gravitee.repository.mongodb.common.MongoFactory;
import io.gravitee.repository.mongodb.management.converters.BsonUndefinedToNullReadingConverter;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan
@EnableMongoRepositories
@Profile("!test")
public class ManagementRepositoryConfiguration extends AbstractRepositoryConfiguration {

    @Autowired
    @Qualifier("managementMongo")
    private MongoFactory mongoFactory;

    @Autowired
    private MappingMongoConverter mappingMongoConverter;

    private static MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new BsonUndefinedToNullReadingConverter()));
    }

    @PostConstruct
    public void addCustomConverters() {
        mappingMongoConverter.setCustomConversions(mongoCustomConversions());
    }

    @Bean(name = "managementMongo")
    public MongoFactory mongoFactory(Environment environment) {
        return new MongoFactory(environment, Scope.MANAGEMENT.getName());
    }

    @Override
    public MongoClient mongoClient() {
        try {
            return mongoFactory.getObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean(name = "managementMongoTemplate")
    public MongoOperations mongoOperations(MongoClient mongo) {
        try {
            return new MongoTemplate(mongo, getDatabaseName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
