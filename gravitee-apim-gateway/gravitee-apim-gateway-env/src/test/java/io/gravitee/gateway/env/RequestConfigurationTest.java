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
package io.gravitee.gateway.env;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RequestConfigurationTest {

    private RequestConfiguration cut;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // get Logback Logger
        Logger logger = (Logger) LoggerFactory.getLogger(RequestConfiguration.class);

        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        logger.addAppender(listAppender);

        cut = new RequestConfiguration();
    }

    @Test
    void should_configure_timeout_when_greater_than_0() {
        assertThat(cut.httpRequestTimeoutConfiguration(30L, 10, 10_000L))
            .extracting(
                RequestTimeoutConfiguration::getRequestTimeout,
                RequestTimeoutConfiguration::getRequestTimeoutGraceDelay,
                RequestTimeoutConfiguration::getPolicyTimeoutMs
            )
            .containsExactly(30L, 10L, 10_000L);
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(longs = { -10L, 0, 30L })
    void should_use_configured_timeout(long timeout) {
        final RequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 10, 10_000L);

        assertThat(result)
            .extracting(RequestTimeoutConfiguration::getRequestTimeout, RequestTimeoutConfiguration::getRequestTimeoutGraceDelay)
            .containsExactly(timeout, 10L);

        if (timeout <= 0) {
            final List<ILoggingEvent> logList = listAppender.list;
            assertThat(logList)
                .hasSize(1)
                .element(0)
                .extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
                .containsExactly(
                    "A proper timeout (greater than 0) should be set in order to avoid unclose connection, suggested value is 30_000 ms",
                    Level.WARN
                );
        }
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @NullSource
    void should_use_default_timeout_when_unset(Long timeout) {
        final RequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 10, 10_000L);

        assertThat(result)
            .extracting(RequestTimeoutConfiguration::getRequestTimeout, RequestTimeoutConfiguration::getRequestTimeoutGraceDelay)
            .containsExactly(30_000L, 10L);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
            .containsExactly("Http request timeout cannot be unset. Setting it to default value: 30_000 ms", Level.WARN);
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class Reading_the_path_handling_mode {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "RAW", "REJECT", "NORMALIZE", "normalize", "  NORMALIZE  ", "NoRmAlIzE" })
        void should_accept_a_known_value_whatever_its_case_or_padding(String configured) {
            final RequestPathConfiguration result = cut.httpRequestPathConfiguration(configured);

            assertThat(result.getHandling()).isEqualTo(RequestPathHandling.valueOf(configured.trim().toUpperCase(Locale.ROOT)));
            assertThat(warnings()).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = { "NORMALISE", "REJEKT", "", "  ", "true" })
        void should_fall_back_to_the_default_on_an_unknown_value_and_say_so(String configured) {
            // The default, not RAW. An operator who mistypes has to be told — we do not refuse to
            // start, the sibling timeout bean above sets the house rule, and the warning is the only
            // signal they get — but an unreadable value must not be a way to reach the raw path.
            final RequestPathConfiguration result = cut.httpRequestPathConfiguration(configured);

            assertThat(result.getHandling()).isEqualTo(RequestPathHandling.NORMALIZE);
            assertThat(warnings())
                .singleElement()
                .satisfies(warning -> assertThat(warning).contains("http.pathHandling"));
        }

        @Test
        void should_survive_a_jvm_whose_default_locale_uppercases_i_to_a_dotted_capital() {
            // Turkish: "normalize".toUpperCase() is "NORMALİZE" and valueOf throws, so a mode the
            // operator spelled correctly is read as unknown. The failure is invisible until someone
            // deploys in Istanbul, and no other accepted value carries an "i" to expose it.
            final Locale previous = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr"));

                assertThat(cut.httpRequestPathConfiguration("normalize").getHandling()).isEqualTo(RequestPathHandling.NORMALIZE);
            } finally {
                Locale.setDefault(previous);
            }
        }

        @Test
        void should_normalize_when_nothing_is_configured() throws NoSuchMethodException {
            // The bean is handed an already-resolved String, so every other test here runs past the
            // placeholder. This is the only assertion covering what a gateway with no http.pathHandling
            // in its configuration actually does — and getting it wrong ships the fix switched off
            // while the whole suite stays green.
            final Method bean = RequestConfiguration.class.getMethod("httpRequestPathConfiguration", String.class);
            final Value annotation = (Value) bean.getParameterAnnotations()[0][0];

            assertThat(annotation.value()).isEqualTo("${http.pathHandling:NORMALIZE}");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "RAW", "REJECT", "NORMALIZE" })
        void should_state_the_active_mode_at_startup_including_the_default(String configured) {
            // The operator's only positive confirmation of which mode came up, and the counterpart
            // of the warning above: without it, a mistyped value looks exactly like the default.
            cut.httpRequestPathConfiguration(configured);

            assertThat(listAppender.list)
                .filteredOn(event -> event.getLevel() == Level.INFO)
                .singleElement()
                .extracting(ILoggingEvent::getFormattedMessage, as(InstanceOfAssertFactories.STRING))
                .contains(configured);
        }

        private List<String> warnings() {
            return listAppender.list
                .stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        }
    }
}
