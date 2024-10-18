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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageMedia;
import static org.mockito.ArgumentMatchers.any;

import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.GraviteeDescriptorService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
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

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private SwaggerService swaggerService;

    @Mock
    private PluginManager<FetcherPlugin> fetcherPluginManager;

    @Mock
    private FetcherConfigurationFactory fetcherConfigurationFactory;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private GraviteeDescriptorService graviteeDescriptorService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private PageConverter pageConverter;

    @Mock
    private ApiEntrypointService apiEntrypointService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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
    public void isMediaUsedInPages_shouldReturnTrueWhenMediaIsUsed() throws Exception {
        String mediaHash = "media-hash";
        String environmentId = "env-id";
        ExecutionContext executionContext = new ExecutionContext("org-id", environmentId);

        Page page = new Page();
        page.setId("page-id");
        page.setAttachedMedia(List.of(new PageMedia(mediaHash, "media-name", null)));

        when(pageRepository.search(any())).thenReturn(List.of(page));

        boolean result = pageService.isMediaUsedInPages(executionContext, mediaHash, null);

        assertThat(result).isTrue();
    }

    @Test
    public void isMediaUsedInPages_shouldReturnFalseWhenMediaIsNotUsed() throws Exception {
        String mediaHash = "media-hash";
        String environmentId = "env-id";
        ExecutionContext executionContext = new ExecutionContext("org-id", environmentId);

        Page page = new Page();
        page.setId("page-id");
        page.setAttachedMedia(List.of(new PageMedia("other-media-hash", "media-name", null)));

        when(pageRepository.search(any())).thenReturn(List.of(page));

        boolean result = pageService.isMediaUsedInPages(executionContext, mediaHash, null);

        assertThat(result).isFalse();
    }

    @Test
    public void isMediaUsedInPages_shouldReturnFalseWhenNoPagesFound() throws Exception {
        String mediaHash = "media-hash";
        String environmentId = "env-id";
        ExecutionContext executionContext = new ExecutionContext("org-id", environmentId);

        when(pageRepository.search(any())).thenReturn(List.of());

        boolean result = pageService.isMediaUsedInPages(executionContext, mediaHash, null);

        assertThat(result).isFalse();
    }

    @Test(expected = TechnicalManagementException.class)
    public void isMediaUsedInPages_shouldThrowTechnicalManagementExceptionOnError() throws Exception {
        String mediaHash = "media-hash";
        String environmentId = "env-id";
        ExecutionContext executionContext = new ExecutionContext("org-id", environmentId);

        when(pageRepository.search(any())).thenThrow(TechnicalException.class);

        pageService.isMediaUsedInPages(executionContext, mediaHash, null);
    }

}
