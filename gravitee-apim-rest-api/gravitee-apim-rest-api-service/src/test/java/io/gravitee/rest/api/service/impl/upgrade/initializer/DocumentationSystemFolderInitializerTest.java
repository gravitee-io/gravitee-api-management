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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.SystemFolderType;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentationSystemFolderInitializerTest {

    @Mock
    private PageService pageService;

    @Mock
    private ApiRepository apiRepository;

    private DocumentationSystemFolderInitializer initializer;
    private ExecutionContext executionContext;
    private static final String ORG_ID = "org-test";
    private static final String ENV_ID = "env-test";

    @Before
    public void setUp() throws Exception {
        initializer = new DocumentationSystemFolderInitializer(pageService, apiRepository);

        executionContext = new ExecutionContext(ORG_ID, ENV_ID);
    }

    @Test
    public void shouldDoNothingIfSystemFoldersAlreadyExist() {
        when(pageService.search(anyString(), argThat(pageQuery -> pageQuery.getType() == PageType.SYSTEM_FOLDER))).thenReturn(
            List.of(new PageEntity())
        );

        initializer.initializeEnvironment(executionContext);

        verify(pageService, never()).initialize(any(ExecutionContext.class));
        verify(pageService, never()).search(anyString(), argThat(pageQuery -> pageQuery.getType() == null));
        verify(pageService, never()).createPage(any(ExecutionContext.class), any(NewPageEntity.class));
        verify(apiRepository, never()).searchIds(any(), any(), any());
        verify(pageService, never()).createSystemFolder(
            any(ExecutionContext.class),
            anyString(),
            any(SystemFolderType.class),
            any(Integer.class)
        );
    }

    @Test
    public void shouldCreatePortalSystemFolderOnly() {
        when(pageService.search(anyString(), argThat(pageQuery -> pageQuery.getType() == PageType.SYSTEM_FOLDER))).thenReturn(List.of());
        when(pageService.initialize(executionContext)).thenReturn(
            Map.of(SystemFolderType.HEADER, "header-id", SystemFolderType.TOPFOOTER, "top-footer-id")
        );
        when(apiRepository.searchIds(anyList(), any(Pageable.class), isNull())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        initializer.initializeEnvironment(executionContext);

        verify(pageService, times(1)).initialize(executionContext);
        verify(pageService, times(1)).search(
            eq(ENV_ID),
            argThat(pageQuery -> pageQuery.getType() == null && !pageQuery.getHomepage() && pageQuery.getRootParent())
        );
        verify(pageService, times(1)).createPage(any(ExecutionContext.class), any(NewPageEntity.class));
        verify(apiRepository, times(1)).searchIds(any(), any(), any());
        verify(pageService, never()).createSystemFolder(
            any(ExecutionContext.class),
            anyString(),
            any(SystemFolderType.class),
            any(Integer.class)
        );
    }

    @Test
    public void shouldCreatePortalSystemFolderAndLinkInFooter() {
        when(pageService.search(anyString(), argThat(pageQuery -> pageQuery.getType() == PageType.SYSTEM_FOLDER))).thenReturn(List.of());
        when(pageService.initialize(executionContext)).thenReturn(
            Map.of(SystemFolderType.HEADER, "header-id", SystemFolderType.TOPFOOTER, "top-footer-id")
        );

        PageEntity documentationFolderPage = new PageEntity();
        documentationFolderPage.setType("FOLDER");
        documentationFolderPage.setParentId("top-footer-id");
        documentationFolderPage.setId("doc-folder-id");
        when(
            pageService.createPage(
                eq(executionContext),
                argThat(newPageEntity -> newPageEntity.getType() == PageType.FOLDER && newPageEntity.getParentId().equals("top-footer-id"))
            )
        ).thenReturn(documentationFolderPage);

        PageEntity markdownPage = new PageEntity();
        markdownPage.setId("markdown-page-id");
        markdownPage.setName("markdown-page-name");
        markdownPage.setType("MARKDOWN");
        when(
            pageService.search(
                eq(ENV_ID),
                argThat(pageQuery -> pageQuery.getType() == null && !pageQuery.getHomepage() && pageQuery.getRootParent())
            )
        ).thenReturn(List.of(markdownPage));

        when(apiRepository.searchIds(anyList(), any(Pageable.class), isNull())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        initializer.initializeEnvironment(executionContext);

        verify(pageService, times(1)).initialize(executionContext);
        verify(pageService, times(1)).search(
            eq(ENV_ID),
            argThat(pageQuery -> pageQuery.getType() == null && !pageQuery.getHomepage() && pageQuery.getRootParent())
        );
        verify(pageService, times(3)).createPage(any(ExecutionContext.class), any(NewPageEntity.class));
        verify(apiRepository, times(1)).searchIds(any(), any(), any());
        verify(pageService, never()).createSystemFolder(
            any(ExecutionContext.class),
            anyString(),
            any(SystemFolderType.class),
            any(Integer.class)
        );
    }

    @Test
    public void shouldCreatePortalSystemFolderAndApisSystemFolder() {
        when(pageService.search(anyString(), argThat(pageQuery -> pageQuery.getType() == PageType.SYSTEM_FOLDER))).thenReturn(List.of());
        when(pageService.initialize(executionContext)).thenReturn(
            Map.of(SystemFolderType.HEADER, "header-id", SystemFolderType.TOPFOOTER, "top-footer-id")
        );
        when(apiRepository.searchIds(anyList(), any(Pageable.class), isNull())).thenReturn(
            new Page<>(List.of("api-1"), 0, 1, 2),
            new Page<>(List.of("api-2"), 1, 1, 2),
            new Page<>(Collections.emptyList(), 2, 0, 2)
        );

        initializer.initializeEnvironment(executionContext);

        verify(pageService, times(1)).initialize(executionContext);
        verify(pageService, times(1)).search(
            eq(ENV_ID),
            argThat(pageQuery -> pageQuery.getType() == null && !pageQuery.getHomepage() && pageQuery.getRootParent())
        );
        verify(pageService, times(1)).createPage(any(ExecutionContext.class), any(NewPageEntity.class));
        verify(apiRepository, times(3)).searchIds(any(), any(), any());
        verify(pageService, times(2)).createSystemFolder(
            any(ExecutionContext.class),
            anyString(),
            any(SystemFolderType.class),
            any(Integer.class)
        );
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.DOCUMENTATION_SYSTEM_FOLDER_INITIALIZER, initializer.getOrder());
    }
}
