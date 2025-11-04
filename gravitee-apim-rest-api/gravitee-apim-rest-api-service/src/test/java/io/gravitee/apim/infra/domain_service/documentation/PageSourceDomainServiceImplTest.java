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
package io.gravitee.apim.infra.domain_service.documentation;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class PageSourceDomainServiceImplTest {

    @Mock
    FetcherConfigurationFactory fetcherConfigurationFactory;

    @Mock
    PluginManager<FetcherPlugin<?>> pluginManager;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    @SuppressWarnings("rawtypes")
    FetcherPlugin fetcherPlugin;

    PageSourceDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new PageSourceDomainServiceImpl(fetcherConfigurationFactory, pluginManager, applicationContext);
    }

    @Test
    void should_not_fetch_content_if_no_page_source() {
        cut.setContentFromSource(Page.builder().build());
        verifyNoInteractions(fetcherConfigurationFactory, pluginManager, applicationContext);
    }

    @Test
    void should_not_fetch_content_if_no_plugin() {
        when(pluginManager.get(any())).thenReturn(null);
        cut.setContentFromSource(Page.builder().source(dummySource()).build());
        verifyNoInteractions(fetcherConfigurationFactory, applicationContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_report_build_fetcher_error() {
        var error = assertThrows(TechnicalDomainException.class, () -> {
            when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);
            cut.setContentFromSource(Page.builder().source(dummySource()).build());
        });
        assertThat(error).hasMessage("unable to build fetcher instance");
    }

    @Test
    void should_remove_sensitive_data() throws JsonProcessingException {
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(
            mock(org.springframework.beans.factory.config.AutowireCapableBeanFactory.class)
        );
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.apim.infra.domain_service.documentation.DummyFetcher");
        when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);
        when(fetcherConfigurationFactory.create(any(), any())).thenReturn(
            new DummyFetcherConfiguration("I'm not a sensitive data", "I'm a sensitive data, I should be masked")
        );
        Page page = Page.builder().source(dummySource()).build();
        cut.removeSensitiveData(page);
        JsonNode configuration = new ObjectMapper().readTree(page.getSource().getConfiguration());
        assertThat(configuration.get("sensitiveData").textValue()).isEqualTo(PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT);
        assertThat(configuration.get("nonSensitiveData").textValue()).isEqualTo("I'm not a sensitive data");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_merge_sensitive_data_from_old_page_to_new_page() throws JsonProcessingException {
        // Arrange
        String originalSensitiveData = "original-secret-token";
        String maskedSensitiveData = PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT;
        String nonSensitiveData = "I'm not a sensitive data";

        PageSource oldPageSource = dummySource(nonSensitiveData, originalSensitiveData);
        PageSource newPageSource = dummySource(nonSensitiveData, maskedSensitiveData);

        Page oldPage = Page.builder().source(oldPageSource).build();
        Page newPage = Page.builder().source(newPageSource).build();

        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(
            mock(org.springframework.beans.factory.config.AutowireCapableBeanFactory.class)
        );
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.apim.infra.domain_service.documentation.DummyFetcher");
        when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);

        // Mock fetcher configuration factory to return appropriate configurations
        // First call for old page, second call for new page
        when(fetcherConfigurationFactory.create(any(), any()))
            .thenReturn(new DummyFetcherConfiguration(nonSensitiveData, originalSensitiveData))
            .thenReturn(new DummyFetcherConfiguration(nonSensitiveData, maskedSensitiveData));

        // Act
        cut.mergeSensitiveData(oldPage, newPage);

        // Assert
        JsonNode configuration = new ObjectMapper().readTree(newPage.getSource().getConfiguration());
        assertThat(configuration.get("sensitiveData").textValue()).isEqualTo(originalSensitiveData);
        assertThat(configuration.get("nonSensitiveData").textValue()).isEqualTo(nonSensitiveData);
    }

    @Test
    void should_not_merge_when_old_page_has_no_source() {
        Page oldPage = Page.builder().build();
        Page newPage = Page.builder().source(dummySource()).build();

        cut.mergeSensitiveData(oldPage, newPage);

        verifyNoInteractions(fetcherConfigurationFactory, pluginManager, applicationContext);
    }

    @Test
    void should_not_merge_when_new_page_has_no_source() {
        Page oldPage = Page.builder().source(dummySource()).build();
        Page newPage = Page.builder().build();

        cut.mergeSensitiveData(oldPage, newPage);

        verifyNoInteractions(fetcherConfigurationFactory, pluginManager, applicationContext);
    }

    @Test
    void should_not_merge_when_sensitive_data_is_not_masked() throws JsonProcessingException {
        // Arrange
        String originalSensitiveData = "original-secret-token";
        String newSensitiveData = "new-secret-token";
        String nonSensitiveData = "I'm not a sensitive data";

        PageSource oldPageSource = dummySource(nonSensitiveData, originalSensitiveData);
        PageSource newPageSource = dummySource(nonSensitiveData, newSensitiveData);

        Page oldPage = Page.builder().source(oldPageSource).build();
        Page newPage = Page.builder().source(newPageSource).build();

        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(
            mock(org.springframework.beans.factory.config.AutowireCapableBeanFactory.class)
        );
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.apim.infra.domain_service.documentation.DummyFetcher");
        when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);

        // Mock fetcher configuration factory to return appropriate configurations
        // First call for old page, second call for new page
        when(fetcherConfigurationFactory.create(any(), any()))
            .thenReturn(new DummyFetcherConfiguration(nonSensitiveData, originalSensitiveData))
            .thenReturn(new DummyFetcherConfiguration(nonSensitiveData, newSensitiveData));

        // Act
        cut.mergeSensitiveData(oldPage, newPage);

        // Assert - new sensitive data should remain unchanged since it's not masked
        JsonNode configuration = new ObjectMapper().readTree(newPage.getSource().getConfiguration());
        assertThat(configuration.get("sensitiveData").textValue()).isEqualTo(newSensitiveData);
        assertThat(configuration.get("nonSensitiveData").textValue()).isEqualTo(nonSensitiveData);
    }

    private static PageSource dummySource() {
        return dummySource("I'm not a sensitive data", "I'm a sensitive data, I should be masked");
    }

    private static PageSource dummySource(String nonSensitiveData, String sensitiveData) {
        return PageSource.builder()
            .type("dummy-fetcher")
            .configuration(
                String.format(
                    """
                    {
                       "nonSensitiveData" : "%s",
                       "sensitiveData" : "%s"
                    }
                    """,
                    nonSensitiveData,
                    sensitiveData
                )
            )
            .build();
    }
}
