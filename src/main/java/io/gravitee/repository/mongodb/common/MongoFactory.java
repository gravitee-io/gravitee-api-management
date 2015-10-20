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

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
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

        Integer connectionPerHost = readPropertyValue(propertyPrefix + "connectionPerHost", Integer.class);
        Integer connectTimeout = readPropertyValue(propertyPrefix + "connectTimeout", Integer.class);
        Integer maxWaitTime = readPropertyValue(propertyPrefix + "maxWaitTime", Integer.class);
        Integer socketTimeout = readPropertyValue(propertyPrefix + "socketTimeout", Integer.class);
        Boolean socketKeepAlive = readPropertyValue(propertyPrefix + "socketKeepAlive", Boolean.class);
        Integer maxConnectionLifeTime = readPropertyValue(propertyPrefix + "maxConnectionLifeTime", Integer.class);
        Integer maxConnectionIdleTime = readPropertyValue(propertyPrefix + "maxConnectionIdleTime", Integer.class);
        Integer minHeartbeatFrequency = readPropertyValue(propertyPrefix + "minHeartbeatFrequency", Integer.class);
        String description = readPropertyValue(propertyPrefix + "description");
        Integer heartbeatConnectTimeout = readPropertyValue(propertyPrefix + "heartbeatConnectTimeout", Integer.class);
        Integer heartbeatFrequency = readPropertyValue(propertyPrefix + "heartbeatFrequency", Integer.class);
        Integer heartbeatSocketTimeout = readPropertyValue(propertyPrefix + "heartbeatSocketTimeout", Integer.class);
        Integer localThreshold = readPropertyValue(propertyPrefix + "localThreshold", Integer.class);
        Integer minConnectionsPerHost = readPropertyValue(propertyPrefix + "minConnectionsPerHost", Integer.class);
        Boolean sslEnabled = readPropertyValue(propertyPrefix + "sslEnabled", Boolean.class);
        Integer threadsAllowedToBlockForConnectionMultiplier = readPropertyValue(propertyPrefix + "threadsAllowedToBlockForConnectionMultiplier", Integer.class);
        Boolean cursorFinalizerEnabled = readPropertyValue(propertyPrefix + "cursorFinalizerEnabled", Boolean.class);

        if(connectionPerHost != null)
            builder.connectionsPerHost(connectionPerHost);
        if(maxWaitTime != null)
            builder.maxWaitTime(maxWaitTime);
        if(connectTimeout != null)
            builder.connectTimeout(connectTimeout);
        if(socketTimeout != null)
            builder.socketTimeout(socketTimeout);
        if(socketKeepAlive != null)
            builder.socketKeepAlive(socketKeepAlive);
        if(maxConnectionLifeTime != null)
            builder.maxConnectionLifeTime(maxConnectionLifeTime);
        if(maxConnectionIdleTime != null)
            builder.maxConnectionIdleTime(maxConnectionIdleTime);
        if(minHeartbeatFrequency != null)
            builder.minHeartbeatFrequency(minHeartbeatFrequency);
        if(description != null)
            builder.description(description);
        if(heartbeatConnectTimeout != null)
            builder.heartbeatConnectTimeout(heartbeatConnectTimeout);
        if(heartbeatFrequency != null)
            builder.heartbeatFrequency(heartbeatFrequency);
        if(heartbeatSocketTimeout != null)
            builder.heartbeatSocketTimeout(heartbeatSocketTimeout);
        if(localThreshold != null)
            builder.localThreshold(localThreshold);
        if(minConnectionsPerHost != null)
            builder.minConnectionsPerHost(minConnectionsPerHost);
        if(sslEnabled != null)
            builder.sslEnabled(sslEnabled);
        if(threadsAllowedToBlockForConnectionMultiplier != null)
            builder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
        if(cursorFinalizerEnabled != null)
            builder.cursorFinalizerEnabled(cursorFinalizerEnabled);

        return builder;
    }

    @Override
    public Mongo getObject() throws Exception {
        String databaseName = readPropertyValue(propertyPrefix + "dbname", String.class, "gravitee");
        String host = readPropertyValue(propertyPrefix + "host", String.class, "localhost");
        int port = readPropertyValue(propertyPrefix + "port", int.class, 27017);
        String username = readPropertyValue(propertyPrefix + "username");
        String password = readPropertyValue(propertyPrefix + "password");

        List<MongoCredential> credentials = null;
        if (username != null || password != null) {
            credentials = Collections.singletonList(MongoCredential.createMongoCRCredential(
                    username, databaseName, password.toCharArray()));
        }

        MongoClientOptions options = builder().build();

        if (credentials == null) {
            return new MongoClient(Collections.singletonList(new ServerAddress(host, port)), options);
        }

        return new MongoClient(Collections.singletonList(new ServerAddress(host, port)),
                credentials, options);
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
