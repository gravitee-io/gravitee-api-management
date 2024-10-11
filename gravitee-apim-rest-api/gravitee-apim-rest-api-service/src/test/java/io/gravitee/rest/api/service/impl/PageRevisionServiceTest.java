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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageRevision;
import io.gravitee.rest.api.model.PageRevisionEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageRevisionServiceImpl;
import java.util.Date;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageRevisionServiceTest {

    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageRevisionServiceImpl pageRevisionService = new PageRevisionServiceImpl();

    @Mock
    private AuditService auditService;

    @Mock
    private PageRevisionRepository pageRevisionRepository;

    @Test
    public void shouldCreateRevision() throws TechnicalException {
        Date now = new Date();
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getName()).thenReturn("SomeName");
        when(page.getContent()).thenReturn("SomeContent");
        when(page.getUpdatedAt()).thenReturn(now);
        when(page.getLastContributor()).thenReturn("Author");
        when(page.getType()).thenReturn(PageType.MARKDOWN.name());

        PageRevision newRevision = mock(PageRevision.class);

        when(pageRevisionRepository.findLastByPageId(PAGE_ID)).thenReturn(Optional.empty());
        when(pageRevisionRepository.create(any())).thenReturn(newRevision);

        ArgumentCaptor<PageRevision> newRevisionCaptor = ArgumentCaptor.forClass(PageRevision.class);

        PageRevisionEntity createdRevision = pageRevisionService.create(page);
        assertNotNull(createdRevision);
        verify(pageRevisionRepository).findLastByPageId(PAGE_ID);
        verify(pageRevisionRepository).create(newRevisionCaptor.capture());

        PageRevision createdRev = newRevisionCaptor.getValue();
        assertNotNull(createdRev);
        assertEquals(1, createdRev.getRevision());
    }

    @Test
    public void shouldCreateRevision_NewIncrementedRevision() throws TechnicalException {
        Date now = new Date();
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getName()).thenReturn("SomeName");
        when(page.getContent()).thenReturn("SomeContent");
        when(page.getUpdatedAt()).thenReturn(now);
        when(page.getLastContributor()).thenReturn("Author");
        when(page.getType()).thenReturn(PageType.MARKDOWN.name());

        PageRevision lastRevision = mock(PageRevision.class);
        when(lastRevision.getRevision()).thenReturn(2);

        PageRevision newRevision = mock(PageRevision.class);
        when(pageRevisionRepository.findLastByPageId(PAGE_ID)).thenReturn(Optional.of(lastRevision));
        when(pageRevisionRepository.create(any())).thenReturn(newRevision);

        ArgumentCaptor<PageRevision> newRevisionCaptor = ArgumentCaptor.forClass(PageRevision.class);

        PageRevisionEntity createdRevision = pageRevisionService.create(page);
        assertNotNull(createdRevision);
        verify(pageRevisionRepository).findLastByPageId(PAGE_ID);
        verify(pageRevisionRepository).create(newRevisionCaptor.capture());

        PageRevision createdRev = newRevisionCaptor.getValue();
        assertNotNull(createdRev);
        assertEquals(lastRevision.getRevision() + 1, createdRev.getRevision());
    }

    @Test
    public void shouldCreateRevision_PortalPage() throws TechnicalException {
        Date now = new Date();
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getName()).thenReturn("SomeName");
        when(page.getContent()).thenReturn("SomeContent");
        when(page.getUpdatedAt()).thenReturn(now);
        when(page.getLastContributor()).thenReturn("Author");
        when(page.getType()).thenReturn(PageType.MARKDOWN.name());

        PageRevision newRevision = mock(PageRevision.class);

        when(pageRevisionRepository.findLastByPageId(PAGE_ID)).thenReturn(Optional.empty());
        when(pageRevisionRepository.create(any())).thenReturn(newRevision);

        ArgumentCaptor<PageRevision> newRevisionCaptor = ArgumentCaptor.forClass(PageRevision.class);

        PageRevisionEntity createdRevision = pageRevisionService.create(page);
        assertNotNull(createdRevision);
        verify(pageRevisionRepository).findLastByPageId(PAGE_ID);
        verify(pageRevisionRepository).create(newRevisionCaptor.capture());

        PageRevision createdRev = newRevisionCaptor.getValue();
        assertNotNull(createdRev);
        assertEquals(1, createdRev.getRevision());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreate_Because_InvalidType() throws TechnicalException {
        Date now = new Date();
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getType()).thenReturn(PageType.FOLDER.name());
        pageRevisionService.create(page);
    }

    @Test
    public void shouldDeletePageRevision() throws TechnicalException {
        pageRevisionService.deleteAllByPageId(PAGE_ID);
        verify(pageRevisionRepository).deleteAllByPageId(PAGE_ID);
    }

    @Test(expected = TechnicalException.class)
    public void shouldNotDeletePageRevisionBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRevisionRepository).deleteAllByPageId(PAGE_ID);
        pageRevisionService.deleteAllByPageId(PAGE_ID);
    }
}
