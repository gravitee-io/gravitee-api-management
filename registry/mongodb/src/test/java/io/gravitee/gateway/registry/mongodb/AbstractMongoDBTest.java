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

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import io.gravitee.common.utils.PropertiesUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Allows to start/stop an instance of MongoDB for each tests and inject a data set provided.
 * The data set must be a json array.
 * The setup phase create a database and a collection named as the file.
 *
 * Example:
 * If the file provided path is:"/data/apis.json", for each test the setup will create a database and a collection
 * named "apis" with the file data set, execute the test, and then shut down.
 *
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(PropertiesUtils.class)
public abstract class AbstractMongoDBTest {

    private static Logger LOG = LoggerFactory.getLogger(AbstractMongoDBTest.class);
    private static String DATABASE_NAME = "gravitee-test";

    private MongodForTestsFactory factory;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    protected abstract String getJsonDataSetResourceName();

    @Before
    public void setup() throws Exception {
        mockStatic(PropertiesUtils.class);

        factory = MongodForTestsFactory.with(Version.Main.DEVELOPMENT);
        mongoClient = factory.newMongo();
        LOG.info("Creating database '{}'...", DATABASE_NAME);
        mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);

        final ServerAddress mongoAddress = mongoClient.getAddress();
        when(PropertiesUtils.getProperty(any(Properties.class), eq("gravitee.io.mongodb.host")))
                .thenReturn(mongoAddress.getHost());
        when(PropertiesUtils.getProperty(any(Properties.class), eq("gravitee.io.mongodb.database")))
                .thenReturn(mongoDatabase.getName());
        when(PropertiesUtils.getPropertyAsInteger(any(Properties.class), eq("gravitee.io.mongodb.port")))
                .thenReturn(mongoAddress.getPort());

        final File file = new File(AbstractMongoDBTest.class.getResource(getJsonDataSetResourceName()).toURI());

        final String collectionName = FilenameUtils.getBaseName(file.getName());
        LOG.info("Creating collection '{}'...", collectionName);
        final MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        final List<DBObject> dbObjects = (List<DBObject>) JSON.parse(FileUtils.readFileToString(file));
        for (final DBObject dbObject : dbObjects) {
            final Document document = new Document();
            for (final String key : dbObject.keySet()) {
                document.put(key, dbObject.get(key));
            }
            collection.insertOne(document);
        }
    }

    @After
    public void teardown() throws Exception {
        if (factory != null) {
            factory.shutdown();
        }
    }
}
