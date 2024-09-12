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

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

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
        assertThat(cut.httpRequestTimeoutConfiguration(30L, 10))
            .extracting(RequestTimeoutConfiguration::getRequestTimeout, RequestTimeoutConfiguration::getRequestTimeoutGraceDelay)
            .containsExactly(30L, 10L);
    }

    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(longs = { -10L, 0, 30L })
    void should_use_configured_timeout(long timeout) {
        final RequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 10);

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
        final RequestTimeoutConfiguration result = cut.httpRequestTimeoutConfiguration(timeout, 10);

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
}
