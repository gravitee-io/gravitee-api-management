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
package io.gravitee.gateway.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class TimeoutConfigurationTest {

    private TimeoutConfiguration cut;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // get Logback Logger
        Logger logger = (Logger) LoggerFactory.getLogger(TimeoutConfiguration.class);

        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        logger.addAppender(listAppender);

        cut = new TimeoutConfiguration();
    }

    @ParameterizedTest(name = "Jupiter enabled: {0}")
    @ValueSource(booleans = { true, false })
    @DisplayName("Should use http.requestTimeout from configuration when greater than 0")
    void shouldConfigureTimeoutWhenGreaterThan0(boolean isJupiterEnabled) {
        assertThat(cut.httpRequestTimeoutConfiguration(30L, 10, isJupiterEnabled))
            .extracting(
                HttpRequestTimeoutConfiguration::getHttpRequestTimeout,
                HttpRequestTimeoutConfiguration::getHttpRequestTimeoutGraceDelay
            )
            .containsExactly(30L, 10L);
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(longs = { -10L, 0 })
    @NullSource
    @DisplayName("Should use default 30_000ms timeout when configured one is 0 or less in Jupiter mode")
    void shouldUseDefaultTimeoutInJupiterMode(Long timeout) {
        final HttpRequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 10, true);

        assertThat(result)
            .extracting(
                HttpRequestTimeoutConfiguration::getHttpRequestTimeout,
                HttpRequestTimeoutConfiguration::getHttpRequestTimeoutGraceDelay
            )
            .containsExactly(30_000L, 10L);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
            .containsExactly("Http request timeout cannot be set to 0 or unset. Setting it to default value: 30_000 ms", Level.WARN);
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(longs = { -10L, 0 })
    @DisplayName("Should just warn when configured one is 0 or less in V3 mode")
    void shouldDoNothingInV3Mode(long timeout) {
        final HttpRequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 5L, false);

        assertThat(result)
            .extracting(
                HttpRequestTimeoutConfiguration::getHttpRequestTimeout,
                HttpRequestTimeoutConfiguration::getHttpRequestTimeoutGraceDelay
            )
            .containsExactly(timeout, 5L);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
            .containsExactly(
                "A proper timeout should be set in order to avoid unclose connexion, suggested value is 30_000 ms",
                Level.WARN
            );
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @NullSource
    @DisplayName("Should set timeout to 30_000ms when http.requestTimeout is unset in V3 mode")
    void shouldSetDefaultTimeoutWhenUsetInV3Mode(Long timeout) {
        final HttpRequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 5L, false);

        assertThat(result)
            .extracting(
                HttpRequestTimeoutConfiguration::getHttpRequestTimeout,
                HttpRequestTimeoutConfiguration::getHttpRequestTimeoutGraceDelay
            )
            .containsExactly(30000L, 5L);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
            .containsExactly("Http request timeout is unset. Setting it to default value: 30_000 ms", Level.WARN);
    }
}
