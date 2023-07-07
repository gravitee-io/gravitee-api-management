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
package io.gravitee.repository.jdbc.management;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import java.util.List;
import org.junit.Test;

public class JdbcAbstractPageableRepositoryTest {

    private static final List<String> aListOfString = List.of("one", "two", "three", "four", "five");

    @Test
    public void getResultAsPageReturnsPagesWithData() {
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(2).build();
        Page<String> page = JdbcAbstractPageableRepository.getResultAsPage(pageable, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getPageNumber());
        assertEquals(2, page.getPageElements());
        assertEquals(List.of("one", "two"), page.getContent());

        pageable = new PageableBuilder().pageNumber(1).pageSize(2).build();
        page = JdbcAbstractPageableRepository.getResultAsPage(pageable, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(1, page.getPageNumber());
        assertEquals(2, page.getPageElements());
        assertEquals(List.of("three", "four"), page.getContent());

        pageable = new PageableBuilder().pageNumber(2).pageSize(2).build();
        page = JdbcAbstractPageableRepository.getResultAsPage(pageable, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getPageNumber());
        assertEquals(1, page.getPageElements());
        assertEquals(List.of("five"), page.getContent());
    }

    @Test
    public void getResultAsPageReturnsEmptyPageForPageableWithoutData() {
        Pageable pageable = new PageableBuilder().pageNumber(1).pageSize(10).build();
        Page<String> page = JdbcAbstractPageableRepository.getResultAsPage(pageable, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(1, page.getPageNumber());
        assertEquals(0, page.getPageElements());
        assertEquals(emptyList(), page.getContent());
    }

    @Test
    public void getResultAsPageWithoutPageableReturnsEverything() {
        Page<String> page = JdbcAbstractPageableRepository.getResultAsPage(null, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getPageNumber());
        assertEquals(5, page.getPageElements());
        assertEquals(List.of("one", "two", "three", "four", "five"), page.getContent());
    }

    @Test
    public void getResultAsPageComputeStartIndexBasedOnPageable() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.pageNumber()).thenReturn(2);
        when(pageable.pageSize()).thenReturn(2);
        when(pageable.from()).thenReturn(0);
        when(pageable.to()).thenReturn(15);

        Page<String> page = JdbcAbstractPageableRepository.getResultAsPage(pageable, aListOfString);

        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getPageNumber());
        assertEquals(1, page.getPageElements());
        assertEquals(List.of("five"), page.getContent());
    }
}
