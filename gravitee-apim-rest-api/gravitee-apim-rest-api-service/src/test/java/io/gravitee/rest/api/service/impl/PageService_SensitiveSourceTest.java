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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PageService_SensitiveSourceTest {

    private static final String PAGE_ID = "page-id";
    private static final String API_ID = "api-id";
    private static final String FETCHER_TYPE = "sensitive-fetcher";
    private static final String MASKED = "********";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PluginManager<FetcherPlugin> fetcherPluginManager;

    @Mock
    private FetcherConfigurationFactory fetcherConfigurationFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private PageRevisionService pageRevisionService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pageService.setObjectMapper(JSON_MAPPER);
        var fetcherPlugin = mock(FetcherPlugin.class);
        when(fetcherPlugin.fetcher()).thenReturn(PageService_MockSensitiveFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(PageService_MockSensitiveFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn(PageService_MockSensitiveFetcher.class.getName());
        when(fetcherPluginManager.get(FETCHER_TYPE)).thenReturn(fetcherPlugin);
        when(fetcherConfigurationFactory.create(any(), anyString())).thenAnswer(invocation ->
            JSON_MAPPER.readValue(invocation.<String>getArgument(1), invocation.<Class<?>>getArgument(0))
        );
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mock(AutowireCapableBeanFactory.class));
    }

    @Test
    void should_reject_an_update_keeping_a_masked_secret_for_another_address() throws TechnicalException {
        givenStoredPage("{\"url\":\"https://a.example/doc.md\",\"token\":\"stored-secret\"}");

        assertThatThrownBy(() ->
            pageService.update(
                EXECUTION_CONTEXT,
                PAGE_ID,
                updateWithSource("{\"url\":\"https://b.example/doc.md\",\"token\":\"" + MASKED + "\"}")
            )
        )
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("token")
            .hasMessageContaining("actual value")
            .hasMessageNotContaining("b.example")
            .hasMessageNotContaining("stored-secret");
        verify(pageRepository, never()).update(any());
    }

    @Test
    void should_restore_the_stored_secret_when_only_non_address_fields_change() throws Exception {
        givenStoredPage("{\"url\":\"https://a.example/doc.md\",\"branch\":\"main\",\"token\":\"stored-secret\"}");
        when(pageRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        pageService.update(
            EXECUTION_CONTEXT,
            PAGE_ID,
            updateWithSource("{\"url\":\"https://a.example/doc.md\",\"branch\":\"dev\",\"token\":\"" + MASKED + "\"}")
        );

        assertThat(persistedToken()).isEqualTo("stored-secret");
    }

    @Test
    void should_accept_a_masked_value_with_no_stored_secret_when_the_address_changes() throws Exception {
        // Classic pages mask a sensitive field even when it holds nothing, so a public source comes back masked
        givenStoredPage("{\"url\":\"https://a.example/doc.md\"}");
        when(pageRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        pageService.update(
            EXECUTION_CONTEXT,
            PAGE_ID,
            updateWithSource("{\"url\":\"https://b.example/doc.md\",\"token\":\"" + MASKED + "\"}")
        );

        assertThat(persistedToken()).isNull();
    }

    @Test
    void should_keep_a_new_secret_when_the_address_changes() throws Exception {
        givenStoredPage("{\"url\":\"https://a.example/doc.md\",\"token\":\"stored-secret\"}");
        when(pageRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        pageService.update(EXECUTION_CONTEXT, PAGE_ID, updateWithSource("{\"url\":\"https://b.example/doc.md\",\"token\":\"new-secret\"}"));

        assertThat(persistedToken()).isEqualTo("new-secret");
    }

    @Test
    void should_restore_the_stored_secret_on_a_root_page_import_for_the_same_address() throws Exception {
        givenStoredRootPage("{\"url\":\"https://a.example/docs\",\"token\":\"stored-secret\"}");
        when(pageRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var rootPage = new ImportPageEntity();
        rootPage.setType(PageType.ROOT);
        rootPage.setSource(pageSourceEntity("{\"url\":\"https://a.example/docs\",\"token\":\"" + MASKED + "\"}"));

        pageService.importFiles(EXECUTION_CONTEXT, API_ID, rootPage);

        assertThat(persistedToken()).isEqualTo("stored-secret");
    }

    @Test
    void should_reject_a_root_page_import_keeping_a_masked_secret_for_another_address() throws TechnicalException {
        givenStoredRootPage("{\"url\":\"https://a.example/docs\",\"token\":\"stored-secret\"}");
        var rootPage = new ImportPageEntity();
        rootPage.setType(PageType.ROOT);
        rootPage.setSource(pageSourceEntity("{\"url\":\"https://b.example/docs\",\"token\":\"" + MASKED + "\"}"));

        assertThatThrownBy(() -> pageService.importFiles(EXECUTION_CONTEXT, API_ID, rootPage))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageNotContaining("b.example")
            .hasMessageNotContaining("stored-secret");
        verify(pageRepository, never()).update(any());
    }

    private void givenStoredRootPage(String sourceConfiguration) throws TechnicalException {
        var storedRoot = new Page();
        storedRoot.setId("root-id");
        storedRoot.setType(PageType.ROOT.name());
        storedRoot.setSource(pageSource(sourceConfiguration));
        when(pageRepository.search(any())).thenReturn(List.of(storedRoot));
    }

    private void givenStoredPage(String sourceConfiguration) throws TechnicalException {
        var stored = new Page();
        stored.setId(PAGE_ID);
        stored.setName("doc");
        stored.setType(PageType.MARKDOWN.name());
        stored.setContent("content");
        stored.setOrder(0);
        stored.setReferenceType(PageReferenceType.API);
        stored.setReferenceId(API_ID);
        stored.setVisibility("PUBLIC");
        stored.setSource(pageSource(sourceConfiguration));
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(stored));
    }

    private static UpdatePageEntity updateWithSource(String sourceConfiguration) throws Exception {
        var update = new UpdatePageEntity();
        update.setName("doc");
        update.setContent("content");
        update.setOrder(0);
        update.setSource(pageSourceEntity(sourceConfiguration));
        return update;
    }

    private String persistedToken() throws Exception {
        var captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).update(captor.capture());
        var token = JSON_MAPPER.readTree(captor.getValue().getSource().getConfiguration()).get("token");
        return token == null || token.isNull() ? null : token.textValue();
    }

    private static PageSource pageSource(String configuration) {
        var source = new PageSource();
        source.setType(FETCHER_TYPE);
        source.setConfiguration(configuration);
        return source;
    }

    private static PageSourceEntity pageSourceEntity(String configuration) {
        var source = new PageSourceEntity();
        source.setType(FETCHER_TYPE);
        try {
            source.setConfiguration(JSON_MAPPER.readTree(configuration));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return source;
    }
}
