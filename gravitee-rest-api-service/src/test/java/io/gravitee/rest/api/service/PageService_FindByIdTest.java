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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageRevisionEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_FindByIdTest {

    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";
    private static final String TRANSLATION_ID = "a5994aad-3e0f-ea18-9944-ad3e0fea10ab";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private Page page1;

    @Mock
    private Page translationPage;

    @Mock
    private PageRevisionService pageRevisionService;
    
    @Test
    public void shouldFindById() throws TechnicalException {
        when(translationPage.getId()).thenReturn(TRANSLATION_ID);
        when(translationPage.getOrder()).thenReturn(1);
        when(translationPage.getParentId()).thenReturn(PAGE_ID);
        when(translationPage.getVisibility()).thenReturn("PUBLIC");
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "FR");
        when(translationPage.getConfiguration()).thenReturn(conf);

        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getOrder()).thenReturn(1);
        when(page1.getVisibility()).thenReturn("PUBLIC");
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.search(argThat(p->"TRANSLATION".equals(p.getType()) && PAGE_ID.equals(p.getParent())))).thenReturn(Arrays.asList(translationPage));

        final PageRevisionEntity pageRevision = new PageRevisionEntity();
        pageRevision.setRevision(5);
        pageRevision.setPageId(PAGE_ID);
        when(pageRevisionService.findLastByPageId(PAGE_ID)).thenReturn(Optional.of(pageRevision));

        final PageEntity pageEntity = pageService.findById(PAGE_ID);

        assertNotNull(pageEntity);
        assertEquals(1, pageEntity.getOrder());
        assertEquals(PAGE_ID, pageEntity.getId());
        List<PageEntity> translations = pageEntity.getTranslations();
        assertNotNull(translations);
        assertEquals(1, translations.size());
        PageEntity oneTranslation = translations.get(0);
        assertNotNull(oneTranslation);
        assertEquals(TRANSLATION_ID, oneTranslation.getId());

        assertNotNull(pageEntity.getContentRevisionId());
        assertEquals(PAGE_ID, pageEntity.getContentRevisionId().getPageId());
        assertEquals(5, pageEntity.getContentRevisionId().getRevision());
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotFindByIdBecauseNotFound() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        pageService.findById(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenThrow(TechnicalException.class);

        pageService.findById(PAGE_ID);
    }
}
