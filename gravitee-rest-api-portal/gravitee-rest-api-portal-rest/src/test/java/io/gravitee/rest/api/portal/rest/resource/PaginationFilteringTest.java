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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.BadRequestException;

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
        
        initList = initIntegerList(100);
        
        List<Integer> resultList = paginatedResourceForTest.paginateResultList(initList, page, size);
        assertEquals(10, resultList.size());
        assertEquals(20, resultList.get(0).intValue());
        assertEquals(29, resultList.get(9).intValue());
        
        assertFalse(initList == resultList);
    }

    @Test
    public void testShortList() {
        Integer page = 1;
        Integer size = 10;
        initList = initIntegerList(8);
        
        List<Integer> resultList = paginatedResourceForTest.paginateResultList(initList, page, size);
        assertEquals(8, resultList.size());
        assertEquals(0, resultList.get(0).intValue());
        assertEquals(7, resultList.get(7).intValue());
        
        assertFalse(initList == resultList);
    }
    
    @Test
    public void testNoFilteringEmptyList() {
        Integer page = 1;
        Integer size = 10;
        
        List<Integer> resultList = paginatedResourceForTest.paginateResultList(initList, page, size);
        assertEquals(0, resultList.size());
        
        assertTrue(initList == resultList);

    }
    
    @Test(expected = BadRequestException.class)
    public void shouldHaveBadRequestExceptionPageGreaterThanMaxPage() {
        Integer page = 20;
        Integer size = 10;
        initList = initIntegerList(10);
        
        paginatedResourceForTest.paginateResultList(initList, page, size);
    }

    @Test(expected = BadRequestException.class)
    public void shouldHaveBadRequestExceptionPageSmallThanMinPage() {
        Integer page = 0;
        Integer size = 10;
        initList = initIntegerList(10);
        
        paginatedResourceForTest.paginateResultList(initList, page, size);
    }
    
    private List<Integer> initIntegerList(int n) {
        return IntStream.rangeClosed(0, n-1).boxed().collect(Collectors.toList());
    }
}
