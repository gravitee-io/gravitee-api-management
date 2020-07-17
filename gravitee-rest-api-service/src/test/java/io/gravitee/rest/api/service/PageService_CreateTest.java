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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_CreateTest {

    private static final String API_ID = "myAPI";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private NewPageEntity newPage;
    @Mock
    private Page page1;

    @Mock
    private ApiService apiService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Test
    public void shouldCreatePage() throws TechnicalException {
        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = "content";
        final String type = "MARKDOWN";

        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getName()).thenReturn(name);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(page1.getType()).thenReturn(type);
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);

        when(pageRepository.create(any())).thenReturn(page1);

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(PageType.SWAGGER);

        final PageEntity createdPage = pageService.createPage(API_ID, newPage);

        verify(pageRepository).create(argThat(pageToCreate -> pageToCreate.getId().split("-").length == 5 &&
                API_ID.equals(pageToCreate.getReferenceId()) &&
                PageReferenceType.API.equals(pageToCreate.getReferenceType()) &&
                name.equals(pageToCreate.getName()) &&
                contrib.equals(pageToCreate.getLastContributor()) &&
                content.equals(pageToCreate.getContent()) &&
                PageType.SWAGGER.name().equals(pageToCreate.getType()) &&
                pageToCreate.getCreatedAt() != null &&
                pageToCreate.getUpdatedAt() != null &&
                pageToCreate.getCreatedAt().equals(pageToCreate.getUpdatedAt())));
        assertNotNull(createdPage);
        assertEquals(5, createdPage.getId().split("-").length);
        assertEquals(1, createdPage.getOrder());
        assertEquals(content, createdPage.getContent());
        assertEquals(contrib, createdPage.getLastContributor());
        assertEquals(type, createdPage.getType());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        final String name = "PAGE_NAME";
        when(newPage.getName()).thenReturn(name);

        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.createPage(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test(expected = PageFolderActionException.class)
    public void shouldNotCreateFolderinFolderOfSystemFolderPage() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");

        Page folderInSystemFolder = new Page();
        folderInSystemFolder.setId("FOLD_IN_SYS");
        folderInSystemFolder.setType("FOLDER");
        folderInSystemFolder.setParentId("SYS");
        doReturn(Optional.of(folderInSystemFolder)).when(pageRepository).findById("FOLD_IN_SYS");

        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setType(PageType.FOLDER);
        newFolder.setParentId("FOLD_IN_SYS");

        pageService.createPage(newFolder);
    }

    @Test
    public void shouldCreateFolderInSystemFolderPage() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");


        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setType(PageType.FOLDER);
        newFolder.setParentId("SYS");

        Page createdPage = new Page();
        createdPage.setId("NEW_FOLD");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        doReturn(createdPage).when(pageRepository).create(any());

        final PageEntity createdFolder = pageService.createPage(newFolder);
        assertNotNull(createdFolder);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateSwaggerInSystemFolderPage() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");


        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setType(PageType.SWAGGER);
        newFolder.setParentId("SYS");

        pageService.createPage(newFolder);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateMarkdownInSystemFolderPage() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");


        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setType(PageType.MARKDOWN);
        newFolder.setParentId("SYS");

        pageService.createPage(newFolder);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateLinkOfSystemFolder() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");

        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, PageConfigurationKeys.LINK_RESOURCE_TYPE_PAGE);

        NewPageEntity newLink = new NewPageEntity();
        newLink.setType(PageType.LINK);
        newLink.setParentId("SYS");
        newLink.setConfiguration(conf);
        newLink.setContent("SYS");

        pageService.createPage(newLink);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateLinkOfFolderInSystemFolder() throws TechnicalException {
        Page systemFolder = new Page();
        systemFolder.setId("SYS");
        systemFolder.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(systemFolder)).when(pageRepository).findById("SYS");

        Page folderInSystemFolder = new Page();
        folderInSystemFolder.setId("FOLD_IN_SYS");
        folderInSystemFolder.setType("FOLDER");
        folderInSystemFolder.setParentId("SYS");
        doReturn(Optional.of(folderInSystemFolder)).when(pageRepository).findById("FOLD_IN_SYS");

        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, PageConfigurationKeys.LINK_RESOURCE_TYPE_PAGE);

        NewPageEntity newLink = new NewPageEntity();
        newLink.setType(PageType.LINK);
        newLink.setParentId("SYS");
        newLink.setConfiguration(conf);
        newLink.setContent("FOLD_IN_SYS");

        pageService.createPage(newLink);
    }

    @Test
    public void shouldCreatePublishedLinkForRoot() throws TechnicalException {
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, PageConfigurationKeys.LINK_RESOURCE_TYPE_EXTERNAL);

        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setType(PageType.LINK);
        newFolder.setParentId("SYS");
        newFolder.setConfiguration(conf);
        newFolder.setContent("root");

        Page createdPage = new Page();
        createdPage.setId("NEW_LINK");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(newFolder);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));

    }

    @Test
    public void shouldCopyPublishedStateWhenCreateLink() throws TechnicalException {
        Page page = new Page();
        page.setId("PAGE");
        page.setType("MARKDOWN");
        page.setPublished(true);
        doReturn(Optional.of(page)).when(pageRepository).findById(PAGE_ID);

        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, PageConfigurationKeys.LINK_RESOURCE_TYPE_PAGE);

        NewPageEntity newLink = new NewPageEntity();
        newLink.setType(PageType.LINK);
        newLink.setParentId("SYS");
        newLink.setConfiguration(conf);
        newLink.setContent(PAGE_ID);

        Page createdPage = new Page();
        createdPage.setId("NEW_LINK");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(newLink);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));

    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfNoParent() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);

        pageService.createPage(newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageWithoutConfiguration() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("FOLDER");

        pageService.createPage(newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageWithoutLang() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("FOLDER");

        Map<String, String> conf = new HashMap<>();
        newTranslation.setConfiguration(conf);

        pageService.createPage(newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfParentIsSystemFolder() throws TechnicalException {
        Page parentPage = new Page();
        parentPage.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(parentPage)).when(pageRepository).findById("SYS_FOLDER");

        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("SYS_FOLDER");
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        newTranslation.setConfiguration(conf);

        pageService.createPage(newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfParentIsRoot() throws TechnicalException {
        Page parentPage = new Page();
        parentPage.setType("ROOT");
        doReturn(Optional.of(parentPage)).when(pageRepository).findById("ROOT");

        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("ROOT");
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        newTranslation.setConfiguration(conf);

        pageService.createPage(newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfParentIsTranslation() throws TechnicalException {
        Page parentPage = new Page();
        parentPage.setType("TRANSLATION");
        doReturn(Optional.of(parentPage)).when(pageRepository).findById("TRANSLATION");

        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("TRANSLATION");

        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        newTranslation.setConfiguration(conf);

        pageService.createPage(newTranslation);
    }

    @Test
    public void shouldCopyPublishedStateWhenCreateTranslation() throws TechnicalException {
        Page page = new Page();
        page.setId(PAGE_ID);
        page.setType("MARKDOWN");
        page.setPublished(true);
        doReturn(Optional.of(page)).when(pageRepository).findById(PAGE_ID);


        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId(PAGE_ID);
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        newTranslation.setConfiguration(conf);
        newTranslation.setPublished(false);

        Page createdPage = new Page();
        createdPage.setId("NEW_TRANSLATION");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(newTranslation);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));

    }


    @Test(expected = UrlForbiddenException.class)
    public void shouldNotCreateBecauseUrlForbiddenException() throws TechnicalException {

        PageSourceEntity pageSource = new PageSourceEntity();
        pageSource.setType("HTTP");
        pageSource.setConfiguration(JsonNodeFactory.instance.objectNode().put("url", "http://localhost"));

        final String name = "PAGE_NAME";

        when(newPage.getName()).thenReturn(name);
        when(newPage.getSource()).thenReturn(pageSource);

        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());

        pageService.createPage(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test(expected = PageContentUnsafeException.class)
    public void shouldNotCreateBecausePageContentUnsafeException() throws TechnicalException {

        setField(pageService, "markdownSanitize", true);

        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = "<script />";

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(PageType.MARKDOWN);

        this.pageService.createPage(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }
}
