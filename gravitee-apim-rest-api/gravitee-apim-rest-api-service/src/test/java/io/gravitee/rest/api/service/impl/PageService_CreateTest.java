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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.TemplateException;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    private PageRevisionService pageRevisionService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    private PageEntity getPage(String resource, String contentType) throws IOException {
        URL url = Resources.getResource(resource);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        PageEntity pageEntity = new PageEntity();
        pageEntity.setContent(descriptor);
        pageEntity.setContentType(contentType);
        return pageEntity;
    }

    @Test
    public void shouldCreatePage() throws TechnicalException, IOException {
        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = getPage("io/gravitee/rest/api/management/service/swagger-v1.json", MediaType.APPLICATION_JSON).getContent();
        final String type = "MARKDOWN";

        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getName()).thenReturn(name);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(page1.getType()).thenReturn(type);
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(page1.getVisibility()).thenReturn("PUBLIC");
        when(page1.toBuilder()).thenReturn(new Page().toBuilder());

        when(pageRepository.create(any())).thenReturn(page1);

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(PageType.SWAGGER);
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(newPage.getAttachedMedia()).thenReturn(List.of(new PageMediaEntity()));

        final PageEntity createdPage = pageService.createPage(new ExecutionContext("DEFAULT", "envId"), API_ID, newPage);

        verify(pageRepository)
            .create(
                argThat(pageToCreate ->
                    pageToCreate.getId().split("-").length == 5 &&
                    API_ID.equals(pageToCreate.getReferenceId()) &&
                    PageReferenceType.API.equals(pageToCreate.getReferenceType()) &&
                    pageToCreate.getAttachedMedia().size() == 1 &&
                    name.equals(pageToCreate.getName()) &&
                    contrib.equals(pageToCreate.getLastContributor()) &&
                    content.equals(pageToCreate.getContent()) &&
                    PageType.SWAGGER.name().equals(pageToCreate.getType()) &&
                    pageToCreate.getCreatedAt() != null &&
                    pageToCreate.getUpdatedAt() != null &&
                    pageToCreate.getCreatedAt().equals(pageToCreate.getUpdatedAt())
                )
            );
        assertNotNull(createdPage);
        assertEquals(5, createdPage.getId().split("-").length);
        assertEquals(1, createdPage.getOrder());
        assertEquals(content, createdPage.getContent());
        assertEquals(contrib, createdPage.getLastContributor());
        assertEquals(type, createdPage.getType());
        // create revision for MD page
        verify(pageRevisionService).create(page1);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException, IOException {
        final String name = "PAGE_NAME";
        when(newPage.getName()).thenReturn(name);
        when(newPage.getType()).thenReturn(PageType.SWAGGER);
        when(newPage.getContent())
            .thenReturn(getPage("io/gravitee/rest/api/management/service/swagger-v1.json", MediaType.APPLICATION_JSON).getContent());
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);

        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.createPage(GraviteeContext.getExecutionContext(), API_ID, newPage);

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

        pageService.createPage(GraviteeContext.getExecutionContext(), newFolder);
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
        newFolder.setVisibility(Visibility.PUBLIC);

        Page createdPage = new Page();
        createdPage.setId("NEW_FOLD");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        createdPage.setVisibility("PUBLIC");
        createdPage.setType(PageType.FOLDER.toString());
        doReturn(createdPage).when(pageRepository).create(any());

        final PageEntity createdFolder = pageService.createPage(new ExecutionContext("DEFAULT", "DEFAULT"), newFolder);
        assertNotNull(createdFolder);

        verify(pageRevisionService, times(0)).create(any());
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newFolder);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newFolder);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newLink);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newLink);
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
        newFolder.setVisibility(Visibility.PUBLIC);

        Page createdPage = new Page();
        createdPage.setId("NEW_LINK");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        createdPage.setVisibility("PUBLIC");
        createdPage.setType(PageType.LINK.toString());
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(new ExecutionContext("DEFAULT", "DEFAULT"), newFolder);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));
        // do not create revision for Link
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test
    public void shouldCopyPublishedStateWhenCreateLink() throws TechnicalException {
        Page page = new Page();
        page.setId("PAGE");
        page.setType("MARKDOWN");
        page.setPublished(true);
        page.setVisibility("PUBLIC");
        doReturn(Optional.of(page)).when(pageRepository).findById(PAGE_ID);

        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, PageConfigurationKeys.LINK_RESOURCE_TYPE_PAGE);

        NewPageEntity newLink = new NewPageEntity();
        newLink.setType(PageType.LINK);
        newLink.setParentId("SYS");
        newLink.setConfiguration(conf);
        newLink.setContent(PAGE_ID);
        newLink.setVisibility(Visibility.PUBLIC);

        Page createdPage = new Page();
        createdPage.setId("NEW_LINK");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        createdPage.setVisibility("PUBLIC");
        createdPage.setType(PageType.LINK.toString());
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(new ExecutionContext("DEFAULT", "DEFAULT"), newLink);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));
        // do not create revision for Link
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfNoParent() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageWithoutConfiguration() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("FOLDER");

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageWithoutLang() throws TechnicalException {
        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId("FOLDER");

        Map<String, String> conf = new HashMap<>();
        newTranslation.setConfiguration(conf);

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
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

        pageService.createPage(GraviteeContext.getExecutionContext(), newTranslation);
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
        newTranslation.setVisibility(Visibility.PUBLIC);

        Page createdPage = new Page();
        createdPage.setId("NEW_TRANSLATION");
        createdPage.setType("TRANSLATION");
        createdPage.setReferenceId("DEFAULT");
        createdPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        createdPage.setVisibility("PUBLIC");
        doReturn(createdPage).when(pageRepository).create(any());

        pageService.createPage(new ExecutionContext("DEFAULT", "DEFAULT"), newTranslation);
        verify(pageRepository).create(argThat(p -> p.isPublished() == true));
        // create revision for translate if the parent page is a Markdown or Swagger
        verify(pageRevisionService, times(1)).create(any());
    }

    @Test(expected = PageContentUnsafeException.class)
    public void shouldNotCreateTranslationBecausePageContentUnsafeException() throws TechnicalException {
        setField(pageService, "markdownSanitize", true);
        Page page = new Page();
        page.setId(PAGE_ID);
        page.setType("MARKDOWN");
        page.setReferenceId(API_ID);
        page.setPublished(true);
        doReturn(Optional.of(page)).when(pageRepository).findById(PAGE_ID);

        NewPageEntity newTranslation = new NewPageEntity();
        newTranslation.setType(PageType.TRANSLATION);
        newTranslation.setParentId(PAGE_ID);
        Map<String, String> conf = new HashMap<>();
        conf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        newTranslation.setConfiguration(conf);
        newTranslation.setPublished(false);
        newTranslation.setVisibility(Visibility.PUBLIC);
        newTranslation.setContent("[Click me](javascript:alert(\"XSS\"))");

        when(htmlSanitizer.isSafe(anyString())).thenReturn(new HtmlSanitizer.SanitizeInfos(false, "Tag not allowed: script"));

        when(
            this.notificationTemplateService.resolveInlineTemplateWithParam(
                    anyString(),
                    anyString(),
                    eq(newTranslation.getContent()),
                    any(),
                    anyBoolean()
                )
        )
            .thenReturn(newTranslation.getContent());

        pageService.createPage(new ExecutionContext("DEFAULT", "DEFAULT"), newTranslation);
        verify(pageRepository, never()).create(any());
    }

    @Test(expected = UrlForbiddenException.class)
    public void shouldNotCreateBecauseUrlForbiddenException() throws TechnicalException {
        PageSourceEntity pageSource = new PageSourceEntity();
        pageSource.setType("HTTP");
        pageSource.setConfiguration(JsonNodeFactory.instance.objectNode().put("url", "http://localhost"));

        final String name = "PAGE_NAME";

        when(newPage.getSource()).thenReturn(pageSource);
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);

        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());

        pageService.createPage(GraviteeContext.getExecutionContext(), API_ID, newPage);

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
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), eq(content), any(), anyBoolean()))
            .thenReturn(content);
        when(htmlSanitizer.isSafe(anyString())).thenReturn(new HtmlSanitizer.SanitizeInfos(false, "Tag not allowed: script"));

        this.pageService.createPage(GraviteeContext.getExecutionContext(), API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test
    public void shouldNotCreateBecausePageContentTemplatingException() throws TechnicalException, TemplateException {
        setField(pageService, "markdownSanitize", true);

        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = "${api.metadata['my_metadata']}";

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(PageType.MARKDOWN);
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getName()).thenReturn(name);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(page1.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page1.getType()).thenReturn("MARKDOWN");
        when(page1.getVisibility()).thenReturn("PUBLIC");
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);
        when(page1.toBuilder()).thenReturn(new Page().toBuilder());

        when(pageRepository.create(any())).thenReturn(page1);

        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), eq(content), any(), anyBoolean()))
            .thenThrow(new TemplateProcessingException(new TemplateException(null)));
        this.pageService.createPage(GraviteeContext.getExecutionContext(), API_ID, newPage);

        verify(pageRepository).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldFailToCreatePageWithInvalidFetchCron() {
        PageSourceEntity source = new PageSourceEntity();
        source.setType("github-fetcher");
        source.setConfiguration(JsonNodeFactory.instance.objectNode().put("autoFetch", true).put("fetchCron", "15 8,13 * * MON-FRI")); // Invalid cron
        when(newPage.getSource()).thenReturn(source);
        when(newPage.getVisibility()).thenReturn(Visibility.PUBLIC);
        pageService.createPage(GraviteeContext.getExecutionContext(), API_ID, newPage);
    }
}
