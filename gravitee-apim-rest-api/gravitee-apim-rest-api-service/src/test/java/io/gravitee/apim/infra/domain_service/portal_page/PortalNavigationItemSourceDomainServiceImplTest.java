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
package io.gravitee.apim.infra.domain_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.infra.domain_service.documentation.DummyFetcher;
import io.gravitee.apim.infra.domain_service.documentation.DummyFetcherConfiguration;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class PortalNavigationItemSourceDomainServiceImplTest {

    private static final String DUMMY_FETCHER = "dummy-fetcher";
    private static final String SENSITIVE_VALUE = "I'm a sensitive data";
    private static final String DUMMY_FETCHER_CLASS = "io.gravitee.apim.infra.domain_service.documentation.DummyFetcher";

    @Mock
    FetcherConfigurationFactory fetcherConfigurationFactory;

    @Mock
    PluginManager<FetcherPlugin<?>> pluginManager;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    @SuppressWarnings("rawtypes")
    FetcherPlugin fetcherPlugin;

    PortalNavigationItemSourceDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new PortalNavigationItemSourceDomainServiceImpl(fetcherConfigurationFactory, pluginManager, applicationContext);
    }

    @SuppressWarnings("unchecked")
    private void mockDummyFetcherPlugin(DummyFetcherConfiguration... configurations) {
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(mock(AutowireCapableBeanFactory.class));
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(fetcherPlugin.clazz()).thenReturn(DUMMY_FETCHER_CLASS);
        when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);
        var stubbing = when(fetcherConfigurationFactory.create(any(), any()));
        for (DummyFetcherConfiguration configuration : configurations) {
            stubbing = stubbing.thenReturn(configuration);
        }
    }

    @Nested
    class FetchContent {

        @Test
        void should_fetch_content() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));
            DummyFetcher.nextStream.set(new ByteArrayInputStream("# fetched markdown".getBytes()));

            var content = cut.fetchContent(dummySource());

            assertThat(content).isEqualTo("# fetched markdown");
        }

        @Test
        void should_throw_when_no_plugin_found_for_source_type() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(null);

            assertThatThrownBy(() -> cut.fetchContent(dummySource()))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining(DUMMY_FETCHER);
        }

        @Test
        void should_report_build_fetcher_error() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);

            assertThatThrownBy(() -> cut.fetchContent(dummySource()))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("unable to build fetcher instance");
        }

        @Test
        void should_not_leak_the_configuration_in_the_fetch_error() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));
            DummyFetcher.nextStream.set(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("stream failure");
                    }
                }
            );

            // This message is stored in lastFetchError and returned by the API: it must carry no secret
            assertThatThrownBy(() -> cut.fetchContent(dummySource()))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining(DUMMY_FETCHER)
                .hasMessageNotContaining(SENSITIVE_VALUE);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockDummyConfigurationClass() {
        when(fetcherPlugin.fetcher()).thenReturn(DummyFetcher.class);
        when(fetcherPlugin.configuration()).thenReturn(DummyFetcherConfiguration.class);
        when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);
    }

    @Nested
    class RemoveSensitiveData {

        @Test
        void should_mask_sensitive_fields() throws Exception {
            mockDummyConfigurationClass();
            var source = dummySource();

            cut.removeSensitiveData(source);

            JsonNode configuration = new ObjectMapper().readTree(source.getSourceConfiguration());
            assertThat(configuration.get("sensitiveData").textValue()).isEqualTo(
                PortalNavigationItemSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT
            );
            assertThat(configuration.get("nonSensitiveData").textValue()).isEqualTo("I'm not a sensitive data");
            assertThat(configuration.size()).isEqualTo(2);
        }

        @Test
        void should_not_rewrite_configuration_when_no_sensitive_value_is_set() {
            mockDummyConfigurationClass();
            var source = dummySource("{\"nonSensitiveData\":\"I'm not a sensitive data\"}");
            var originalConfiguration = source.getSourceConfiguration();

            cut.removeSensitiveData(source);

            assertThat(source.getSourceConfiguration()).isEqualTo(originalConfiguration);
        }

        @Test
        void should_resolve_the_sensitive_keys_only_once_per_source_type() {
            mockDummyConfigurationClass();

            cut.removeSensitiveData(dummySource());
            cut.removeSensitiveData(dummySource());

            verify(pluginManager, times(1)).get(DUMMY_FETCHER);
        }

        @Test
        void should_blank_configuration_when_plugin_cannot_be_loaded() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(null);
            var source = dummySource();

            cut.removeSensitiveData(source);

            assertThat(source.getSourceConfiguration()).isEqualTo(PortalNavigationItemSourceDomainServiceImpl.BLANKED_CONFIGURATION);
        }

        @Test
        void should_blank_configuration_when_sensitive_keys_cannot_be_resolved() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);
            var source = dummySource();

            cut.removeSensitiveData(source);

            assertThat(source.getSourceConfiguration()).isEqualTo(PortalNavigationItemSourceDomainServiceImpl.BLANKED_CONFIGURATION);
        }
    }

    @Nested
    class MergeSensitiveData {

        @Test
        void should_restore_masked_values_from_old_source() throws Exception {
            mockDummyConfigurationClass();
            var oldSource = dummySource("{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"original-secret-token\"}");
            var newSource = dummySource(
                "{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"" +
                    PortalNavigationItemSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT +
                    "\"}"
            );

            cut.mergeSensitiveData(oldSource, newSource);

            JsonNode configuration = new ObjectMapper().readTree(newSource.getSourceConfiguration());
            assertThat(configuration.get("sensitiveData").textValue()).isEqualTo("original-secret-token");
            assertThat(configuration.get("nonSensitiveData").textValue()).isEqualTo("data");
        }

        @Test
        void should_not_restore_masked_values_across_a_source_type_change() {
            var oldSource = dummySource("{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"original-secret-token\"}");
            var maskedConfiguration =
                "{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"" +
                PortalNavigationItemSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT +
                "\"}";
            var newSource = PortalNavigationItemSource.builder()
                .sourceType("another-fetcher")
                .sourceConfiguration(maskedConfiguration)
                .build();

            cut.mergeSensitiveData(oldSource, newSource);

            assertThat(newSource.getSourceConfiguration()).isEqualTo(maskedConfiguration);
            assertThat(newSource.getSourceConfiguration()).doesNotContain("original-secret-token");
        }

        @Test
        void should_keep_new_value_when_not_masked() {
            mockDummyConfigurationClass();
            var newSource = dummySource("{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"new-secret-token\"}");
            var originalConfiguration = newSource.getSourceConfiguration();

            cut.mergeSensitiveData(dummySource(), newSource);

            assertThat(newSource.getSourceConfiguration()).isEqualTo(originalConfiguration);
        }

        @Test
        void should_do_nothing_when_old_source_is_null() {
            cut.mergeSensitiveData(null, dummySource());

            verifyNoInteractions(fetcherConfigurationFactory, pluginManager, applicationContext);
        }

        @Test
        void should_keep_client_configuration_when_sensitive_keys_cannot_be_resolved() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);
            var newSource = dummySource();
            var clientConfiguration = newSource.getSourceConfiguration();

            cut.mergeSensitiveData(dummySource(), newSource);

            assertThat(newSource.getSourceConfiguration()).isEqualTo(clientConfiguration);
        }

        @Test
        void should_keep_the_configuration_key_set_stable_across_mask_and_merge() {
            mockDummyConfigurationClass();
            var storedConfiguration = "{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"secret-token\"}";
            var stored = dummySource(storedConfiguration);

            var exposed = dummySource(storedConfiguration);
            cut.removeSensitiveData(exposed);
            cut.mergeSensitiveData(stored, exposed);

            assertThat(exposed.getSourceConfiguration()).isEqualTo(storedConfiguration);
        }
    }

    @Nested
    class ValidateSourceConfiguration {

        @Test
        void should_accept_valid_configuration() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));

            cut.validateSourceConfiguration(dummySource());
        }

        @Test
        void should_reject_unknown_source_type() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(null);

            assertThatThrownBy(() -> cut.validateSourceConfiguration(dummySource()))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining(DUMMY_FETCHER);
        }

        @Test
        void should_reject_configuration_that_cannot_build_a_fetcher() {
            when(pluginManager.get(DUMMY_FETCHER)).thenReturn(fetcherPlugin);

            assertThatThrownBy(() -> cut.validateSourceConfiguration(dummySource()))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("not valid");
        }

        @Test
        void should_reject_a_sensitive_field_still_holding_the_masked_placeholder() {
            // Validation runs after the merge: a leftover placeholder means there was nothing to restore
            mockDummyConfigurationClass();
            var source = dummySource(
                "{\"nonSensitiveData\":\"data\",\"sensitiveData\":\"%s\"}".formatted(
                    PortalNavigationItemSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT
                )
            );

            assertThatThrownBy(() -> cut.validateSourceConfiguration(source))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("sensitiveData")
                .hasMessageContaining("actual value");
        }

        @Test
        void should_reject_a_configuration_the_fetcher_cannot_read() {
            // The factory swallows deserialization errors and returns null instead of throwing
            mockDummyConfigurationClass();
            when(fetcherConfigurationFactory.create(any(), any())).thenReturn(null);

            assertThatThrownBy(() -> cut.validateSourceConfiguration(dummySource()))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("not valid");
        }

        @Test
        void should_reject_invalid_cron_expression() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));
            var source = dummySource().toBuilder().useAutoFetch(true).fetchCron("not-a-cron").build();

            assertThatThrownBy(() -> cut.validateSourceConfiguration(source))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("not-a-cron");
        }

        @Test
        void should_reject_auto_fetch_without_cron() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));
            var source = dummySource().toBuilder().useAutoFetch(true).fetchCron(null).build();

            assertThatThrownBy(() -> cut.validateSourceConfiguration(source))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("auto-fetch");
        }

        @Test
        void should_accept_valid_cron_expression() {
            mockDummyFetcherPlugin(new DummyFetcherConfiguration("data", "secret"));
            var source = dummySource().toBuilder().useAutoFetch(true).fetchCron("0 */5 * * * *").build();

            cut.validateSourceConfiguration(source);
        }
    }

    @Nested
    class IsAutoFetchDue {

        private static final Instant NOW = Instant.parse("2026-08-05T12:34:56Z");

        @BeforeEach
        void freezeTime() {
            TimeProvider.overrideClock(Clock.fixed(NOW, ZoneId.systemDefault()));
        }

        @AfterEach
        void unfreezeTime() {
            TimeProvider.overrideClock(Clock.systemDefaultZone());
        }

        @Test
        void should_be_due_when_the_cron_elapsed_since_the_last_attempt() {
            var source = autoFetchSource("0 */10 * * * *", NOW.minus(11, ChronoUnit.MINUTES));

            assertThat(cut.isAutoFetchDue(source)).isTrue();
        }

        @Test
        void should_not_be_due_when_the_cron_has_not_elapsed_yet() {
            var source = autoFetchSource("0 0 * * * *", NOW.minus(5, ChronoUnit.MINUTES));

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        /**
         * The point of lastFetchAttemptAt: a source that keeps failing never updates lastFetchedAt, and
         * counting from it would make the page due on every scheduler run instead of on its own cron.
         */
        @Test
        void should_not_be_due_when_the_last_attempt_failed_before_the_cron_elapsed() {
            var source = dummySource()
                .toBuilder()
                .useAutoFetch(true)
                .fetchCron("0 0 * * * *")
                .lastFetchedAt(null)
                .lastFetchAttemptAt(NOW.minus(5, ChronoUnit.MINUTES))
                .lastFetchError("Unable to fetch content from source type dummy-fetcher.")
                .build();

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        @Test
        void should_be_due_again_once_the_cron_elapsed_since_a_failed_attempt() {
            var source = dummySource()
                .toBuilder()
                .useAutoFetch(true)
                .fetchCron("0 0 * * * *")
                .lastFetchedAt(null)
                .lastFetchAttemptAt(NOW.minus(2, ChronoUnit.HOURS))
                .lastFetchError("Unable to fetch content from source type dummy-fetcher.")
                .build();

            assertThat(cut.isAutoFetchDue(source)).isTrue();
        }

        /** Items stored before lastFetchAttemptAt existed still honour their cron via lastFetchedAt. */
        @Test
        void should_fall_back_on_last_fetched_at_when_no_attempt_was_ever_recorded() {
            var source = dummySource()
                .toBuilder()
                .useAutoFetch(true)
                .fetchCron("0 0 * * * *")
                .lastFetchedAt(NOW.minus(5, ChronoUnit.MINUTES))
                .lastFetchAttemptAt(null)
                .build();

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        @Test
        void should_be_due_when_the_page_was_never_fetched() {
            var source = autoFetchSource("0 0 1 1 1 *", null);

            assertThat(cut.isAutoFetchDue(source)).isTrue();
        }

        @Test
        void should_not_be_due_when_auto_fetch_is_disabled() {
            var source = dummySource()
                .toBuilder()
                .useAutoFetch(false)
                .fetchCron("0 */10 * * * *")
                .lastFetchAttemptAt(Instant.EPOCH)
                .build();

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        @Test
        void should_not_be_due_when_no_cron_is_configured() {
            var source = dummySource().toBuilder().useAutoFetch(true).fetchCron(null).lastFetchAttemptAt(Instant.EPOCH).build();

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        @Test
        void should_not_be_due_when_the_stored_cron_cannot_be_parsed() {
            var source = autoFetchSource("not-a-cron", Instant.EPOCH);

            assertThat(cut.isAutoFetchDue(source)).isFalse();
        }

        private PortalNavigationItemSource autoFetchSource(String cron, Instant lastFetchAttemptAt) {
            return dummySource().toBuilder().useAutoFetch(true).fetchCron(cron).lastFetchAttemptAt(lastFetchAttemptAt).build();
        }
    }

    private static PortalNavigationItemSource dummySource() {
        return dummySource(
            """
            {
               "nonSensitiveData" : "I'm not a sensitive data",
               "sensitiveData" : "%s"
            }
            """.formatted(SENSITIVE_VALUE)
        );
    }

    private static PortalNavigationItemSource dummySource(String configuration) {
        return PortalNavigationItemSource.builder().sourceType(DUMMY_FETCHER).sourceConfiguration(configuration).build();
    }
}
