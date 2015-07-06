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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import io.gravitee.common.utils.PropertiesUtils;
import io.gravitee.gateway.core.repository.AbstractRepository;
import io.gravitee.gateway.registry.mongodb.converters.ApiConverter;
import io.gravitee.model.Api;

/**
 * MongoDB API repository.
 * This repository is based on MongoDB datasource to provide Gateway configuration.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class MongoDBRepository extends AbstractRepository {

    private ApiConverter apiConverter = new ApiConverter();
    private final Properties properties = new Properties();
    private MongoDatabase mongoDatabase;

    public MongoDBRepository() {
        this(System.getProperty("gateway.conf.mongodb", "/etc/gravitee.io/conf/mongodb.properties"));
    }

    public MongoDBRepository(final String configurationPath) {
        try {
            final InputStream input = new FileInputStream(configurationPath);
            properties.load(input);
            final String host = PropertiesUtils.getProperty(properties, "gravitee.io.mongodb.host");
            final int port = PropertiesUtils.getPropertyAsInteger(properties, "gravitee.io.mongodb.port");
            final String database = PropertiesUtils.getProperty(properties, "gravitee.io.mongodb.database");
            final MongoClient mongoClient = new MongoClient(host, port);
            mongoDatabase = mongoClient.getDatabase(database);
            readConfiguration();
        } catch (final IOException e) {
            LOGGER.error("No MongoDB configuration can be read from {}", configurationPath, e);
        }
    }

    @Override
    public boolean create(final Api api) {
        final DBObject document = apiConverter.convertTo(api);
        if (document != null) {
            mongoDatabase.getCollection("apis", DBObject.class).insertOne(document);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(final Api api) {
        final DBObject document = apiConverter.convertTo(api);
        if (document != null) {
            mongoDatabase.getCollection("apis", DBObject.class).replaceOne(new BasicDBObject("description", api.getName()), document);
            return true;
        }
        return false;
    }

    @Override
    public Api fetch(final String name) {
        final DBObject api = mongoDatabase.getCollection("apis", DBObject.class).find(new Document("description", name)).first();
        return apiConverter.convertFrom(api);
    }

    @Override
    public Set<Api> fetchAll() {
        final FindIterable<DBObject> apis = mongoDatabase.getCollection("apis", DBObject.class).find();
        final Set<Api> list = new HashSet<>();
        apis.forEach((Consumer<DBObject>) api -> list.add(apiConverter.convertFrom(api)));
        return list;
    }

    private void readConfiguration() {
        LOGGER.info("Loading Gravitee configuration from MongoDB database '{}'", mongoDatabase.getName());
        final FindIterable<DBObject> apis = mongoDatabase.getCollection("apis", DBObject.class).find();
        apis.forEach((Consumer<DBObject>) dbObject -> register(apiConverter.convertFrom(dbObject)));
        LOGGER.info("{} API(s) registered", listAll().size());
    }
}
