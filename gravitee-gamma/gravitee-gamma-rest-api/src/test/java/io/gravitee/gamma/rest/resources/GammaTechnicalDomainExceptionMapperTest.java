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
package io.gravitee.gamma.rest.resources;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaTechnicalDomainExceptionMapperTest {

    private static final TechnicalDomainException FAILURE = new TechnicalDomainException(
        "An error occurs while trying to get plugin schema for plugin guardrail-decision",
        new IOException("unreadable")
    );

    private final GammaTechnicalDomainExceptionMapper mapper = new GammaTechnicalDomainExceptionMapper();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GammaTechnicalDomainExceptionMapper.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
    }

    @Test
    void should_answer_500_with_the_message_of_the_failure() {
        try (Response response = mapper.toResponse(FAILURE)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            assertThat(((ErrorEntity) response.getEntity()).getMessage()).isEqualTo(FAILURE.getMessage());
        }
    }

    /** The layers below rethrow without logging: this is the one place the failure is recorded. */
    @Test
    void should_log_the_failure_once_with_its_cause() {
        mapper.toResponse(FAILURE).close();

        assertThat(logs.list)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage()).contains(FAILURE.getMessage());
                assertThat(event.getThrowableProxy().getClassName()).isEqualTo(TechnicalDomainException.class.getName());
            });
    }
}
