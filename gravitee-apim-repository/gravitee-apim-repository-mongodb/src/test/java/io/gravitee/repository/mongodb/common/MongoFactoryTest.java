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
package io.gravitee.repository.mongodb.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

public class MongoFactoryTest {

    private static final String PROPERTY_PREFIX = "management.mongodb.";

    private MongoFactory mongoFactory;
    private MockEnvironment environment;

    @Before
    public void setUp() {
        environment = new MockEnvironment();
        mongoFactory = new MongoFactory(environment, "management");
    }

    @Test
    public void buildSocketSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        SocketSettings socketSettings = mongoFactory.buildSocketSettings();

        assertThat(socketSettings).isNotNull();
        assertThat(socketSettings.getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(1000);
        assertThat(socketSettings.getReadTimeout(TimeUnit.MILLISECONDS)).isEqualTo(1000);
    }

    @Test
    public void buildSocketSettingsShouldReturnSettingsConfiguredWithProperties() {
        environment.setProperty(PROPERTY_PREFIX + "connectTimeout", "200");
        environment.setProperty(PROPERTY_PREFIX + "socketTimeout", "300");

        SocketSettings socketSettings = mongoFactory.buildSocketSettings();

        assertThat(socketSettings).isNotNull();
        assertThat(socketSettings.getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(socketSettings.getReadTimeout(TimeUnit.MILLISECONDS)).isEqualTo(300);
    }

    @Test
    public void buildClusterSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        ClusterSettings clusterSettings = mongoFactory.buildClusterSettings(false);

        assertThat(clusterSettings).isNotNull();
        assertThat(clusterSettings.getServerSelectionTimeout(TimeUnit.MILLISECONDS)).isEqualTo(1000);
        assertThat(clusterSettings.getHosts()).isEqualTo(List.of(new ServerAddress("localhost", 27017)));
        assertThat(clusterSettings.getLocalThreshold(TimeUnit.MILLISECONDS)).isEqualTo(15);
    }

    @Test
    public void buildClusterSettingsShouldReturnSettingsConfiguredWithSingleHostProperties() {
        environment.setProperty(PROPERTY_PREFIX + "serverSelectionTimeout", "200");
        environment.setProperty(PROPERTY_PREFIX + "localThreshold", "300");
        environment.setProperty(PROPERTY_PREFIX + "host", "mongohost");
        environment.setProperty(PROPERTY_PREFIX + "port", "27117");

        ClusterSettings clusterSettings = mongoFactory.buildClusterSettings(false);

        assertThat(clusterSettings).isNotNull();
        assertThat(clusterSettings.getServerSelectionTimeout(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(clusterSettings.getHosts()).isEqualTo(List.of(new ServerAddress("mongohost", 27117)));
        assertThat(clusterSettings.getLocalThreshold(TimeUnit.MILLISECONDS)).isEqualTo(300);
    }

    @Test
    public void buildClusterSettingsShouldReturnSettingsConfiguredWithMultipleHostsProperties() {
        environment.setProperty(PROPERTY_PREFIX + "serverSelectionTimeout", "200");
        environment.setProperty(PROPERTY_PREFIX + "localThreshold", "300");
        environment.setProperty(PROPERTY_PREFIX + "servers[0].host", "mongohost1");
        environment.setProperty(PROPERTY_PREFIX + "servers[0].port", "27117");
        environment.setProperty(PROPERTY_PREFIX + "servers[1].host", "mongohost2");
        environment.setProperty(PROPERTY_PREFIX + "servers[1].port", "27118");

        ClusterSettings clusterSettings = mongoFactory.buildClusterSettings(false);

        assertThat(clusterSettings).isNotNull();
        assertThat(clusterSettings.getServerSelectionTimeout(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(clusterSettings.getHosts())
            .isEqualTo(List.of(new ServerAddress("mongohost1", 27117), new ServerAddress("mongohost2", 27118)));
        assertThat(clusterSettings.getLocalThreshold(TimeUnit.MILLISECONDS)).isEqualTo(300);
    }

    @Test
    public void buildClusterSettingsShouldSkipLocalThresholdInReactiveMode() {
        environment.setProperty(PROPERTY_PREFIX + "serverSelectionTimeout", "200");
        environment.setProperty(PROPERTY_PREFIX + "localThreshold", "300");

        ClusterSettings clusterSettings = mongoFactory.buildClusterSettings(true);

        assertThat(clusterSettings).isNotNull();
        assertThat(clusterSettings.getServerSelectionTimeout(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(clusterSettings.getHosts()).isEqualTo(List.of(new ServerAddress("localhost", 27017)));
        assertThat(clusterSettings.getLocalThreshold(TimeUnit.MILLISECONDS)).isEqualTo(15);
    }

    @Test
    public void buildConnectionPoolSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        ConnectionPoolSettings connectionPoolSettings = mongoFactory.buildConnectionPoolSettings(false);

        assertThat(connectionPoolSettings).isNotNull();
        assertThat(connectionPoolSettings.getMaxWaitTime(TimeUnit.MILLISECONDS)).isEqualTo(120000);
        assertThat(connectionPoolSettings.getMaxConnectionLifeTime(TimeUnit.MILLISECONDS)).isZero();
        assertThat(connectionPoolSettings.getMaxConnectionIdleTime(TimeUnit.MILLISECONDS)).isZero();
        assertThat(connectionPoolSettings.getMinSize()).isZero();
        assertThat(connectionPoolSettings.getMaxSize()).isEqualTo(100);
    }

    @Test
    public void buildConnectionPoolSettingsShouldReturnSettingsConfiguredWithProperties() {
        environment.setProperty(PROPERTY_PREFIX + "maxWaitTime", "200");
        environment.setProperty(PROPERTY_PREFIX + "maxConnectionLifeTime", "300");
        environment.setProperty(PROPERTY_PREFIX + "maxConnectionIdleTime", "400");
        environment.setProperty(PROPERTY_PREFIX + "minConnectionsPerHost", "500");
        environment.setProperty(PROPERTY_PREFIX + "connectionsPerHost", "600");

        ConnectionPoolSettings connectionPoolSettings = mongoFactory.buildConnectionPoolSettings(false);

        assertThat(connectionPoolSettings).isNotNull();
        assertThat(connectionPoolSettings.getMaxWaitTime(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(connectionPoolSettings.getMaxConnectionLifeTime(TimeUnit.MILLISECONDS)).isEqualTo(300);
        assertThat(connectionPoolSettings.getMaxConnectionIdleTime(TimeUnit.MILLISECONDS)).isEqualTo(400);
        assertThat(connectionPoolSettings.getMinSize()).isEqualTo(500);
        assertThat(connectionPoolSettings.getMaxSize()).isEqualTo(600);
    }

    @Test
    public void buildConnectionPoolSettingsShouldSkipConnectionsPerHostInReactiveMode() {
        environment.setProperty(PROPERTY_PREFIX + "maxWaitTime", "200");
        environment.setProperty(PROPERTY_PREFIX + "maxConnectionLifeTime", "300");
        environment.setProperty(PROPERTY_PREFIX + "maxConnectionIdleTime", "400");
        environment.setProperty(PROPERTY_PREFIX + "minConnectionsPerHost", "500");
        environment.setProperty(PROPERTY_PREFIX + "connectionsPerHost", "600");

        ConnectionPoolSettings connectionPoolSettings = mongoFactory.buildConnectionPoolSettings(true);

        assertThat(connectionPoolSettings).isNotNull();
        assertThat(connectionPoolSettings.getMaxWaitTime(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(connectionPoolSettings.getMaxConnectionLifeTime(TimeUnit.MILLISECONDS)).isEqualTo(300);
        assertThat(connectionPoolSettings.getMaxConnectionIdleTime(TimeUnit.MILLISECONDS)).isEqualTo(400);
        assertThat(connectionPoolSettings.getMinSize()).isZero();
        assertThat(connectionPoolSettings.getMaxSize()).isEqualTo(100);
    }

    @Test
    public void buildServerSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        ServerSettings serverSettings = mongoFactory.buildServerSettings();

        assertThat(serverSettings).isNotNull();
        assertThat(serverSettings.getHeartbeatFrequency(TimeUnit.MILLISECONDS)).isEqualTo(10000);
        assertThat(serverSettings.getMinHeartbeatFrequency(TimeUnit.MILLISECONDS)).isEqualTo(500);
    }

    @Test
    public void buildServerSettingsShouldReturnSettingsConfiguredWithProperties() {
        environment.setProperty(PROPERTY_PREFIX + "heartbeatFrequency", "200");
        environment.setProperty(PROPERTY_PREFIX + "minHeartbeatFrequency", "100");

        ServerSettings serverSettings = mongoFactory.buildServerSettings();

        assertThat(serverSettings).isNotNull();
        assertThat(serverSettings.getHeartbeatFrequency(TimeUnit.MILLISECONDS)).isEqualTo(200);
        assertThat(serverSettings.getMinHeartbeatFrequency(TimeUnit.MILLISECONDS)).isEqualTo(100);
    }

    @Test
    public void buildSslSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        SslSettings sslSettings = mongoFactory.buildSslSettings();

        assertThat(sslSettings).isNotNull();
        assertThat(sslSettings.isEnabled()).isFalse();
    }

    @Test
    public void buildSslSettingsShouldReturnSettingsConfiguredWithProperties() {
        environment.setProperty(PROPERTY_PREFIX + "sslEnabled", "true");
        environment.setProperty(PROPERTY_PREFIX + "truststore.path", this.getClass().getResource("/ca-truststore.jks").getPath());
        environment.setProperty(PROPERTY_PREFIX + "truststore.password", "truststore-secret");
        environment.setProperty(PROPERTY_PREFIX + "truststore.type", "jks");

        environment.setProperty(PROPERTY_PREFIX + "keystore.path", this.getClass().getResource("/keystore.jks").getPath());
        environment.setProperty(PROPERTY_PREFIX + "keystore.password", "keystore-secret");
        environment.setProperty(PROPERTY_PREFIX + "keystore.type", "jks");

        SslSettings sslSettings = mongoFactory.buildSslSettings();

        assertThat(sslSettings).isNotNull();
        assertThat(sslSettings.isEnabled()).isTrue();
        assertThat(sslSettings.getContext().getProtocol()).isEqualTo("TLS");
    }

    @Test
    public void buildClientSettingsShouldReturnSettingsConfiguredWithDefaultValues() {
        MongoClientSettings mongoClientSettings = mongoFactory.buildClientSettings(false);

        assertThat(mongoClientSettings).isNotNull();
        assertThat(mongoClientSettings.getApplicationName()).isEqualTo("gravitee.io");
        assertThat(mongoClientSettings.getCredential()).isNull();
        assertThat(mongoClientSettings.getRetryWrites()).isTrue();
        assertThat(mongoClientSettings.getReadPreference()).isEqualTo(ReadPreference.primary());
        assertThat(mongoClientSettings.getWriteConcern()).isEqualTo(WriteConcern.W1.withWTimeout(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void buildClientSettingsShouldReturnSettingsConfiguredWithProperties() {
        environment.setProperty(PROPERTY_PREFIX + "description", "my-application");
        environment.setProperty(PROPERTY_PREFIX + "username", "username");
        environment.setProperty(PROPERTY_PREFIX + "password", "password");
        environment.setProperty(PROPERTY_PREFIX + "authSource", "authSource");
        environment.setProperty(PROPERTY_PREFIX + "retryWrites", "false");
        environment.setProperty(PROPERTY_PREFIX + "readPreference", "secondaryPreferred");
        environment.setProperty(PROPERTY_PREFIX + "readPreferenceTags", "tag1:value1,tag2:value2");
        environment.setProperty(PROPERTY_PREFIX + "writeConcern", "majority");
        environment.setProperty(PROPERTY_PREFIX + "journal", "true");
        environment.setProperty(PROPERTY_PREFIX + "wtimeout", "1000");

        MongoClientSettings mongoClientSettings = mongoFactory.buildClientSettings(false);

        assertThat(mongoClientSettings).isNotNull();
        assertThat(mongoClientSettings.getApplicationName()).isEqualTo("my-application");
        assertThat(mongoClientSettings.getCredential())
            .isEqualTo(MongoCredential.createCredential("username", "authSource", "password".toCharArray()));
        assertThat(mongoClientSettings.getRetryWrites()).isFalse();
        assertThat(mongoClientSettings.getReadPreference())
            .isEqualTo(ReadPreference.secondaryPreferred(new TagSet(List.of(new Tag("tag1", "value1"), new Tag("tag2", "value2")))));
        assertThat(mongoClientSettings.getWriteConcern())
            .isEqualTo(WriteConcern.MAJORITY.withWTimeout(1000, TimeUnit.MILLISECONDS).withJournal(true));
    }

    @Test
    public void buildClientSettingsShouldSkipSomePropertiesInReactiveMode() {
        environment.setProperty(PROPERTY_PREFIX + "description", "my-application");
        environment.setProperty(PROPERTY_PREFIX + "username", "username");
        environment.setProperty(PROPERTY_PREFIX + "password", "password");
        environment.setProperty(PROPERTY_PREFIX + "authSource", "authSource");
        environment.setProperty(PROPERTY_PREFIX + "retryWrites", "false");
        environment.setProperty(PROPERTY_PREFIX + "readPreference", "secondaryPreferred");
        environment.setProperty(PROPERTY_PREFIX + "readPreferenceTags", "tag1:value1,tag2:value2");
        environment.setProperty(PROPERTY_PREFIX + "writeConcern", "majority");
        environment.setProperty(PROPERTY_PREFIX + "journal", "true");
        environment.setProperty(PROPERTY_PREFIX + "wtimeout", "1000");

        MongoClientSettings mongoClientSettings = mongoFactory.buildClientSettings(true);

        assertThat(mongoClientSettings).isNotNull();
        assertThat(mongoClientSettings.getApplicationName()).isEqualTo("my-application");
        assertThat(mongoClientSettings.getCredential())
            .isEqualTo(MongoCredential.createCredential("username", "authSource", "password".toCharArray()));
        assertThat(mongoClientSettings.getRetryWrites()).isFalse();

        // These are not supported in reactive mode and so fallback to default values
        assertThat(mongoClientSettings.getReadPreference()).isEqualTo(ReadPreference.primary());
        assertThat(mongoClientSettings.getWriteConcern()).isEqualTo(WriteConcern.ACKNOWLEDGED);
    }

    @Test
    public void getObjectShouldReturnAMongoClientConfiguredWithURI() throws Exception {
        environment.setProperty(PROPERTY_PREFIX + "uri", "mongodb://localhost:27017/gravitee?connectTimeoutMS=2000");

        MongoClient mongoClient = mongoFactory.getObject();

        assertThat(mongoClient).isNotNull();
        assertThat(mongoClient.getClusterDescription().getClusterSettings().getHosts())
            .isEqualTo(List.of(new ServerAddress("localhost", 27017)));
    }

    @Test
    public void getObjectShouldReturnAMongoClientConfiguredWithProperties() throws Exception {
        MongoFactory spiedMongoFactory = spy(mongoFactory);

        spiedMongoFactory.getObject();

        verify(spiedMongoFactory).buildClientSettings(false);
    }

    @Test
    public void getReactiveClientShouldReturnAMongoClientConfiguredWithURI() {
        environment.setProperty(PROPERTY_PREFIX + "uri", "mongodb://localhost:27017/gravitee?connectTimeoutMS=2000");

        com.mongodb.reactivestreams.client.MongoClient mongoClient = mongoFactory.getReactiveClient();

        assertThat(mongoClient).isNotNull();
        assertThat(mongoClient.getClusterDescription().getClusterSettings().getHosts())
            .isEqualTo(List.of(new ServerAddress("localhost", 27017)));
    }

    @Test
    public void getReactiveClientShouldReturnAMongoClientConfiguredWithProperties() {
        MongoFactory spiedMongoFactory = spy(mongoFactory);

        spiedMongoFactory.getReactiveClient();

        verify(spiedMongoFactory).buildClientSettings(true);
    }

    @Test
    public void isSingletonShouldReturnTrue() {
        assertThat(mongoFactory.isSingleton()).isTrue();
    }
}
