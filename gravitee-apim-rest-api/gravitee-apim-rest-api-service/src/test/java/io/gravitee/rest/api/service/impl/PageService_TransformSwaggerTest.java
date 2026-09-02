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

import static io.gravitee.rest.api.model.PageType.MARKDOWN;
import static io.gravitee.rest.api.model.PageType.SWAGGER;
import static io.gravitee.rest.api.model.PageType.TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PageService_TransformSwaggerTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String PAGE_ID = "page-id";
    private static final String PARENT_PAGE_ID = "parent-page-id";
    private static final String OPENAPI_CONTENT = """
        {
          "openapi": "3.0.0",
          "info": {
            "title": "Rundeck API",
            "version": "1.0.0",
            "description": "-payload ${raw}"
          },
          "paths": {}
        }
        """;

    @InjectMocks
    private PageServiceImpl pageService;

    @Mock
    private SwaggerService swaggerService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private ApiEntrypointService apiEntrypointService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private PageRepository pageRepository;

    @Test
    void should_preserve_openapi_expression_when_transforming_swagger_page() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        PageEntity page = PageEntity.builder()
            .id(PAGE_ID)
            .type(SWAGGER.name())
            .content(OPENAPI_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .build();

        when(swaggerService.parse(OPENAPI_CONTENT)).thenReturn(new OAIParser().parse(OPENAPI_CONTENT));

        pageService.transformSwagger(executionContext, page, api);

        assertThat(page.getContent()).contains("${raw}");
        assertThat(page.getMessages()).isNullOrEmpty();
        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_preserve_known_template_expression_when_transforming_swagger_page() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        String openApiContent = OPENAPI_CONTENT.replace("${raw}", "${api.name}");
        PageEntity page = PageEntity.builder()
            .id(PAGE_ID)
            .type(SWAGGER.name())
            .content(openApiContent)
            .contentType(MediaType.APPLICATION_JSON)
            .build();

        when(swaggerService.parse(openApiContent)).thenReturn(new OAIParser().parse(openApiContent));

        pageService.transformSwagger(executionContext, page, api);

        assertThat(page.getContent()).contains("${api.name}");
        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_preserve_openapi_expression_when_transforming_environment_swagger_page() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        PageEntity page = PageEntity.builder().id(PAGE_ID).type(SWAGGER.name()).content(OPENAPI_CONTENT).build();

        pageService.transformWithTemplate(executionContext, page, null);

        assertThat(page.getContent()).isEqualTo(OPENAPI_CONTENT);
        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_preserve_openapi_expression_when_transforming_swagger_translation() throws TechnicalException {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Page parentPage = new Page();
        parentPage.setId(PARENT_PAGE_ID);
        parentPage.setType(SWAGGER.name());
        PageEntity translation = PageEntity.builder()
            .id(PAGE_ID)
            .parentId(PARENT_PAGE_ID)
            .type(TRANSLATION.name())
            .content(OPENAPI_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .build();

        when(pageRepository.findById(PARENT_PAGE_ID)).thenReturn(Optional.of(parentPage));
        when(swaggerService.parse(OPENAPI_CONTENT)).thenReturn(new OAIParser().parse(OPENAPI_CONTENT));

        pageService.transformSwagger(executionContext, translation, api);

        assertThat(translation.getContent()).contains("${raw}");
        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_validate_swagger_translation_as_openapi() throws TechnicalException {
        ReflectionTestUtils.setField(pageService, "swaggerValidateSafeContent", true);
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Page parentPage = new Page();
        parentPage.setId(PARENT_PAGE_ID);
        parentPage.setType(SWAGGER.name());
        PageEntity translation = PageEntity.builder()
            .id(PAGE_ID)
            .parentId(PARENT_PAGE_ID)
            .type(TRANSLATION.name())
            .content("{\"openapi\":\"3.0.0\"}")
            .build();

        when(pageRepository.findById(PARENT_PAGE_ID)).thenReturn(Optional.of(parentPage));

        var messages = pageService.validateSafeContent(executionContext, translation, API_ID);

        assertThat(messages).contains("attribute paths is missing");
    }

    @Test
    void should_resolve_template_when_transforming_markdown_page() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        PageEntity page = PageEntity.builder().id(PAGE_ID).type(MARKDOWN.name()).content("# ${api.name}").build();

        when(
            notificationTemplateService.resolveInlineTemplateWithParam(
                eq(ORGANIZATION_ID),
                eq(PAGE_ID),
                eq("# ${api.name}"),
                anyMap(),
                eq(false)
            )
        ).thenReturn("# Rundeck API");

        pageService.transformSwagger(executionContext, page, api);

        assertThat(page.getContent()).isEqualTo("# Rundeck API");
    }
}
