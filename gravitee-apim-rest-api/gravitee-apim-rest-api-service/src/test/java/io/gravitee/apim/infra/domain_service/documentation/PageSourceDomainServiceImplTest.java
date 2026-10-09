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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.exception.InvalidPageSourceException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.ResourceNotFoundException;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
    @SuppressWarnings("unchecked")
    void should_read_content_and_close_stream() throws Exception {
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(
            mock(org.springframework.beans.factory.config.AutowireCapableBeanFactory.class)
        );
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.apim.infra.domain_service.documentation.DummyFetcher");
        when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);
        when(fetcherConfigurationFactory.create(any(), any())).thenReturn(new DummyFetcherConfiguration("data", "secret"));

        var trackingStream = new TrackingInputStream(new ByteArrayInputStream("dummy content".getBytes()));
        DummyFetcher.nextStream.set(trackingStream);

        Page page = Page.builder().source(dummySource()).build();
        cut.setContentFromSource(page);

        assertThat(page.getContent()).isEqualTo("dummy content");
        assertThat(trackingStream.closed).isTrue();
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

    @Test
    void should_reject_a_masked_secret_when_the_address_changes() {
        var oldPage = pageWithSource("{\"url\":\"https://a.example/doc.md\",\"sensitiveData\":\"original-secret-token\"}");
        var newPage = pageWithSource(
            "{\"url\":\"https://b.example/doc.md\",\"sensitiveData\":\"" + PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT + "\"}"
        );
        mockDummyFetcherPlugin(
            new DummyFetcherConfiguration(null, "original-secret-token"),
            new DummyFetcherConfiguration(null, PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT)
        );

        assertThatThrownBy(() -> cut.mergeSensitiveData(oldPage, newPage))
            .isInstanceOf(InvalidPageSourceException.class)
            .hasMessageContaining("sensitiveData")
            .hasMessageContaining("actual value")
            .hasMessageNotContaining("b.example")
            .hasMessageNotContaining("original-secret-token");
    }

    @Test
    void should_merge_sensitive_data_when_only_non_address_fields_change() throws JsonProcessingException {
        var oldPage = pageWithSource(
            "{\"url\":\"https://a.example\",\"nonSensitiveData\":\"main\",\"sensitiveData\":\"original-secret-token\"}"
        );
        var newPage = pageWithSource(
            "{\"url\":\"https://a.example\",\"nonSensitiveData\":\"dev\",\"sensitiveData\":\"" +
                PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT +
                "\"}"
        );
        mockDummyFetcherPlugin(
            new DummyFetcherConfiguration("main", "original-secret-token"),
            new DummyFetcherConfiguration("dev", PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT)
        );

        cut.mergeSensitiveData(oldPage, newPage);

        JsonNode configuration = new ObjectMapper().readTree(newPage.getSource().getConfiguration());
        assertThat(configuration.get("sensitiveData").textValue()).isEqualTo("original-secret-token");
    }

    @Test
    void should_accept_a_masked_value_with_no_stored_secret_when_the_address_changes() throws JsonProcessingException {
        // Classic pages mask a sensitive field even when it holds nothing, so a public source comes back masked
        var oldPage = pageWithSource("{\"url\":\"https://a.example\",\"sensitiveData\":null}");
        var newPage = pageWithSource(
            "{\"url\":\"https://b.example\",\"sensitiveData\":\"" + PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT + "\"}"
        );
        mockDummyFetcherPlugin(
            new DummyFetcherConfiguration(null, null),
            new DummyFetcherConfiguration(null, PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT)
        );

        cut.mergeSensitiveData(oldPage, newPage);

        JsonNode configuration = new ObjectMapper().readTree(newPage.getSource().getConfiguration());
        assertThat(configuration.get("sensitiveData").isNull()).isTrue();
    }

    @Test
    void should_keep_a_new_secret_when_the_address_changes() {
        var oldPage = pageWithSource("{\"url\":\"https://a.example\",\"sensitiveData\":\"original-secret-token\"}");
        var newConfiguration = "{\"url\":\"https://b.example\",\"sensitiveData\":\"new-secret-token\"}";
        var newPage = pageWithSource(newConfiguration);
        mockDummyFetcherPlugin(
            new DummyFetcherConfiguration(null, "original-secret-token"),
            new DummyFetcherConfiguration(null, "new-secret-token")
        );

        cut.mergeSensitiveData(oldPage, newPage);

        assertThat(newPage.getSource().getConfiguration()).isEqualTo(newConfiguration);
    }

    @Test
    void should_not_return_the_stored_secret_when_the_fetch_fails_after_a_masked_update() {
        var oldPage = pageWithSource(
            "{\"url\":\"https://a.example\",\"nonSensitiveData\":\"doc.md\",\"sensitiveData\":\"stored-secret-token\"}"
        );
        var newPage = pageWithSource(
            "{\"url\":\"https://a.example\",\"nonSensitiveData\":\"missing.md\",\"sensitiveData\":\"" +
                PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT +
                "\"}"
        );
        mockDummyFetcherPlugin(
            new DummyFetcherConfiguration("doc.md", "stored-secret-token"),
            new DummyFetcherConfiguration("missing.md", PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT)
        );
        DummyFetcher.nextFailure.set(new ResourceNotFoundException("Unable to find file 'missing.md'", null));

        cut.mergeSensitiveData(oldPage, newPage);

        assertThatThrownBy(() -> cut.setContentFromSource(newPage))
            .hasMessageNotContaining("stored-secret-token")
            .hasMessageNotContaining("sensitiveData");
    }

    @Test
    void should_not_return_a_typed_secret_when_the_fetch_fails_on_create() {
        var page = pageWithSource("{\"nonSensitiveData\":\"missing.md\",\"sensitiveData\":\"typed-secret-token\"}");
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("missing.md", "typed-secret-token"));
        DummyFetcher.nextFailure.set(new ResourceNotFoundException("Unable to find file 'missing.md'", null));

        assertThatThrownBy(() -> cut.setContentFromSource(page))
            .hasMessageNotContaining("typed-secret-token")
            .hasMessageNotContaining("sensitiveData");
    }

    @Test
    void should_reject_a_missing_file_as_an_invalid_source() {
        var page = pageWithSource("{\"nonSensitiveData\":\"missing.md\",\"sensitiveData\":\"typed-secret-token\"}");
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("missing.md", "typed-secret-token"));
        var notFound = new ResourceNotFoundException("Unable to find file 'missing.md' in repository 'https://a.example/repo.git'", null);
        DummyFetcher.nextFailure.set(notFound);

        assertThatThrownBy(() -> cut.setContentFromSource(page))
            .isInstanceOf(InvalidPageSourceException.class)
            .hasMessage(
                "Unable to fetch the page content from source [dummy-fetcher]: Unable to find file 'missing.md' in repository 'https://a.example/repo.git'"
            )
            .hasCause(notFound);
    }

    @Test
    void should_report_a_fetch_failure_as_technical_with_its_cause() {
        var page = pageWithSource("{\"nonSensitiveData\":\"doc.md\",\"sensitiveData\":\"typed-secret-token\"}");
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("doc.md", "typed-secret-token"));
        var authenticationFailure = new FetcherException(
            "Unable to fetch git content: authentication failed for repository 'https://a.example/repo.git'",
            null
        );
        DummyFetcher.nextFailure.set(authenticationFailure);

        assertThatThrownBy(() -> cut.setContentFromSource(page))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessage(
                "Unable to fetch the page content from source [dummy-fetcher]: Unable to fetch git content: authentication failed for repository 'https://a.example/repo.git'"
            )
            .hasCause(authenticationFailure);
    }

    @Test
    void should_redact_a_secret_echoed_by_the_fetcher() {
        var page = pageWithSource("{\"nonSensitiveData\":\"doc.md\",\"sensitiveData\":\"typed-secret-token\"}");
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("doc.md", "typed-secret-token"));
        DummyFetcher.nextFailure.set(new FetcherException("Token typed-secret-token was rejected", null));

        assertThatThrownBy(() -> cut.setContentFromSource(page))
            .hasMessageEndingWith("Token " + PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT + " was rejected")
            .hasMessageNotContaining("typed-secret-token");
    }

    @Test
    void should_report_a_content_read_failure_without_the_configuration() {
        var page = pageWithSource("{\"nonSensitiveData\":\"doc.md\",\"sensitiveData\":\"typed-secret-token\"}");
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("doc.md", "typed-secret-token"));
        DummyFetcher.nextStream.set(
            new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Connection reset");
                }
            }
        );

        assertThatThrownBy(() -> cut.setContentFromSource(page))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessage("Unable to read the page content fetched from source [dummy-fetcher]")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void should_log_the_fetch_failure_without_the_secret() {
        var page = Page.builder()
            .id("page-id")
            .source(
                PageSource.builder()
                    .type("dummy-fetcher")
                    .configuration("{\"nonSensitiveData\":\"doc.md\",\"sensitiveData\":\"typed-secret-token\"}")
                    .build()
            )
            .build();
        mockDummyFetcherPlugin(new DummyFetcherConfiguration("doc.md", "typed-secret-token"));
        DummyFetcher.nextFailure.set(new FetcherException("Token typed-secret-token was rejected", null));
        var logger = (Logger) LoggerFactory.getLogger(PageSourceDomainServiceImpl.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> cut.setContentFromSource(page)).isInstanceOf(TechnicalDomainException.class);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
            .filteredOn(event -> event.getLevel() == Level.WARN)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getFormattedMessage())
                    .contains("page-id", "dummy-fetcher", "was rejected")
                    .doesNotContain("typed-secret-token");
                assertThat(event.getThrowableProxy()).isNull();
            });
    }

    private void mockDummyFetcherPlugin(DummyFetcherConfiguration configuration) {
        mockDummyFetcherPlugin(configuration, configuration);
    }

    @SuppressWarnings("unchecked")
    private void mockDummyFetcherPlugin(DummyFetcherConfiguration oldConfiguration, DummyFetcherConfiguration newConfiguration) {
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mock(AutowireCapableBeanFactory.class));
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn("io.gravitee.apim.infra.domain_service.documentation.DummyFetcher");
        when(pluginManager.get("dummy-fetcher")).thenReturn(fetcherPlugin);
        when(fetcherConfigurationFactory.create(any(), any())).thenReturn(oldConfiguration).thenReturn(newConfiguration);
    }

    private static Page pageWithSource(String configuration) {
        return Page.builder().source(PageSource.builder().type("dummy-fetcher").configuration(configuration).build()).build();
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

    /** Wraps an InputStream and tracks whether close() was called. */
    static class TrackingInputStream extends InputStream {

        private final InputStream delegate;
        boolean closed = false;

        TrackingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }
}
