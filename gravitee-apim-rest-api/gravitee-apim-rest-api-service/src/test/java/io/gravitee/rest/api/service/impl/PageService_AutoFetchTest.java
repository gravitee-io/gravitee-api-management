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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
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
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_AutoFetchTest {

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PlanService planService;

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

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private Page mockPage;

    @Mock
    private Page mockRootPage;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldNotFetch_NoSourcePage() throws Exception {
        when(mockPage.getSource()).thenReturn(null);
        when(pageRepository.search(any())).thenReturn(Arrays.asList(mockPage));

        long pages = pageService.execAutoFetch(GraviteeContext.getCurrentEnvironment());
        assertEquals(0, pages);

        verify(pageRepository, times(0)).update(any());
        verify(pageRepository, times(0)).create(any());
    }

    @Test
    public void shouldNotFetch_SourcePage_NoAutoFetch() throws Exception {
        PageSource pageSource = new PageSource();
        pageSource.setType("type");
        pageSource.setConfiguration("{}");
        when(mockPage.getSource()).thenReturn(pageSource);
        when(pageRepository.search(any())).thenReturn(Arrays.asList(mockPage));

        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.impl.PageService_ImportDescriptorMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockDescriptorFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportDescriptorMockFetcher> mockFetcherClass = PageService_ImportDescriptorMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockDescriptorFetcherConfiguration fetcherConfiguration = new PageService_MockDescriptorFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockDescriptorFetcherConfiguration.class), anyString()))
            .thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);

        long pages = pageService.execAutoFetch(GraviteeContext.getCurrentEnvironment());
        assertEquals(0, pages);

        verify(pageRepository, times(0)).update(any());
        verify(pageRepository, times(0)).create(any());
    }

    @Test
    public void shouldFetch_SourcePage_AutoFetch() throws Exception {
        PageSource pageSource = new PageSource();

        pageSource.setType("type");
        pageSource.setConfiguration("{\"autoFetch\": true, \"fetchCron\" : \"* * * * * *\"}");
        when(mockPage.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(mockPage.getReferenceId()).thenReturn("envId");
        when(mockPage.getSource()).thenReturn(pageSource);
        when(mockPage.getUpdatedAt()).thenReturn(new Date(Instant.now().minus(2, ChronoUnit.SECONDS).toEpochMilli()));
        when(mockPage.getVisibility()).thenReturn("PUBLIC");
        when(pageRepository.search(any())).thenReturn(Arrays.asList(mockPage));
        when(pageRepository.update(any())).thenReturn(mockPage);

        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.impl.PageService_ImportSimplePageMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockSinglePageFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportSimplePageMockFetcher> mockFetcherClass = PageService_ImportSimplePageMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockSinglePageFetcherConfiguration fetcherConfiguration = new PageService_MockSinglePageFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockSinglePageFetcherConfiguration.class), anyString()))
            .thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);

        long pages = pageService.execAutoFetch(GraviteeContext.getCurrentEnvironment());
        assertEquals(1, pages);

        verify(pageRepository, times(1)).update(any());
        verify(pageRepository, times(0)).create(any());
    }

    @Test
    public void shouldNotFetch_SourcePage_AutoFetch_NotRequiredYet() throws Exception {
        PageSource pageSource = new PageSource();
        pageSource.setType("type");
        pageSource.setConfiguration("{\"autoFetch\": true, \"fetchCron\" : \"* 10 * * * *\"}");
        when(mockPage.getSource()).thenReturn(pageSource);
        when(mockPage.getUpdatedAt()).thenReturn(new Date(Instant.now().toEpochMilli()));
        when(pageRepository.search(any())).thenReturn(Arrays.asList(mockPage));

        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.impl.PageService_ImportSimplePageMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockSinglePageFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportSimplePageMockFetcher> mockFetcherClass = PageService_ImportSimplePageMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockSinglePageFetcherConfiguration fetcherConfiguration = new PageService_MockSinglePageFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockSinglePageFetcherConfiguration.class), anyString()))
            .thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);
        PageService_MockSinglePageFetcherConfiguration.forceCronValue("* 10 * * * *");

        long pages = pageService.execAutoFetch(GraviteeContext.getCurrentEnvironment());
        assertEquals(0, pages);

        verify(pageRepository, times(0)).update(any());
        verify(pageRepository, times(0)).create(any());
    }

    @Test
    public void shouldNotFetch_Descriptor_AutoFetch() throws Exception {
        PageSource pageSource = new PageSource();
        pageSource.setType("type");
        pageSource.setConfiguration("{\"autoFetch\": true, \"fetchCron\" : \"* * * * * *\"}");

        when(mockRootPage.getSource()).thenReturn(pageSource);
        when(mockRootPage.getReferenceId()).thenReturn("apiid");
        when(mockRootPage.getType()).thenReturn(PageType.ROOT.name());
        when(mockRootPage.getUpdatedAt()).thenReturn(new Date(Instant.now().minus(2, ChronoUnit.SECONDS).toEpochMilli()));
        when(mockRootPage.getVisibility()).thenReturn("PUBLIC");

        when(mockPage.getSource()).thenReturn(pageSource);
        when(mockPage.getId()).thenReturn("someid");
        when(mockPage.getReferenceId()).thenReturn("apiid");
        when(mockPage.getReferenceType()).thenReturn(PageReferenceType.API);
        when(mockPage.getType()).thenReturn(PageType.MARKDOWN.name());
        when(mockPage.getUpdatedAt()).thenReturn(new Date(Instant.now().minus(2, ChronoUnit.SECONDS).toEpochMilli()));
        when(mockPage.getVisibility()).thenReturn("PUBLIC");

        when(pageRepository.findById(any())).thenReturn(Optional.of(mockPage));
        when(pageRepository.update(any())).thenReturn(mockPage);
        when(pageRepository.search(any())).thenReturn(Arrays.asList(mockRootPage)).thenReturn(Arrays.asList(mockPage));

        FetcherPlugin fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.rest.api.service.impl.PageService_ImportAutFetchDescriptorMockFetcher");
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockAutoFetchDescriptorFetcherConfiguration.class);
        when(fetcherPluginManager.get(any())).thenReturn(fetcherPlugin);
        Class<PageService_ImportAutFetchDescriptorMockFetcher> mockFetcherClass = PageService_ImportAutFetchDescriptorMockFetcher.class;
        when(fetcherPlugin.fetcher()).thenReturn(mockFetcherClass);
        PageService_MockAutoFetchDescriptorFetcherConfiguration fetcherConfiguration = new PageService_MockAutoFetchDescriptorFetcherConfiguration();
        when(fetcherConfigurationFactory.create(eq(PageService_MockAutoFetchDescriptorFetcherConfiguration.class), anyString()))
            .thenReturn(fetcherConfiguration);
        AutowireCapableBeanFactory mockAutowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mockAutowireCapableBeanFactory);
        when(pageRepository.update(any())).thenReturn(mockPage);
        when(graviteeDescriptorService.descriptorName()).thenReturn(".gravitee.json");
        when(graviteeDescriptorService.read(anyString())).thenCallRealMethod();

        pageService.execAutoFetch(GraviteeContext.getCurrentEnvironment());
        verify(pageRepository, times(11)).update(any());
    }
}
