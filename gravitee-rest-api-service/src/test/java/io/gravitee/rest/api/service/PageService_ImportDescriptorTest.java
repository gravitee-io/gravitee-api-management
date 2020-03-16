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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.impl.GraviteeDescriptorServiceImpl;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_ImportDescriptorTest {

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

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
    private ObjectMapper mockMapper;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldImportDescriptor() throws Exception {
        PageSourceEntity pageSource = new PageSourceEntity();
        pageSource.setType("type");
        pageSource.setConfiguration(mapper.readTree("{}"));
        ImportPageEntity pageEntity = new ImportPageEntity();
        pageEntity.setSource(pageSource);
        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.PageService_ImportDescriptorMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockDescriptorFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportDescriptorMockFetcher> mockFetcherClass = PageService_ImportDescriptorMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockDescriptorFetcherConfiguration fetcherConfiguration = new PageService_MockDescriptorFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockDescriptorFetcherConfiguration.class), anyString())).thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);
        Page newPage = mock(Page.class);
        when(newPage.getId()).thenReturn(UUID.toString(UUID.random()));
        when(pageRepository.create(any())).thenReturn(newPage);
        when(graviteeDescriptorService.descriptorName()).thenReturn(".gravitee.json");
        when(graviteeDescriptorService.read(anyString())).thenCallRealMethod();

        List<PageEntity> pageEntities = pageService.importFiles(pageEntity);


        assertNotNull(pageEntities);
        assertEquals(8, pageEntities.size());
    }
}
