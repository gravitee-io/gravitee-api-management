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
package io.gravitee.gateway.core.impl;

import com.google.common.base.Strings;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import io.gravitee.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * MongoDB API registry.
 * This registry is based on MongoDB datasource to provide Gateway configuration.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class MongoDBRegistry extends AbstractRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBRegistry.class);

    private final Properties properties = new Properties();

    public MongoDBRegistry() {
        this(System.getProperty("gateway.conf.mongodb", "/etc/gravitee.io/conf/mongodb.properties"));
    }

    public MongoDBRegistry(final String configurationPath) {
        try {
            final InputStream input = new FileInputStream(configurationPath);
            properties.load(input);
            final String host = getProperty("host");
            final int port = getPropertyAsInteger("port");
            final String database = getProperty("database");
            final MongoClient mongoClient = new MongoClient(host, port);
            readConfiguration(mongoClient.getDatabase(database));
        } catch (final IOException e) {
            LOGGER.error("No MongoDB configuration can be read from {}", configurationPath, e);
        }
    }

    private void readConfiguration(final MongoDatabase db) {
        LOGGER.info("Loading Gravitee configuration from MongoDB database '{}'", db.getName());
        final FindIterable<DBObject> apis = db.getCollection("apis", DBObject.class).find();
        apis.forEach(new Consumer<DBObject>() {
            @Override
            public void accept(final DBObject api) {
                final Api newAPI = new Api();
                final Object name = api.get("name");
                if (name != null) {
                    newAPI.setName((String) name);
                }
                final Object version = api.get("version");
                if (version != null) {
                    newAPI.setVersion((String) version);
                }
                final Object contextPath = api.get("contextPath");
                if (contextPath != null) {
                    newAPI.setContextPath((String) contextPath);
                }
                final Object enabled = api.get("enabled");
                if (enabled != null) {
                    newAPI.setEnabled((Boolean) enabled);
                }
                register(newAPI);
            }
        });
        LOGGER.info("{} API(s) registered", listAll().size());
    }

    private String getProperty(final String propertyName) {
        return (String) getProperty(propertyName, false);
    }

    private Integer getPropertyAsInteger(final String propertyName) {
        return (Integer) getProperty(propertyName, true);
    }

    private Object getProperty(final String propertyName, final boolean isNumber) {
        final String propertyValue = properties.getProperty(propertyName);
        if (Strings.isNullOrEmpty(propertyValue)) {
            LOGGER.error("Missing property configuration:{}", propertyName);
            throw new IllegalStateException("Missing MongoDB configuration properties");
        }
        try {
            if (isNumber) {
                return Integer.parseInt(propertyValue);
            }
            return propertyValue;
        } catch (final NumberFormatException e) {
            LOGGER.error("The property configuration {} must be a valid number", propertyName);
            throw new IllegalArgumentException("Wrong MongoDB configuration properties", e);
        }
    }
}
