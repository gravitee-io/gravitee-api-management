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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageMedia;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceImplTests {

    @InjectMocks
    private PageServiceImpl pageService;

    @Mock
    private PageRepository pageRepository;

    @Test
    public void getParentPathFromFilePath_should_return_correct_path() {
        String parentPath = pageService.getParentPathFromFilePath("/folder1/folder.2/folder3/file.txt");
        assertEquals("/folder1/folder.2/folder3", parentPath);
    }

    @Test
    public void getParentPathFromFilePath_with_filename_should_return_empty() {
        String parentPath = pageService.getParentPathFromFilePath("file.txt");
        assertEquals("", parentPath);
    }

    @Test
    public void getParentPathFromFilePath_with_empty_path_should_return_slash() {
        String parentPath = pageService.getParentPathFromFilePath("");
        assertEquals("/", parentPath);
    }

    @Test
    public void isMediaUsedInPages_shouldReturnTrueWhenMediaIsUsedInAttachedMedia() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        var page = createPage("page-id", List.of(new PageMedia(mediaHash, "media-name", null)), null);
        var pages = new io.gravitee.common.data.domain.Page<>(List.of(page), 0, 1, 1);

        when(pageRepository.findAll(any())).thenReturn(pages);

        var result = pageService.isMediaUsedInPages(executionContext, mediaHash);

        assertThat(result).isTrue();
    }

    @Test
    public void isMediaUsedInPages_shouldReturnTrueWhenMediaIsUsedInContent() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        var page = createPage("page-id", null, "This is a sample content with media hash: " + mediaHash);
        var pages = new io.gravitee.common.data.domain.Page<>(List.of(page), 0, 1, 1);

        when(pageRepository.findAll(any())).thenReturn(pages);

        var result = pageService.isMediaUsedInPages(executionContext, mediaHash);

        assertThat(result).isTrue();
    }

    @Test
    public void isMediaUsedInPages_shouldReturnFalseWhenMediaIsNotUsed() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        var page = createPage("page-id", List.of(new PageMedia("other-media-hash", "media-name", null)), "Content without the media hash");

        var pages = new io.gravitee.common.data.domain.Page<>(List.of(page), 0, 1, 1);
        io.gravitee.common.data.domain.Page<Page> emptyPages = new io.gravitee.common.data.domain.Page<>(List.of(), 0, 0, 0);

        when(pageRepository.findAll(any())).thenReturn(pages).thenReturn(emptyPages);

        var result = pageService.isMediaUsedInPages(executionContext, mediaHash);

        assertThat(result).isFalse();
    }

    @Test
    public void isMediaUsedInPages_shouldReturnFalseWhenNoPagesFound() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        io.gravitee.common.data.domain.Page<Page> pages = new io.gravitee.common.data.domain.Page<>(List.of(), 0, 0, 0);

        when(pageRepository.findAll(any())).thenReturn(pages);

        var result = pageService.isMediaUsedInPages(executionContext, mediaHash);

        assertThat(result).isFalse();
    }

    @Test(expected = TechnicalManagementException.class)
    public void isMediaUsedInPages_shouldThrowTechnicalManagementExceptionOnError() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        when(pageRepository.findAll(any())).thenThrow(TechnicalException.class);

        pageService.isMediaUsedInPages(executionContext, mediaHash);
    }

    @Test
    public void isMediaUsedInPages_shouldHandlePagination() throws Exception {
        var mediaHash = "media-hash";
        var executionContext = new ExecutionContext("org-id", "env-id");

        var page1 = createPage("page-id-1", null, "Content without media hash");
        var page2 = createPage("page-id-2", null, "Content with media hash " + mediaHash);

        var pages1 = new io.gravitee.common.data.domain.Page<>(List.of(page1), 0, 1, 2);
        var pages2 = new io.gravitee.common.data.domain.Page<>(List.of(page2), 1, 1, 2);

        when(pageRepository.findAll(any())).thenReturn(pages1).thenReturn(pages2);

        var result = pageService.isMediaUsedInPages(executionContext, mediaHash);

        assertThat(result).isTrue();
    }

    private Page createPage(String id, List<PageMedia> mediaList, String content) {
        Page page = new Page();
        page.setId(id);
        page.setAttachedMedia(mediaList);
        page.setContent(content);
        return page;
    }
}
