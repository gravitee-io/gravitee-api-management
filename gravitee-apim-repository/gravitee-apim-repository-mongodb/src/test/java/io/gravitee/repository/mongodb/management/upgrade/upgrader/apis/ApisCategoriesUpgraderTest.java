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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.apis;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.apis.ApisCategoriesUpgrader.APIS_CATEGORY_VERSION_UPGRADER_ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApisCategoriesUpgraderTest {

    @InjectMocks
    private ApisCategoriesUpgrader apisCategoriesUpgrader;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MongoCollection<Document> apisCollection;

    @Mock
    private MongoCollection<Document> categoriesCollection;

    @Mock
    private FindIterable<Document> apisFindIterable;

    @Mock
    private FindIterable<Document> categoriesFindIterable;

    @Mock
    private MongoCursor<Document> apisCursor;

    @Mock
    private MongoCursor<Document> categoriesCursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mongoTemplate.getCollection("apis")).thenReturn(apisCollection);
        when(mongoTemplate.getCollection("categories")).thenReturn(categoriesCollection);

        when(apisFindIterable.projection(any())).thenReturn(apisFindIterable);
        when(apisFindIterable.batchSize(eq(100))).thenReturn(apisFindIterable);
    }

    @Test
    public void testUpgradeSuccess() {
        when(categoriesCollection.find()).thenReturn(categoriesFindIterable);

        List<Document> apis = new ArrayList<>();
        apis.add(new Document("_id", "apiId1").append("categories", List.of("key1", "key3")));
        apis.add(new Document("_id", "apiId2").append("categories", List.of("key2")));
        when(apisCollection.find()).thenReturn(apisFindIterable);
        when(apisFindIterable.iterator()).thenReturn(apisCursor);
        when(apisCursor.hasNext()).thenReturn(true, true, false);
        when(apisCursor.next()).thenReturn(apis.get(0), apis.get(1));

        boolean result = apisCategoriesUpgrader.upgrade();

        assertTrue(result);
        verify(apisCollection, times(1)).bulkWrite(anyList());
    }

    @Test
    public void testUpgradeFailureDuringBulkWrite() {
        when(categoriesCollection.find()).thenReturn(categoriesFindIterable);

        List<Document> apis = new ArrayList<>();
        apis.add(new Document("_id", "apiId1").append("categories", List.of("key1")));
        when(apisCollection.find()).thenReturn(apisFindIterable);
        when(apisFindIterable.iterator()).thenReturn(apisCursor);
        when(apisCursor.hasNext()).thenReturn(true, false);
        when(apisCursor.next()).thenReturn(apis.get(0));

        doThrow(new RuntimeException("Bulk write error")).when(apisCollection).bulkWrite(anyList());

        boolean result = apisCategoriesUpgrader.upgrade();

        assertFalse(result);
        verify(apisCollection, times(1)).bulkWrite(anyList());
    }

    @Test
    public void testUpgradeNoCategoriesInApisTable() {
        when(categoriesCollection.find()).thenReturn(categoriesFindIterable);

        List<Document> apis = new ArrayList<>();
        apis.add(new Document("_id", "apiId1").append("categories", List.of()));
        apis.add(new Document("_id", "apiId2").append("categories", List.of()));
        when(apisCollection.find()).thenReturn(apisFindIterable);
        when(apisFindIterable.iterator()).thenReturn(apisCursor);
        when(apisCursor.hasNext()).thenReturn(true, true, false);
        when(apisCursor.next()).thenReturn(apis.get(0), apis.get(1));

        boolean result = apisCategoriesUpgrader.upgrade();

        assertTrue(result);
        verify(apisCollection, times(0)).bulkWrite(anyList());
    }

    @Test
    public void testUpgradeNoCategoriesInCategoriesTable() {
        when(categoriesCollection.find()).thenReturn(categoriesFindIterable);

        List<Document> apis = new ArrayList<>();
        apis.add(new Document("_id", "apiId1").append("categories", List.of("key1", "key3")));
        apis.add(new Document("_id", "apiId2").append("categories", List.of("key2")));
        when(apisCollection.find()).thenReturn(apisFindIterable);
        when(apisFindIterable.iterator()).thenReturn(apisCursor);
        when(apisCursor.hasNext()).thenReturn(true, true, false);
        when(apisCursor.next()).thenReturn(apis.get(0), apis.get(1));

        boolean result = apisCategoriesUpgrader.upgrade();

        assertTrue(result);
        verify(apisCollection, times(1)).bulkWrite(anyList());
    }

    @Test
    public void getOrder_shouldReturnApisCategoryVersionUpgraderOrder() {
        var result = apisCategoriesUpgrader.getOrder();
        assertThat(result).isEqualTo(APIS_CATEGORY_VERSION_UPGRADER_ORDER);
    }
}
