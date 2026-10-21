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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiService_ImportPathMappingsFromPageTest {

    private static final String API_ID = "api-id";
    private static final String PAGE_ID = "page-id";

    private final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

    @Mock
    private PageService pageService;

    @Mock
    private SwaggerService swaggerService;

    @Mock
    private ApiConverter apiConverter;

    @Spy
    @InjectMocks
    private ApiServiceImpl apiService;

    private ApiEntity api;

    @BeforeEach
    void setUp() {
        api = new ApiEntity();
        api.setId(API_ID);
        api.setPathMappings(new HashSet<>(Set.of("/existing")));
    }

    @Test
    void should_merge_path_mappings_from_a_swagger_page_of_the_api() {
        when(pageService.findById(PAGE_ID)).thenReturn(page(PageType.SWAGGER, "API", API_ID));
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setPathMappings(Set.of("/imported"));
        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(
            swaggerApi
        );
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        when(apiConverter.toUpdateApiEntity(api)).thenReturn(updateApiEntity);
        doReturn(api).when(apiService).update(executionContext, API_ID, updateApiEntity);

        ApiEntity result = apiService.importPathMappingsFromPage(executionContext, api, PAGE_ID, DefinitionVersion.V2);

        assertThat(result).isSameAs(api);
        assertThat(api.getPathMappings()).containsExactlyInAnyOrder("/existing", "/imported");
        ArgumentCaptor<ImportSwaggerDescriptorEntity> descriptor = ArgumentCaptor.forClass(ImportSwaggerDescriptorEntity.class);
        verify(swaggerService).createAPI(eq(executionContext), descriptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptor.getValue().getPayload()).isEqualTo("swagger content");
        assertThat(descriptor.getValue().isWithPathMapping()).isTrue();
    }

    @ParameterizedTest(name = "{0} page owned by {1}:{2}")
    @CsvSource(
        nullValues = "null",
        value = {
            "SWAGGER, API, another-api",
            "SWAGGER, ENVIRONMENT, DEFAULT",
            "SWAGGER, null, null",
            "SWAGGER, API, null",
            "SWAGGER, null, api-id",
            "MARKDOWN, API, another-api",
        }
    )
    void should_reject_a_page_not_owned_by_the_api(PageType type, String referenceType, String referenceId) {
        when(pageService.findById(PAGE_ID)).thenReturn(page(type, referenceType, referenceId));

        assertThatThrownBy(() -> apiService.importPathMappingsFromPage(executionContext, api, PAGE_ID, DefinitionVersion.V2)).isInstanceOf(
            PageNotFoundException.class
        );

        assertThat(api.getPathMappings()).containsExactly("/existing");
        verify(swaggerService, never()).createAPI(any(), any(), any());
        verify(apiService, never()).update(any(), any(), any(UpdateApiEntity.class));
    }

    @Test
    void should_reject_an_unknown_page() {
        when(pageService.findById(PAGE_ID)).thenThrow(new PageNotFoundException(PAGE_ID));

        assertThatThrownBy(() -> apiService.importPathMappingsFromPage(executionContext, api, PAGE_ID, DefinitionVersion.V2)).isInstanceOf(
            PageNotFoundException.class
        );

        verify(swaggerService, never()).createAPI(any(), any(), any());
        verify(apiService, never()).update(any(), any(), any(UpdateApiEntity.class));
    }

    private static PageEntity page(PageType type, String referenceType, String referenceId) {
        PageEntity page = new PageEntity();
        page.setId(PAGE_ID);
        page.setType(type.name());
        page.setContent("swagger content");
        page.setReferenceType(referenceType);
        page.setReferenceId(referenceId);
        return page;
    }
}
