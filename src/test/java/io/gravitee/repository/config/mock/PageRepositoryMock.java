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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.repository.management.model.PageType;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRepositoryMock extends AbstractRepositoryMock<PageRepository> {

    public PageRepositoryMock() {
        super(PageRepository.class);
    }

    @Override
    void prepare(PageRepository pageRepository) throws Exception {
        Page findApiPage = mock(Page.class);
        when(findApiPage.getId()).thenReturn("FindApiPage");
        when(findApiPage.getName()).thenReturn("Find apiPage by apiId or Id");
        when(findApiPage.getContent()).thenReturn("Content of the page");
        when(findApiPage.getApi()).thenReturn("my-api");
        when(findApiPage.getType()).thenReturn(PageType.MARKDOWN);
        when(findApiPage.getLastContributor()).thenReturn("john_doe");
        when(findApiPage.getOrder()).thenReturn(2);
        when(findApiPage.isPublished()).thenReturn(true);
        PageSource pageSource = new PageSource();
        pageSource.setType("sourceType");
        pageSource.setConfiguration("sourceConfiguration");
        when(findApiPage.getSource()).thenReturn(pageSource);

        Map<String, String> pageConfiguration = new HashMap<>();
        pageConfiguration.put("tryIt", "true");
        pageConfiguration.put("tryItURL", "http://company.com");
        pageConfiguration.put("showURL", "true");
        pageConfiguration.put("displayOperationId", "true");
        pageConfiguration.put("docExpansion", "FULL");
        pageConfiguration.put("enableFiltering", "true");
        pageConfiguration.put("showExtensions", "true");
        pageConfiguration.put("showCommonExtensions", "true");
        pageConfiguration.put("maxDisplayedTags", "1234");
        when(findApiPage.getConfiguration()).thenReturn(pageConfiguration);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "http://provider.com/edit/page");
        metadata.put("size", "256");
        when(findApiPage.getMetadata()).thenReturn(metadata);

        when(findApiPage.isHomepage()).thenReturn(true);
        when(findApiPage.getExcludedGroups()).thenReturn(asList("grp1", "grp2"));
        when(findApiPage.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(findApiPage.getUpdatedAt()).thenReturn(new Date(1486771200000L));

        // shouldFindApiPageByApiId
        when(pageRepository.search(argThat(o -> o == null || o.getApi().equals("my-api")))).thenReturn(singletonList(findApiPage));

        // shouldFindApiPageById
        when(pageRepository.findById("FindApiPage")).thenReturn(of(findApiPage));

        // shouldCreateApiPage
        final Page createPage = mock(Page.class);
        when(createPage.getName()).thenReturn("Page name");
        when(createPage.getContent()).thenReturn("Page content");
        when(createPage.getOrder()).thenReturn(3);
        when(createPage.getType()).thenReturn(PageType.MARKDOWN);
        when(createPage.isHomepage()).thenReturn(true);
        when(createPage.getParentId()).thenReturn("2");
        metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        when(createPage.getConfiguration()).thenReturn(pageConfiguration);
        when(createPage.getMetadata()).thenReturn(metadata);
        when(pageRepository.findById("new-page")).thenReturn(empty(), of(createPage));

        // shouldCreateApiPageFolder
        final Page createPageFolder = mock(Page.class);
        when(createPageFolder.getName()).thenReturn("Folder name");
        when(createPageFolder.getContent()).thenReturn(null);
        when(createPageFolder.getOrder()).thenReturn(3);
        when(createPageFolder.getType()).thenReturn(PageType.FOLDER);
        when(createPageFolder.isHomepage()).thenReturn(false);
        when(createPageFolder.getParentId()).thenReturn("");
        when(pageRepository.findById("new-page-folder")).thenReturn(empty(), of(createPageFolder));

        // shouldCreatePortalPage
        final Page createPortalPage = mock(Page.class);
        when(createPortalPage.getName()).thenReturn("Page name");
        when(createPortalPage.getContent()).thenReturn("Page content");
        when(createPortalPage.getOrder()).thenReturn(3);
        when(createPortalPage.getType()).thenReturn(PageType.MARKDOWN);
        when(createPortalPage.isHomepage()).thenReturn(false);
        when(createPortalPage.getParentId()).thenReturn("2");
        metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        when(createPortalPage.getConfiguration()).thenReturn(pageConfiguration);
        when(createPortalPage.getMetadata()).thenReturn(metadata);
        when(pageRepository.findById("new-portal-page")).thenReturn(empty(), of(createPortalPage));

        // shouldCreatePortalPageFolder
        final Page createPortalPageFolder = mock(Page.class);
        when(createPortalPageFolder.getName()).thenReturn("Folder name");
        when(createPortalPageFolder.getContent()).thenReturn(null);
        when(createPortalPageFolder.getOrder()).thenReturn(3);
        when(createPortalPageFolder.getType()).thenReturn(PageType.FOLDER);
        when(createPortalPageFolder.isHomepage()).thenReturn(false);
        when(createPortalPageFolder.getParentId()).thenReturn("");
        when(pageRepository.findById("new-portal-page-folder")).thenReturn(empty(), of(createPortalPageFolder));


        // shouldDelete
        when(pageRepository.findById("page-to-be-deleted")).thenReturn(of(mock(Page.class)), empty());

        // should Update
        Page updatePageBefore = mock(Page.class);
        when(updatePageBefore.getId()).thenReturn("updatePage");
        when(updatePageBefore.getName()).thenReturn("Update Page");
        when(updatePageBefore.getContent()).thenReturn("Content of the update page");
        when(updatePageBefore.getConfiguration()).thenReturn(new HashMap<>());
        Page updatePageAfter = mock(Page.class);
        when(updatePageAfter.getId()).thenReturn("updatePage");
        when(updatePageAfter.getName()).thenReturn("New name");
        when(updatePageAfter.getContent()).thenReturn("New content");
        when(updatePageAfter.getApi()).thenReturn("new api");
        when(updatePageAfter.getType()).thenReturn(PageType.SWAGGER);
        when(updatePageAfter.getOrder()).thenReturn(1);
        when(updatePageAfter.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(updatePageAfter.getCreatedAt()).thenReturn(new Date(1486772200000L));
        when(updatePageAfter.getParentId()).thenReturn("parent-123");
        when(updatePageAfter.isHomepage()).thenReturn(true);
        when(updatePageAfter.getExcludedGroups()).thenReturn(Collections.singletonList("excluded"));
        when(updatePageAfter.getLastContributor()).thenReturn("me");
        when(updatePageAfter.isPublished()).thenReturn(true);
        Map<String, String> pageConfigurationMock = mock(HashMap.class);
        when(pageConfigurationMock.get("tryIt")).thenReturn("true");
        when(pageConfigurationMock.get("tryItURL")).thenReturn("http://company.com");
        when(pageConfigurationMock.get("showURL")).thenReturn("true");
        when(pageConfigurationMock.get("displayOperationId")).thenReturn("true");
        when(pageConfigurationMock.get("docExpansion")).thenReturn("FULL");
        when(pageConfigurationMock.get("enableFiltering")).thenReturn("true");
        when(pageConfigurationMock.get("showExtensions")).thenReturn("true");
        when(pageConfigurationMock.get("showCommonExtensions")).thenReturn("true");
        when(pageConfigurationMock.get("maxDisplayedTags")).thenReturn("1234");
        when(updatePageAfter.getConfiguration()).thenReturn(pageConfigurationMock);
        metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        when(updatePageAfter.getMetadata()).thenReturn(metadata);
        when(pageRepository.findById("updatePage")).thenReturn(of(updatePageBefore), of(updatePageAfter));

        when(pageRepository.update(argThat(o -> o != null && o.getId().equals("updatePage")))).thenReturn(updatePageAfter);

        // should Update Page folder
        Page updatePageFolderBefore = mock(Page.class);
        when(updatePageFolderBefore.getId()).thenReturn("updatePageFolder");
        when(updatePageFolderBefore.getName()).thenReturn("Update Page Folder");
        when(updatePageFolderBefore.getContent()).thenReturn("Content of the update page folder");
        when(updatePageFolderBefore.getParentId()).thenReturn("2");
        Page updatePageFolderAfter = mock(Page.class);
        when(updatePageFolderAfter.getId()).thenReturn("updatePageFolder");
        when(updatePageFolderAfter.getName()).thenReturn("New name page folder");
        when(updatePageFolderAfter.getContent()).thenReturn("New content page folder");
        when(updatePageFolderAfter.getParentId()).thenReturn("3");
        when(pageRepository.findById("updatePageFolder")).thenReturn(of(updatePageFolderBefore), of(updatePageFolderAfter));
        when(pageRepository.update(argThat(o -> o != null && o.getId().equals("updatePageFolder")))).thenReturn(updatePageFolderAfter);
        when(pageRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        //Find api pages
        final Page homepage = mock(Page.class);
        when(homepage.getId()).thenReturn("home");
        when(pageRepository.search(argThat(o -> o == null || o.getApi().equals("my-api-2") && o.getHomepage().equals(Boolean.TRUE)))).thenReturn(singletonList(homepage));
        when(pageRepository.search(argThat(o -> o == null || o.getApi().equals("my-api-2") && o.getHomepage().equals(Boolean.FALSE)))).thenReturn(asList(mock(Page.class), mock(Page.class)));

        //Find portal pages
        final Page portalHomepage = mock(Page.class);
        when(portalHomepage.getId()).thenReturn("FindPortalPage-homepage");
        final Page portalNotHomepage = mock(Page.class);
        when(portalNotHomepage.getId()).thenReturn("FindPortalPage-nothomepage");
        when(pageRepository.search(argThat(o -> o == null || o.getApi() == null))).thenReturn(asList(portalHomepage, portalNotHomepage));
        when(pageRepository.search(argThat(o -> o == null || o.getApi() == null && o.getHomepage() != null && o.getHomepage().equals(Boolean.TRUE)))).thenReturn(singletonList(portalHomepage));
        when(pageRepository.search(argThat(o -> o == null || o.getApi() == null && o.getHomepage() != null && o.getHomepage().equals(Boolean.FALSE)))).thenReturn(singletonList(portalNotHomepage));

        // Find max api page order
        when(pageRepository.findMaxApiPageOrderByApiId("my-api-2")).thenReturn(2);
        when(pageRepository.findMaxApiPageOrderByApiId("unknown api id")).thenReturn(0);
        when(pageRepository.findMaxPortalPageOrder()).thenReturn(20);
    }
}
