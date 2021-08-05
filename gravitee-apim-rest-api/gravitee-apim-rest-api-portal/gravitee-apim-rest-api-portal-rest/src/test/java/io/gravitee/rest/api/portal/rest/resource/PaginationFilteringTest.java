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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.*;

import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PaginationFilteringTest {

    AbstractResource paginatedResourceForTest = new AbstractResource() {};
    List<Integer> initList = Collections.emptyList();

    @Before
    public void init() {
        initList.clear();
    }

    @Test
    public void testPaginatedList() {
        Integer page = 3;
        Integer size = 10;
        Integer totalItems = 100;

        initList = initIntegerList(totalItems);
        Map<String, Object> paginatedMetadata = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Integer> resultList = paginatedResourceForTest.paginateResultList(initList, totalItems, page, size, paginatedMetadata);
        assertEquals(10, resultList.size());
        assertEquals(20, resultList.get(0).intValue());
        assertEquals(29, resultList.get(9).intValue());

        assertEquals(page, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_CURRENT_PAGE_KEY));
        assertEquals(21, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY));
        assertEquals(30, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_LAST_ITEM_INDEX_KEY));
        assertEquals(10, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_SIZE_KEY));
        assertEquals(100, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_TOTAL_KEY));
        assertEquals(10, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_TOTAL_PAGE_KEY));

        assertFalse(initList == resultList);
    }

    @Test
    public void testShortList() {
        Integer page = 1;
        Integer size = 10;
        Integer totalItems = 8;

        initList = initIntegerList(totalItems);
        Map<String, Object> paginatedMetadata = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Integer> resultList = paginatedResourceForTest.paginateResultList(initList, totalItems, page, size, paginatedMetadata);
        assertEquals(8, resultList.size());
        assertEquals(0, resultList.get(0).intValue());
        assertEquals(7, resultList.get(7).intValue());

        assertEquals(page, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_CURRENT_PAGE_KEY));
        assertEquals(1, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY));
        assertEquals(8, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_LAST_ITEM_INDEX_KEY));
        assertEquals(10, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_SIZE_KEY));
        assertEquals(8, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_TOTAL_KEY));
        assertEquals(1, paginatedMetadata.get(AbstractResource.METADATA_PAGINATION_TOTAL_PAGE_KEY));

        assertFalse(initList == resultList);
    }

    @Test(expected = PaginationInvalidException.class)
    public void shouldHaveBadRequestExceptionPageGreaterThanMaxPage() {
        Integer page = 20;
        Integer size = 10;
        Integer totalItems = 10;

        initList = initIntegerList(totalItems);
        Map<String, Object> paginatedMetadata = new HashMap<>();

        paginatedResourceForTest.paginateResultList(initList, totalItems, page, size, paginatedMetadata);
    }

    @Test(expected = PaginationInvalidException.class)
    public void shouldHaveBadRequestExceptionPageSmallThanMinPage() {
        Integer page = 0;
        Integer size = 10;
        Integer totalItems = 10;

        initList = initIntegerList(totalItems);
        Map<String, Object> paginatedMetadata = new HashMap<>();

        paginatedResourceForTest.paginateResultList(initList, totalItems, page, size, paginatedMetadata);
    }

    private List<Integer> initIntegerList(int n) {
        return IntStream.rangeClosed(0, n - 1).boxed().collect(Collectors.toList());
    }

    @Test
    public void testComputeMetadataWithoutInitList() {
        Map<String, Object> dataMetadata = new HashMap<>();
        dataMetadata.put("KEY", 1);
        Map<String, Object> paginationMetadata = new HashMap<>();
        paginationMetadata.put("KEY", true);

        Map<String, Map<String, Object>> metadata = paginatedResourceForTest.computeMetadata(null, dataMetadata, paginationMetadata);
        assertNotNull(metadata);
        assertEquals(2, metadata.size());

        assertEquals(dataMetadata, metadata.get(AbstractResource.METADATA_DATA_KEY));
        assertEquals(paginationMetadata, metadata.get(AbstractResource.METADATA_PAGINATION_KEY));
    }

    @Test
    public void testComputeMetadataWithInitList() {
        Map<String, Map<String, Object>> initMetadata = new HashMap<>();
        Map<String, Object> testMetadata = new HashMap<>();
        testMetadata.put("foo", "bar");
        initMetadata.put("test", testMetadata);

        Map<String, Object> dataMetadata = new HashMap<>();
        dataMetadata.put("KEY", 12);
        Map<String, Object> paginationMetadata = new HashMap<>();

        Map<String, Map<String, Object>> metadata = paginatedResourceForTest.computeMetadata(
            initMetadata,
            dataMetadata,
            paginationMetadata
        );

        assertNotNull(metadata);
        assertEquals(2, metadata.size());

        assertEquals(dataMetadata, metadata.get(AbstractResource.METADATA_DATA_KEY));

        assertEquals(testMetadata, metadata.get("test"));
    }
}
