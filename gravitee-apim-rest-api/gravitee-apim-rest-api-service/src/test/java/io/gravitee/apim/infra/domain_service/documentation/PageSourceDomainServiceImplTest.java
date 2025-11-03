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

    private static PageSource dummySource() {
        return PageSource.builder()
            .type("dummy-fetcher")
            .configuration(
                """
                {
                   "sensitiveData" : I'm a sensitive data, I should be masked,
                   "nonSensitiveData" : "I'm not a sensitive data"
                }
                """
            )
            .build();
    }
}
