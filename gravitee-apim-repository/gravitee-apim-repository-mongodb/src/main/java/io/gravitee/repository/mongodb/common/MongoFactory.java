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

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.*;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON (guillaume.gillon@outlook.com)
 */
public class MongoFactory implements FactoryBean<MongoClient> {

    private final Logger logger = LoggerFactory.getLogger(MongoFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    private MongoClient mongoClient;

    public MongoFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".mongodb.";
    }

    private SocketSettings buildSocketSettings() {
        SocketSettings.Builder socketBuilder = SocketSettings.builder();

        Integer connectTimeout = readPropertyValue(propertyPrefix + "connectTimeout", Integer.class, 1000);
        Integer socketTimeout = readPropertyValue(propertyPrefix + "socketTimeout", Integer.class, 1000);

        socketBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        socketBuilder.readTimeout(socketTimeout, TimeUnit.MILLISECONDS);

        return socketBuilder.build();
    }

    private ClusterSettings buildClusterSettings(boolean isReactive) {
        ClusterSettings.Builder clusterBuilder = ClusterSettings.builder();

        // We do not want to wait for a server
        Integer serverSelectionTimeout = readPropertyValue(propertyPrefix + "serverSelectionTimeout", Integer.class, 1000);

        clusterBuilder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS);

        List<ServerAddress> seeds;
        int serversCount = getServersCount();
        if (serversCount == 0) {
            String host = readPropertyValue(propertyPrefix + "host", String.class, "localhost");
            int port = readPropertyValue(propertyPrefix + "port", int.class, 27017);
            seeds = Collections.singletonList(new ServerAddress(host, port));
        } else {
            seeds = new ArrayList<>(serversCount);
            for (int i = 0; i < serversCount; i++) {
                seeds.add(buildServerAddress(i));
            }
        }
        clusterBuilder.hosts(seeds);

        if (!isReactive) {
            Integer localThreshold = readPropertyValue(propertyPrefix + "localThreshold", Integer.class);
            if (localThreshold != null) clusterBuilder.localThreshold(localThreshold, TimeUnit.MILLISECONDS);
        }

        return clusterBuilder.build();
    }

    private ConnectionPoolSettings buildConnectionPoolSettings(boolean isReactive) {
        ConnectionPoolSettings.Builder connectionPoolBuilder = ConnectionPoolSettings.builder();

        Integer maxWaitTime = readPropertyValue(propertyPrefix + "maxWaitTime", Integer.class);
        Integer maxConnectionLifeTime = readPropertyValue(propertyPrefix + "maxConnectionLifeTime", Integer.class);
        Integer maxConnectionIdleTime = readPropertyValue(propertyPrefix + "maxConnectionIdleTime", Integer.class);

        if (maxWaitTime != null) connectionPoolBuilder.maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS);
        if (maxConnectionLifeTime != null) connectionPoolBuilder.maxConnectionLifeTime(maxConnectionLifeTime, TimeUnit.MILLISECONDS);
        if (maxConnectionIdleTime != null) connectionPoolBuilder.maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS);
        if (!isReactive) {
            Integer connectionsPerHost = readPropertyValue(propertyPrefix + "connectionsPerHost", Integer.class);
            Integer minConnectionsPerHost = readPropertyValue(propertyPrefix + "minConnectionsPerHost", Integer.class);
            if (connectionsPerHost != null) connectionPoolBuilder.maxSize(connectionsPerHost);
            if (minConnectionsPerHost != null) connectionPoolBuilder.minSize(minConnectionsPerHost);
        }

        return connectionPoolBuilder.build();
    }

    private ServerSettings buildServerSettings() {
        ServerSettings.Builder serverBuilder = ServerSettings.builder();

        Integer heartbeatFrequency = readPropertyValue(propertyPrefix + "heartbeatFrequency", Integer.class);
        Integer minHeartbeatFrequency = readPropertyValue(propertyPrefix + "minHeartbeatFrequency", Integer.class);

        if (heartbeatFrequency != null) serverBuilder.heartbeatFrequency(heartbeatFrequency, TimeUnit.MILLISECONDS);
        if (minHeartbeatFrequency != null) serverBuilder.minHeartbeatFrequency(minHeartbeatFrequency, TimeUnit.MILLISECONDS);

        return serverBuilder.build();
    }

    private SslSettings buildSslSettings() {
        SslSettings.Builder sslBuilder = SslSettings.builder();

        boolean sslEnabled = readPropertyValue(propertyPrefix + "sslEnabled", Boolean.class, false);
        sslBuilder.enabled(sslEnabled);

        if (sslEnabled) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(getKeyManagers(), getTrustManagers(), null);
                sslBuilder.context(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IllegalStateException("Error creating the SSLContext for mongodb", e);
            }
        }

        return sslBuilder.build();
    }

    private KeyManager[] getKeyManagers() {
        String keystore = readPropertyValue(propertyPrefix + "keystore", String.class);
        String keystorePassword = readPropertyValue(propertyPrefix + "keystorePassword", String.class, "");
        String keyPassword = readPropertyValue(propertyPrefix + "keyPassword", String.class, "");

        if (keystore == null) {
            return null;
        }

        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(keystore), keystorePassword.toCharArray());
            keyManagerFactory.init(keyStore, keyPassword.toCharArray());
            return keyManagerFactory.getKeyManagers();
        } catch (Exception e) {
            throw new IllegalStateException("Error creating the keystore for mongodb", e);
        }
    }

    private TrustManager[] getTrustManagers() {
        String truststorePropertyPrefix = propertyPrefix + "truststore.";
        String truststorePath = readPropertyValue(truststorePropertyPrefix + "path", String.class);
        String truststoreType = readPropertyValue(truststorePropertyPrefix + "type", String.class);
        String truststorePassword = readPropertyValue(truststorePropertyPrefix + "password", String.class, "");

        if (truststorePath == null) {
            return null;
        }

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(truststoreType != null ? truststoreType : KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(truststorePath), truststorePassword.toCharArray());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            throw new IllegalStateException("Error creating the truststore for mongodb", e);
        }
    }

    private MongoClientSettings buildClientSettings(boolean isReactive) {
        // Base Builder
        MongoClientSettings.Builder builder = MongoClientSettings.builder();

        // https://mongodb.github.io/mongo-java-driver/3.12/javadoc/com/mongodb/MongoClientOptions.html#getDescription()
        String description = readPropertyValue(propertyPrefix + "description", String.class, "gravitee.io");
        builder.applicationName(description);

        // https://mongodb.github.io/mongo-java-driver/3.12/javadoc/com/mongodb/MongoClientOptions.html#getThreadsAllowedToBlockForConnectionMultiplier()
        // threadsAllowedToBlockForConnectionMultiplier has been deprecated in 3.12, so it is no longer available in 4.3.0
        // Integer threadsAllowedToBlockForConnectionMultiplier = readPropertyValue(propertyPrefix + "threadsAllowedToBlockForConnectionMultiplier", Integer.class)

        // Disappear from configuration in 4.x ?
        // Boolean cursorFinalizerEnabled = readPropertyValue(propertyPrefix + "cursorFinalizerEnabled", Boolean.class)

        // https://github.com/mongodb/mongo-java-driver/blob/master/driver-core/src/main/com/mongodb/MongoClientSettings.java#L807-L814
        // It not possible anymore to configure heartbeatSocketTimeout and heartbeatConnectTimeout
        // Integer heartbeatConnectTimeout = readPropertyValue(propertyPrefix + "heartbeatConnectTimeout", Integer.class, 1000)
        // Integer heartbeatSocketTimeout = readPropertyValue(propertyPrefix + "heartbeatSocketTimeout", Integer.class)

        // https://mongodb.github.io/mongo-java-driver/3.12/javadoc/com/mongodb/MongoClientOptions.html#isSocketKeepAlive()
        // socketKeepAlive has been deprecated in 3.12, so it is no longer available in 4.3.0
        // Boolean socketKeepAlive = readPropertyValue(propertyPrefix + "socketKeepAlive", Boolean.class)

        // credentials option
        String username = readPropertyValue(propertyPrefix + "username");
        String password = readPropertyValue(propertyPrefix + "password");
        MongoCredential credentials = null;
        if (username != null || password != null) {
            String authSource = readPropertyValue(propertyPrefix + "authSource", String.class, "gravitee");
            credentials = MongoCredential.createCredential(username, authSource, password.toCharArray());
            builder.credential(credentials);
        }

        if (!isReactive) {
            String readPreference = readPropertyValue(propertyPrefix + "readPreference", String.class);
            String readPreferenceTags = readPropertyValue(propertyPrefix + "readPreferenceTags", String.class);
            String writeConcern = readPropertyValue(propertyPrefix + "writeConcern", String.class, "1");
            Boolean journal = readPropertyValue(propertyPrefix + "journal", Boolean.class);
            Integer wtimeout = readPropertyValue(propertyPrefix + "wtimeout", Integer.class, 0);

            if (readPreference != null) {
                TagSet tagSet = null;
                ReadPreference readPrefObj = null;

                if (readPreferenceTags != null) {
                    tagSet = buildTagSet(readPreferenceTags);
                }

                switch (readPreference) {
                    case "nearest":
                        readPrefObj = tagSet != null ? ReadPreference.nearest(tagSet) : ReadPreference.nearest();
                        break;
                    case "primary":
                        readPrefObj = ReadPreference.primary();
                        break;
                    case "primaryPreferred":
                        readPrefObj = ReadPreference.primaryPreferred();
                        break;
                    case "secondary":
                        readPrefObj = tagSet != null ? ReadPreference.secondary(tagSet) : ReadPreference.secondary();
                        break;
                    case "secondaryPreferred":
                        readPrefObj = tagSet != null ? ReadPreference.secondaryPreferred(tagSet) : ReadPreference.secondaryPreferred();
                        break;
                }

                builder.readPreference(readPrefObj);
            }

            WriteConcern wc;
            if (StringUtils.isNumeric(writeConcern)) {
                wc = new WriteConcern(Integer.valueOf(writeConcern));
            } else {
                Assert.isTrue(writeConcern.equals("majority"), "writeConcern must be numeric or equals to 'majority'");
                wc = new WriteConcern(writeConcern);
            }
            builder.writeConcern(wc.withJournal(journal).withWTimeout(wtimeout, TimeUnit.MILLISECONDS));
        }

        SocketSettings socketSettings = buildSocketSettings();
        ClusterSettings clusterSettings = buildClusterSettings(isReactive);
        ConnectionPoolSettings connectionPoolSettings = buildConnectionPoolSettings(isReactive);
        ServerSettings serverSettings = buildServerSettings();
        SslSettings sslSettings = buildSslSettings();
        return builder
            .applyToClusterSettings(builder1 -> builder1.applySettings(clusterSettings))
            .applyToSocketSettings(builder1 -> builder1.applySettings(socketSettings))
            .applyToConnectionPoolSettings(builder1 -> builder1.applySettings(connectionPoolSettings))
            .applyToServerSettings(builder1 -> builder1.applySettings(serverSettings))
            .applyToSslSettings(builder1 -> builder1.applySettings(sslSettings))
            .build();
    }

    @Override
    public MongoClient getObject() throws Exception {
        // According to https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/FactoryBean.html#isSingleton--
        // It is the responsibility of the bean factory to ensure singleton instance.
        if (mongoClient == null) {
            // Trying to get the MongoClientURI if uri property is defined
            String uri = readPropertyValue(propertyPrefix + "uri");

            if (uri != null && !uri.isEmpty()) {
                // The builder can be configured with default options, which may be overridden by options specified in
                // the URI string.
                mongoClient = MongoClients.create(new ConnectionString(uri));
            } else {
                mongoClient = MongoClients.create(buildClientSettings(false));
            }
        }

        return mongoClient;
    }

    public com.mongodb.reactivestreams.client.MongoClient getReactiveClient() {
        // Trying to get the MongoClientURI if uri property is defined
        String uri = readPropertyValue(propertyPrefix + "uri");

        if (uri != null && !uri.isEmpty()) {
            // codec configuration for pojo mapping
            CodecRegistry pojoCodecRegistry = fromRegistries(
                com.mongodb.reactivestreams.client.MongoClients.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );
            MongoClientSettings.builder().codecRegistry(pojoCodecRegistry);

            return com.mongodb.reactivestreams.client.MongoClients.create(new ConnectionString(uri));
        } else {
            return com.mongodb.reactivestreams.client.MongoClients.create(buildClientSettings(true));
        }
    }

    private int getServersCount() {
        logger.debug("Looking for MongoDB server configuration...");

        boolean found = true;
        int idx = 0;

        while (found) {
            String serverHost = environment.getProperty(propertyPrefix + "servers[" + (idx++) + "].host");
            found = (serverHost != null);
        }

        return --idx;
    }

    private ServerAddress buildServerAddress(int idx) {
        String host = environment.getProperty(propertyPrefix + "servers[" + idx + "].host");
        int port = readPropertyValue(propertyPrefix + "servers[" + idx + "].port", int.class, 27017);

        return new ServerAddress(host, port);
    }

    private TagSet buildTagSet(String readPreferenceTags) {
        List<Tag> tags = Pattern
            .compile(",")
            .splitAsStream(readPreferenceTags)
            .map((String::trim))
            .map(
                tag -> {
                    String[] tagString = tag.split(":");
                    return new Tag(tagString[0].trim(), tagString[1].trim());
                }
            )
            .collect(Collectors.toList());

        if (tags.size() > 1) {
            return new TagSet(tags);
        } else {
            return new TagSet(tags.get(0));
        }
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value;
        if (defaultValue == null) {
            value = environment.getProperty(propertyName, propertyType);
        } else {
            value = environment.getProperty(propertyName, propertyType, defaultValue);
        }
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }

    @Override
    public Class<?> getObjectType() {
        return MongoClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
