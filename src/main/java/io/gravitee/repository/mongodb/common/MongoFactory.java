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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class MongoFactory implements FactoryBean<Mongo> {

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    public MongoFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".mongodb.";
    }
    
    private MongoClientOptions.Builder builder() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        builder.writeConcern(WriteConcern.SAFE);

        Integer connectionPerHost = environment.getProperty(propertyPrefix + "connectionPerHost", Integer.class);
        Integer connectTimeout = environment.getProperty(propertyPrefix + "connectTimeout", Integer.class);
        Integer maxWaitTime = environment.getProperty(propertyPrefix + "maxWaitTime", Integer.class);
        Integer socketTimeout = environment.getProperty(propertyPrefix + "socketTimeout", Integer.class);
        Boolean socketKeepAlive = environment.getProperty(propertyPrefix + "socketKeepAlive", Boolean.class);
        Integer maxConnectionLifeTime = environment.getProperty(propertyPrefix + "maxConnectionLifeTime", Integer.class);
        Integer maxConnectionIdleTime = environment.getProperty(propertyPrefix + "maxConnectionIdleTime", Integer.class);
        Integer minHeartbeatFrequency = environment.getProperty(propertyPrefix + "minHeartbeatFrequency", Integer.class);
        String description = environment.getProperty(propertyPrefix + "description");
        Integer heartbeatConnectTimeout = environment.getProperty(propertyPrefix + "heartbeatConnectTimeout", Integer.class);
        Integer heartbeatFrequency = environment.getProperty(propertyPrefix + "heartbeatFrequency", Integer.class);
        Integer heartbeatSocketTimeout = environment.getProperty(propertyPrefix + "heartbeatSocketTimeout", Integer.class);
        Integer localThreshold = environment.getProperty(propertyPrefix + "localThreshold", Integer.class);
        Integer minConnectionsPerHost = environment.getProperty(propertyPrefix + "minConnectionsPerHost", Integer.class);
        Boolean sslEnabled = environment.getProperty(propertyPrefix + "sslEnabled", Boolean.class);
        Integer threadsAllowedToBlockForConnectionMultiplier = environment.getProperty(propertyPrefix + "threadsAllowedToBlockForConnectionMultiplier", Integer.class);
        Boolean cursorFinalizerEnabled = environment.getProperty(propertyPrefix + "cursorFinalizerEnabled", Boolean.class);

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
        String databaseName = environment.getProperty(propertyPrefix + "dbname", "gravitee");
        String host = environment.getProperty(propertyPrefix + "host", "localhost");
        int port = environment.getProperty(propertyPrefix + "port", int.class, 27017);
        String username = environment.getProperty(propertyPrefix + "username");
        String password = environment.getProperty(propertyPrefix + "password");

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

    @Override
    public Class<?> getObjectType() {
        return Mongo.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
