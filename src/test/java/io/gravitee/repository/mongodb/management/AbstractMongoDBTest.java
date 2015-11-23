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
package io.gravitee.repository.mongodb.management;

import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.List;

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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestRepositoryConfiguration.class })
@ActiveProfiles("test")
public abstract class AbstractMongoDBTest {

    private static Logger LOG = LoggerFactory.getLogger(AbstractMongoDBTest.class);
    private static String DATABASE_NAME = "gravitee";

    @Autowired
    private MongodForTestsFactory factory;
    
    @Autowired
    private Mongo mongoClient;
    private MongoDatabase mongoDatabase;

    protected abstract String getTestCasesPath();

    private static final String JSON_EXTENSION = "json";
    
    @Before
    public void setup() throws Exception {
        LOG.info("Creating database '{}'...", DATABASE_NAME);

        mongoDatabase = ((MongoClient) mongoClient).getDatabase(DATABASE_NAME);
        
        final File file = new File(AbstractMongoDBTest.class.getResource(getTestCasesPath()).toURI());

        File[] collectionsDumps = file.listFiles(
                pathname -> pathname.isFile()
                        && JSON_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(pathname.toString())));
        
        importJsonFiles(collectionsDumps);
    }

    private void importJsonFiles(File[] files) throws Exception {
    	if(files != null){
    		for (File file : files) {
				importJsonFile(file);
			}
    	}
    }
    
    private void importJsonFile(File file) throws Exception {
    	
        final String collectionName = FilenameUtils.getBaseName(file.getName());
        
        LOG.info("Creating collection '{}'...", collectionName);
        
        final MongoCollection<Document> collectionToRemove = mongoDatabase.getCollection(collectionName);
        collectionToRemove.drop();
        
        final MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.drop();
        
        @SuppressWarnings("unchecked")
		final List<DBObject> dbObjects = (List<DBObject>) JSON.parse(FileUtils.readFileToString(file));
     
        for (final DBObject dbObject : dbObjects) {
            final Document document = new Document();
            for (final String key : dbObject.keySet()) {
                document.put(key, dbObject.get(key));
            }
            collection.insertOne(document);
        }	
    }
    
    //@After
    public void teardown() throws Exception {
        if (factory != null) {
            factory.shutdown();
        }
    }
}