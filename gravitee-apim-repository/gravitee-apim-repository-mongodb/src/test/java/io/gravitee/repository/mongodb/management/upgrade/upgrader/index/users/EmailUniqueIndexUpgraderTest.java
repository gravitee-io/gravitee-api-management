/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class EmailUniqueIndexUpgraderTest {

    private MongoCollection<Document> mockedCollection;
    private EmailUniqueIndexUpgrader upgrader;

    @BeforeEach
    void setUp() {
        mockedCollection = mock(MongoCollection.class);
        upgrader = new TestableEmailUniqueIndexUpgrader(mockedCollection);
    }

    @Test
    void testShouldCreateIndexAfterRemovingDuplicates() {
        Document user1 = new Document("_id", "1")
            .append("email", "test@example.com")
            .append("status", "ACTIVE");
        Document user2 = new Document("_id", "2")
            .append("email", "test@example.com")
            .append("status", "ACTIVE");
        Document user3 = new Document("_id", "3")
            .append("email", "unique@example.com")
            .append("status", "ARCHIVED");

        FindIterable<Document> iterable = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);

        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next()).thenReturn(user1, user2, user3);
        when(iterable.iterator()).thenReturn(cursor);

        when(mockedCollection.find()).thenReturn(iterable);

        boolean result = upgrader.upgrade();

        //verify(mockedCollection).deleteOne(user2);
        ArgumentCaptor<Bson> captor = ArgumentCaptor.forClass(Bson.class);
        verify(mockedCollection).deleteOne(captor.capture());

        Bson actualFilter = captor.getValue();
        assertTrue(
            actualFilter
                .toBsonDocument(
                    Document.class,
                    MongoClientSettings.getDefaultCodecRegistry()
                )
                .toJson()
                .contains("{\"_id\": \"2\"}")
        );
        verify(mockedCollection).createIndex(any(), any(IndexOptions.class));
        assertTrue(result);
    }

    @Test
    void testGetters() {
        assertEquals(0, upgrader.getOrder());
        assertEquals("v1", upgrader.version());
    }

    static class TestableEmailUniqueIndexUpgrader extends EmailUniqueIndexUpgrader {

        private final MongoCollection<Document> mockedCollection;

        public TestableEmailUniqueIndexUpgrader(
            MongoCollection<Document> mockedCollection
        ) {
            this.mockedCollection = mockedCollection;
        }

        @Override
        protected MongoCollection<Document> getCollection(String name) {
            return mockedCollection;
        }
    }
}
