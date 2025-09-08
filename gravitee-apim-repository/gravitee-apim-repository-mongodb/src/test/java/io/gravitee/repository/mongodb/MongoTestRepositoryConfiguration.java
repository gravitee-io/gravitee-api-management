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
package io.gravitee.repository.mongodb;

import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import io.gravitee.repository.mongodb.common.AbstractRepositoryConfiguration;
import io.gravitee.repository.mongodb.common.MongoFactory;
import io.gravitee.repository.mongodb.encryption.EncryptionEnabledCondition;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.config.MongoUpgraderConfiguration;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ComponentScan(
    basePackages = { "io.gravitee.repository.mongodb" },
    excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MongoUpgraderConfiguration.class) }
)
@Import(EncryptionEnabledCondition.class)
@EnableMongoRepositories
public class MongoTestRepositoryConfiguration extends AbstractRepositoryConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MongoTestRepositoryConfiguration.class);

    @Value("${mongoVersion:6.0}")
    private String mongoVersion;

    @Inject
    private MongoDBContainer mongoDBContainer;

    public MongoTestRepositoryConfiguration(ConfigurableEnvironment environment, ApplicationContext applicationContext) {
        super(environment, applicationContext);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("graviteeTest", graviteeProperties()));
    }

    public static Properties graviteeProperties() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        final Resource yamlResource = new ClassPathResource("graviteeTest.yml");
        yaml.setResources(yamlResource);
        return yaml.getObject();
    }

    @Bean(destroyMethod = "stop")
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer mongoDb = new MongoDBContainer(DockerImageName.parse("mongo:" + mongoVersion));
        mongoDb.withCommand("--replSet", "docker-rs", "--setParameter", "notablescan=true").start();

        LOG.info("Running tests with MongoDB version: {}", getMongoFullVersion(mongoDb.getContainerInfo().getConfig().getEnv()));

        return mongoDb;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoDBContainer.getReplicaSetUrl());
    }

    @Bean
    public com.mongodb.reactivestreams.client.MongoClient reactiveMongoClient() {
        return com.mongodb.reactivestreams.client.MongoClients.create(mongoDBContainer.getReplicaSetUrl());
    }

    @Bean(name = "managementMongoTemplate")
    public MongoOperations mongoOperations(MongoClient mongoClient) {
        try {
            return new MongoTemplate(mongoClient, getDatabaseName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public ReactiveMongoOperations rateLimitMongoTemplate(com.mongodb.reactivestreams.client.MongoClient mongoClient) {
        try {
            return new ReactiveMongoTemplate(mongoClient, getDatabaseName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getMongoFullVersion(String[] containerEnvs) {
        return Arrays
            .stream(containerEnvs)
            .filter(env -> env.startsWith("MONGO_VERSION="))
            .findFirst()
            .map(env -> env.split("=")[1])
            .orElse("");
    }

    @Bean
    @Conditional(EncryptionEnabledCondition.class)
    public ClientEncryption clientEncryption(MongoFactory mongoFactory) {
        // Key Management System (KMS) providers.
        Map<String, Object> localKmsProvider = new HashMap<>();
        localKmsProvider.put("key", environment.getProperty("management.mongodb.encryption.kms[0].local.key"));
        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        kmsProviders.put("local", localKmsProvider);

        String collectionName = environment.getProperty("management.mongodb.encryption.keyVault.collectionName");
        ClientEncryptionSettings.Builder builder = ClientEncryptionSettings
            .builder()
            // The collection in MongoDB where the Data Encryption Keys (DEKs) will be stored.
            .keyVaultNamespace("test.test_prefix_" + collectionName)
            .keyVaultMongoClientSettings(
                MongoClientSettings.builder().applyConnectionString(new ConnectionString(mongoDBContainer.getReplicaSetUrl())).build()
            )
            .kmsProviders(kmsProviders);

        ClientEncryption clientEncryption = ClientEncryptions.create(builder.build());

        // Initialize the Data Encryption Key if not already existing.
        String keyAlternativeName = environment.getProperty("management.mongodb.encryption.keyVault.keyAlternativeName");

        BsonDocument keyByAltName = clientEncryption.getKeyByAltName(keyAlternativeName);
        if (keyByAltName == null) {
            clientEncryption.createDataKey("local", new DataKeyOptions().keyAltNames(List.of(keyAlternativeName)));
        }

        return clientEncryption;
    }
}
