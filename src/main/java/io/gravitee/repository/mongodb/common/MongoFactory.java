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

import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MongoFactory implements FactoryBean<Mongo> {

    private final Logger logger = LoggerFactory.getLogger(MongoFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    public MongoFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".mongodb.";
    }
    
    private MongoClientOptions.Builder builder() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        builder.writeConcern(WriteConcern.SAFE);

        Integer connectionsPerHost = readPropertyValue(propertyPrefix + "connectionsPerHost", Integer.class);
        Integer connectTimeout = readPropertyValue(propertyPrefix + "connectTimeout", Integer.class, 1000);
        Integer maxWaitTime = readPropertyValue(propertyPrefix + "maxWaitTime", Integer.class);
        Integer socketTimeout = readPropertyValue(propertyPrefix + "socketTimeout", Integer.class, 1000);
        Boolean socketKeepAlive = readPropertyValue(propertyPrefix + "socketKeepAlive", Boolean.class);
        Integer maxConnectionLifeTime = readPropertyValue(propertyPrefix + "maxConnectionLifeTime", Integer.class);
        Integer maxConnectionIdleTime = readPropertyValue(propertyPrefix + "maxConnectionIdleTime", Integer.class);

        // We do not want to wait for a server
        Integer serverSelectionTimeout = readPropertyValue(propertyPrefix + "serverSelectionTimeout", Integer.class, 1000);
        Integer minHeartbeatFrequency = readPropertyValue(propertyPrefix + "minHeartbeatFrequency", Integer.class);
        String description = readPropertyValue(propertyPrefix + "description", String.class, "gravitee.io");
        Integer heartbeatConnectTimeout = readPropertyValue(propertyPrefix + "heartbeatConnectTimeout", Integer.class, 1000);
        Integer heartbeatFrequency = readPropertyValue(propertyPrefix + "heartbeatFrequency", Integer.class);
        Integer heartbeatSocketTimeout = readPropertyValue(propertyPrefix + "heartbeatSocketTimeout", Integer.class);
        Integer localThreshold = readPropertyValue(propertyPrefix + "localThreshold", Integer.class);
        Integer minConnectionsPerHost = readPropertyValue(propertyPrefix + "minConnectionsPerHost", Integer.class);
        Boolean sslEnabled = readPropertyValue(propertyPrefix + "sslEnabled", Boolean.class);
        Integer threadsAllowedToBlockForConnectionMultiplier = readPropertyValue(propertyPrefix + "threadsAllowedToBlockForConnectionMultiplier", Integer.class);
        Boolean cursorFinalizerEnabled = readPropertyValue(propertyPrefix + "cursorFinalizerEnabled", Boolean.class);

        if (connectionsPerHost != null)
            builder.connectionsPerHost(connectionsPerHost);
        if (maxWaitTime != null)
            builder.maxWaitTime(maxWaitTime);
        if (connectTimeout != null)
            builder.connectTimeout(connectTimeout);
        if (socketTimeout != null)
            builder.socketTimeout(socketTimeout);
        if (socketKeepAlive != null)
            builder.socketKeepAlive(socketKeepAlive);
        if (maxConnectionLifeTime != null)
            builder.maxConnectionLifeTime(maxConnectionLifeTime);
        if (maxConnectionIdleTime != null)
            builder.maxConnectionIdleTime(maxConnectionIdleTime);
        if (minHeartbeatFrequency != null)
            builder.minHeartbeatFrequency(minHeartbeatFrequency);
        if (description != null)
            builder.description(description);
        if (heartbeatConnectTimeout != null)
            builder.heartbeatConnectTimeout(heartbeatConnectTimeout);
        if (heartbeatFrequency != null)
            builder.heartbeatFrequency(heartbeatFrequency);
        if (heartbeatSocketTimeout != null)
            builder.heartbeatSocketTimeout(heartbeatSocketTimeout);
        if (localThreshold != null)
            builder.localThreshold(localThreshold);
        if (minConnectionsPerHost != null)
            builder.minConnectionsPerHost(minConnectionsPerHost);
        if (sslEnabled != null)
            builder.sslEnabled(sslEnabled);
        if (threadsAllowedToBlockForConnectionMultiplier != null)
            builder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
        if (cursorFinalizerEnabled != null)
            builder.cursorFinalizerEnabled(cursorFinalizerEnabled);
        if (serverSelectionTimeout != null)
            builder.serverSelectionTimeout(serverSelectionTimeout);

        return builder;
    }

    @Override
    public Mongo getObject() throws Exception {
        MongoClientOptions.Builder builder = builder();

        // Trying to get the MongoClientURI if uri property is defined
        String uri = readPropertyValue(propertyPrefix + "uri");

        if (uri != null && ! uri.isEmpty()) {
            // The builder can be configured with default options, which may be overridden by options specified in
            // the URI string.
            return new MongoClient(
                    new MongoClientURI(uri, builder));
        } else {

            String databaseName = readPropertyValue(propertyPrefix + "dbname", String.class, "gravitee");

            String username = readPropertyValue(propertyPrefix + "username");
            String password = readPropertyValue(propertyPrefix + "password");

            List<MongoCredential> credentials = null;
            if (username != null || password != null) {
                credentials = Collections.singletonList(MongoCredential.createMongoCRCredential(
                        username, databaseName, password.toCharArray()));
            }


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

            MongoClientOptions options = builder.build();
            if (credentials == null) {
                return new MongoClient(seeds, options);
            }

            return new MongoClient(seeds,
                    credentials, options);
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

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }

    @Override
    public Class<?> getObjectType() {
        return Mongo.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
