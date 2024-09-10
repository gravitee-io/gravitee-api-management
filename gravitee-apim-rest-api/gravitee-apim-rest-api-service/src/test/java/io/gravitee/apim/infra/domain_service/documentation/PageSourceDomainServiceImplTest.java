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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.documentation.exception.InvalidPageSourceException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.util.HashMap;
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
        cut.setContentFromSource(Page.builder().source(httpSource()).build());
        verifyNoInteractions(fetcherConfigurationFactory, applicationContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_report_build_fetcher_error() {
        var error = assertThrows(
            TechnicalDomainException.class,
            () -> {
                when(pluginManager.get("http-fetcher")).thenReturn(fetcherPlugin);
                cut.setContentFromSource(Page.builder().source(httpSource()).build());
            }
        );
        assertThat(error).hasMessage("unable to build fetcher instance");
    }

    private static PageSource httpSource() {
        return PageSource
            .builder()
            .type("http-fetcher")
            .configuration(
                """
                {
                   "autoFetch" : true,
                   "fetchCron" : "*/10 * * * * *",
                   "url" : "https://raw.githubusercontent.com/a-cordier/hands-on-gko/main/README.md"
                }
                """
            )
            .build();
    }
}
