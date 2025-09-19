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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PageServiceImplTests {

    public static final String ORGANIZATION_ID = "my-org";
    public static final String ENVIRONMENT_ID = "my-env";
    public static final String API_ID = "my-api";
    public static final ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    @InjectMocks
    private PageServiceImpl pageService;

    @Mock
    AuditService auditService;

    @Mock
    PageRepository pageRepository;

    @Mock
    PageRevisionService pageRevisionService;

    @Captor
    private ArgumentCaptor<Page> pageCaptor;

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
    @SneakyThrows
    public void create_page_should_not_save_content_in_audit() {
        ReflectionTestUtils.setField(pageService, "maxContentSize", -1);
        var aNewPageEntity = aNewPageEntity();
        var createdPage = aPage(aNewPageEntity);
        when(pageRepository.create(any())).thenReturn(createdPage);

        pageService.createPage(executionContext, API_ID, aNewPageEntity);

        verify(auditService).createApiAuditLog(
            any(ExecutionContext.class),
            eq(API_ID),
            any(),
            any(Audit.AuditEvent.class),
            any(),
            isNull(),
            pageCaptor.capture()
        );
        var auditPage = pageCaptor.getValue();
        assertThat(auditPage).isNotNull();
        assertThat(auditPage.getContent()).isNotNull();
        assertThat(auditPage.getContent().length()).isEqualTo(aNewPageEntity.getContent().length());
    }

    @Test
    @SneakyThrows
    public void create_page_should_limit_content_in_audit() {
        ReflectionTestUtils.setField(pageService, "maxContentSize", 10);
        var aNewPageEntity = aNewPageEntity();
        var createdPage = aPage(aNewPageEntity);
        when(pageRepository.create(any())).thenReturn(createdPage);

        pageService.createPage(executionContext, API_ID, aNewPageEntity);

        verify(auditService).createApiAuditLog(
            any(ExecutionContext.class),
            eq(API_ID),
            any(),
            any(Audit.AuditEvent.class),
            any(),
            isNull(),
            pageCaptor.capture()
        );
        var auditPage = pageCaptor.getValue();
        assertThat(auditPage).isNotNull();
        assertThat(auditPage.getContent()).isEqualTo(
            """
            {
              "opena..."""
        );
    }

    @Test
    @SneakyThrows
    public void create_page_should_force_null_content_in_audit() {
        ReflectionTestUtils.setField(pageService, "maxContentSize", 0);
        var aNewPageEntity = aNewPageEntity();
        var createdPage = aPage(aNewPageEntity);
        when(pageRepository.create(any())).thenReturn(createdPage);

        pageService.createPage(executionContext, API_ID, aNewPageEntity);

        verify(auditService).createApiAuditLog(
            any(ExecutionContext.class),
            eq(API_ID),
            any(),
            any(Audit.AuditEvent.class),
            any(),
            isNull(),
            pageCaptor.capture()
        );
        var auditPage = pageCaptor.getValue();
        assertThat(auditPage).isNotNull();
        assertThat(auditPage.getContent()).isNull();
    }

    private NewPageEntity aNewPageEntity() throws JsonProcessingException {
        var page = new NewPageEntity();
        page.setType(PageType.SWAGGER);
        page.setVisibility(Visibility.PUBLIC);
        page.setName("my awesome page");

        var source = new PageSourceEntity();
        page.setSource(source);

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0").description("A description"));
        openAPI.setServers(List.of());
        var descriptor = new OAIDescriptor(openAPI);
        page.setContent(descriptor.toJson());

        return page;
    }

    private Page aPage(NewPageEntity newPageEntity) {
        return Page.builder()
            .type(newPageEntity.getType().name())
            .visibility(newPageEntity.getVisibility().name())
            .name(newPageEntity.getName())
            .content(newPageEntity.getContent())
            .referenceType(PageReferenceType.API)
            .build();
    }
}
