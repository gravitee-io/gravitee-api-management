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
package io.gravitee.repository.mongodb;

import com.mongodb.ReadPreference;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import io.gravitee.repository.mongodb.common.MongoFactory;
import io.gravitee.repository.mongodb.config.AbstractMongoRepositoryTest;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author Guillaume GILLON (guillaume.gillon@outlook.com)
 */
public class MongoTestConfig extends AbstractMongoRepositoryTest {

    @Autowired
    private MongoFactory factory;

    @Test
    public void shouldReadPreferenceSecondary() throws Exception {
        ReadPreference readRef = factory.getObject().getReadPreference();

        Assert.assertEquals("secondary",readRef.getName());
    }
 /*
    @Test
    public void souldFirstReadPreferenceTagsDcIndia() throws Exception {
        BsonArray value = (BsonArray)factory.getObject().getReadPreference().toDocument().get("tags");
        BsonDocument tagset = (BsonDocument) value.getValues().get(0);

        Assert.assertNotNull(value);
        Assert.assertEquals(2, tagset.size());
        Assert.assertEquals("india", tagset.get("dc").asString().getValue());
    }

    @Test
    public void souldSecondReadPreferenceTagsDcIndia() throws Exception {
        BsonArray value = (BsonArray)factory.getObject().getReadPreference().toDocument().get("tags");
        BsonDocument tagset = (BsonDocument) value.getValues().get(0);

        Assert.assertNotNull(value);
        Assert.assertEquals(2, tagset.size());
        Assert.assertEquals("prod", tagset.get("sc").asString().getValue());
    }*/
}
