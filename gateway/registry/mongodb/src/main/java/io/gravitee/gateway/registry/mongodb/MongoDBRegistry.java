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
package io.gravitee.gateway.registry.mongodb;

import com.google.common.base.Strings;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import io.gravitee.gateway.core.registry.AbstractRegistry;
import io.gravitee.gateway.registry.mongodb.converters.ApiConverter;
import io.gravitee.model.Api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * MongoDB API registry.
 * This registry is based on MongoDB datasource to provide Gateway configuration.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class MongoDBRegistry extends AbstractRegistry {

    private ApiConverter apiConverter = new ApiConverter();
    private final Properties properties = new Properties();
    private MongoDatabase mongoDatabase;

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
            mongoDatabase = mongoClient.getDatabase(database);
            readConfiguration();
        } catch (final IOException e) {
            LOGGER.error("No MongoDB configuration can be read from {}", configurationPath, e);
        }
    }

    public void writeApi(final Api api) {
        final DBObject document = apiConverter.convertTo(api);
        if (document != null) {
            mongoDatabase.getCollection("apis", DBObject.class).insertOne(document);
        }
    }

    private void readConfiguration() {
        LOGGER.info("Loading Gravitee configuration from MongoDB database '{}'", mongoDatabase.getName());
        final FindIterable<DBObject> apis = mongoDatabase.getCollection("apis", DBObject.class).find();
        apis.forEach(new Block<DBObject>() {
            @Override
            public void apply(final DBObject dbObject) {
                register(apiConverter.convertFrom(dbObject));
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
