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
package io.gravitee.management.service;

import io.gravitee.management.model.PageListItem;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceTest_FindByApi {

    private static final String API_ID = "myAPI";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private Page page1;
    @Mock
    private Page page2;

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final Set<Page> pages = new HashSet<>();
        pages.add(page1);
        pages.add(page2);

        when(page1.getId()).thenReturn("Page 1");
        when(page2.getId()).thenReturn("Page 2");
        when(page1.getOrder()).thenReturn(1);
        when(page1.getType()).thenReturn(PageType.MARKDOWN);
        when(page2.getOrder()).thenReturn(2);
        when(page2.getType()).thenReturn(PageType.RAML);
        when(pageRepository.findApiPageByApiId(API_ID)).thenReturn(pages);

        final List<PageListItem> pageEntities = pageService.findApiPagesByApi(API_ID);

        assertNotNull(pageEntities);
        assertEquals(2, pageEntities.size());
        assertEquals(1, pageEntities.iterator().next().getOrder());
    }

    @Test
    public void shouldNotFindByApiBecauseNotFound() throws TechnicalException {
        when(pageRepository.findApiPageByApiId(API_ID)).thenReturn(null);

        final List<PageListItem> pageEntities = pageService.findApiPagesByApi(API_ID);

        assertNotNull(pageEntities);
        assertTrue(pageEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiNameBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findApiPageByApiId(API_ID)).thenThrow(TechnicalException.class);

        pageService.findApiPagesByApi(API_ID);
    }
}
