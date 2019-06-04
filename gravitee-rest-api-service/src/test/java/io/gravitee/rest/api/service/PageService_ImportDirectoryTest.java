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
import io.gravitee.repository.management.model.PageType;
import io.gravitee.rest.api.management.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GraviteeDescriptorService;
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
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_ImportDirectoryTest {

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
    private GraviteeDescriptorService graviteeDescriptorService;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldImportDirectory() throws Exception {
        PageSourceEntity pageSource = new PageSourceEntity();
        pageSource.setType("type");
        pageSource.setConfiguration(mapper.readTree("{}"));
        ImportPageEntity pageEntity = new ImportPageEntity();
        pageEntity.setSource(pageSource);
        pageEntity.setPublished(true);
        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.PageService_ImportDirectoryMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockFilesFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportDirectoryMockFetcher> mockFetcherClass = PageService_ImportDirectoryMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockFilesFetcherConfiguration fetcherConfiguration = new PageService_MockFilesFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockFilesFetcherConfiguration.class), anyString())).thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);
        Page newPage = mock(Page.class);
        when(newPage.getId()).thenReturn(UUID.toString(UUID.random()));
        when(newPage.isPublished()).thenReturn(Boolean.TRUE);
        when(pageRepository.create(any())).thenReturn(newPage);
        when(graviteeDescriptorService.descriptorName()).thenReturn(".gravitee.json");

        List<PageEntity> pageEntities = pageService.importFiles(pageEntity);

        assertNotNull(pageEntities);
        assertEquals(8, pageEntities.size());

        verify(searchEngineService, times(8)).index(any(), eq(false));
        // //////////////////////
        // check Folder creation
        // //////////////////////
        verify(pageRepository, times(3)).create(argThat(pageToCreate -> PageType.FOLDER.equals(pageToCreate.getType())));

        // /src
        verify(pageRepository).create(argThat(pageToCreate -> "src".equals(pageToCreate.getName())
                && PageType.FOLDER.equals(pageToCreate.getType())
                && null == pageToCreate.getParentId()));
        // /src/doc
        verify(pageRepository).create(argThat(pageToCreate -> "doc".equals(pageToCreate.getName())
                && PageType.FOLDER.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));
        // /src/folder.with.dot/
        verify(pageRepository).create(argThat(pageToCreate -> "folder.with.dot".equals(pageToCreate.getName())
                && PageType.FOLDER.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));

        // //////////////////////
        // verify files creation
        // //////////////////////
        verify(pageRepository, times(5)).create(argThat(pageToCreate -> pageToCreate.getType() != null && !PageType.FOLDER.equals(pageToCreate.getType())));

        // /src/doc/m1.md
        verify(pageRepository).create(argThat(pageToCreate -> "m1".equals(pageToCreate.getName())
                && PageType.MARKDOWN.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));

        // /swagger.json
        verify(pageRepository).create(argThat(pageToCreate -> "swagger".equals(pageToCreate.getName())
                && PageType.SWAGGER.equals(pageToCreate.getType())
                && null == pageToCreate.getParentId()));

        // /src/doc/sub.m11.md
        verify(pageRepository).create(argThat(pageToCreate -> "sub.m11".equals(pageToCreate.getName())
                && PageType.MARKDOWN.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));

        // /src/doc/m2.yaml
        verify(pageRepository).create(argThat(pageToCreate -> "m2".equals(pageToCreate.getName())
                && PageType.SWAGGER.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));
        // /src/folder.with.dot/m2.MD
        verify(pageRepository).create(argThat(pageToCreate -> "m2".equals(pageToCreate.getName())
                && PageType.MARKDOWN.equals(pageToCreate.getType())
                && null != pageToCreate.getParentId()));

    }
}
