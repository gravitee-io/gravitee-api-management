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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_ImportDescriptorTest {

    @InjectMocks
    private PageServiceImpl pageService = Mockito.mock(PageServiceImpl.class, CALLS_REAL_METHODS);

    @Mock
    private PageRepository pageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private PluginManager<FetcherPlugin> fetcherPluginManager;

    @Mock
    private FetcherConfigurationFactory fetcherConfigurationFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private GraviteeDescriptorServiceImpl graviteeDescriptorService;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private ObjectMapper mockMapper;

    @Mock
    private ImportConfiguration importConfiguration;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldImportDescriptor() throws Exception {
        // We mock the validateSafeContent method because the fetcher keeps sending the same json descriptor which is
        // not a swagger valid document (and modify the fetcher mock to produce valid desc is overkill)
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId");
        when(pageService.validateSafeContent(eq(executionContext), any(), any())).thenReturn(new ArrayList<>());

        PageSourceEntity pageSource = new PageSourceEntity();
        pageSource.setType("type");
        pageSource.setConfiguration(mapper.readTree("{}"));
        ImportPageEntity pageEntity = new ImportPageEntity();
        pageEntity.setSource(pageSource);
        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.impl.PageService_ImportDescriptorMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockDescriptorFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportDescriptorMockFetcher> mockFetcherClass = PageService_ImportDescriptorMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockDescriptorFetcherConfiguration fetcherConfiguration = new PageService_MockDescriptorFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockDescriptorFetcherConfiguration.class), anyString())).thenReturn(
            fetcherConfiguration
        );
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);
        Page newPage = mock(Page.class);
        PageSource ps = new PageSource();
        ps.setType(pageSource.getType());
        ps.setConfiguration(pageSource.getConfiguration());
        when(newPage.getId()).thenReturn(UuidString.generateRandom());
        when(newPage.getSource()).thenReturn(ps);
        when(newPage.getType()).thenReturn("MARKDOWN");
        when(newPage.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(newPage.getReferenceId()).thenReturn("envId");
        when(newPage.getVisibility()).thenReturn("PUBLIC");
        when(newPage.toBuilder()).thenReturn(new Page().toBuilder());
        when(pageRepository.create(any())).thenReturn(newPage);
        when(graviteeDescriptorService.descriptorName()).thenReturn(".gravitee.json");
        when(graviteeDescriptorService.read(anyString())).thenCallRealMethod();

        List<PageEntity> pageEntities = pageService.importFiles(executionContext, pageEntity);

        assertNotNull(pageEntities);
        assertEquals(8, pageEntities.size());
        verify(pageRevisionService, times(5)).create(any());
    }

    @Test
    @SneakyThrows
    public void shouldNotValidateSafeContent() {
        ReflectionTestUtils.setField(pageService, "swaggerValidateSafeContent", false);
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId");
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0").description("A description"));
        openAPI.setServers(List.of());
        OAIDescriptor descriptor = new OAIDescriptor(openAPI);
        PageEntity pageEntity = PageEntity.builder().type(PageType.SWAGGER.name()).content(descriptor.toJson()).build();

        var result = pageService.validateSafeContent(executionContext, pageEntity, "api-id");

        assertThat(result).isEmpty();
    }

    @Test
    @SneakyThrows
    public void shouldValidateSafeContent() {
        ReflectionTestUtils.setField(pageService, "swaggerValidateSafeContent", true);
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId");

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0").description("A description"));
        openAPI.setServers(List.of());
        OAIDescriptor descriptor = new OAIDescriptor(openAPI);
        PageEntity pageEntity = PageEntity.builder().type(PageType.SWAGGER.name()).content(descriptor.toJson()).build();

        var result = pageService.validateSafeContent(executionContext, pageEntity, "api-id");

        assertThat(result).contains("attribute paths is missing");
    }
}
