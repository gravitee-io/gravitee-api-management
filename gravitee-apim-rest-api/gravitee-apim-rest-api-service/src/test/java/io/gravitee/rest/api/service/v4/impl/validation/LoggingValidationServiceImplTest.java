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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.validation.LoggingValidationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingValidationServiceImplTest {

    @Mock
    private ParameterService parameterService;

    private LoggingValidationService loggingValidationService;

    MockedStatic<Instant> mockedStaticInstant;

    @Before
    public void setUp() throws Exception {
        Instant instant = Instant.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC));
        mockedStaticInstant = mockStatic(Instant.class);
        mockedStaticInstant.when(Instant::now).thenReturn(instant);

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.LOGGING_DEFAULT_MAX_DURATION),
                any(Function.class),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(singletonList(1L));

        loggingValidationService = new LoggingValidationServiceImpl(parameterService);
    }

    @After
    public void tearDown() throws Exception {
        mockedStaticInstant.close();
    }

    @Test
    public void shouldNotAddDefaultConditionIfNoLogging() {
        // No logging
        assertNull(loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), null));

        // Default logging
        Logging defaultLogging = new Logging();
        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), defaultLogging);
        assertSame(defaultLogging, sanitizedLogging);
    }

    @Test
    public void shouldNotAddDefaultConditionIfNoneLogging() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        logging.setCondition("wrong");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertSame(logging, sanitizedLogging);
    }

    @Test
    public void shouldNotAddDefaultConditionIfWrongConditionAndNoSettings() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("true");

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.LOGGING_DEFAULT_MAX_DURATION),
                any(Function.class),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(singletonList(0L));

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertSame(logging, sanitizedLogging);
    }

    @Test
    public void shouldAddDefaultConditionIfWrongConditionAndWithSettings() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("true");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l && (true)}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLessOrEqual() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp <= 2550166583090l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLess() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp < 2550166583090l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampAndAfter() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{#request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2)}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestamp() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldHandleAfter_doubleParenthesis() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldHandleBefore_doubleParenthesis() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldHandleBefore_multipleParenthesis() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestampAndAfter() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2'}"
        );

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedLogging.getCondition()
        );
    }

    @Test
    public void shouldNotOverrideTimestampIfBeforeThreshold() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp <= 2l}");

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.LOGGING_DEFAULT_MAX_DURATION),
                any(Function.class),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(singletonList(3L));

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 2l}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEquals() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp >= 5l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp >= 5l)}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreater() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp > 5l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp > 5l)}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePast() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp >= 0l}");

        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp >= 0l)}", sanitizedLogging.getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePastWithOrCondition() {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("{#request.timestamp >= 0l}");

        checkCondition(logging, "true || #request.timestamp <= 2l", "{(true) && #request.timestamp <= 1l}");
        checkCondition(logging, "#request.timestamp <= 2l || true", "{#request.timestamp <= 1l && (true)}");
        checkCondition(
            logging,
            "{#request.timestamp <= 2l || #request.timestamp >= 1l}",
            "{#request.timestamp <= 1l && (#request.timestamp >= 1l)}"
        );
        checkCondition(
            logging,
            "{#request.timestamp <= 1234l  || #request.timestamp > 2l}",
            "{#request.timestamp <= 1l && (#request.timestamp > 2l)}"
        );
        checkCondition(logging, "#request.timestamp <= 1l || true", "{#request.timestamp <= 1l && (true)}");
        checkCondition(logging, "{#request.timestamp <= 0l}", "{#request.timestamp <= 0l}");
    }

    private void checkCondition(final Logging logging, final String condition, final String expectedCondition) {
        logging.setCondition(condition);
        Logging sanitizedLogging = loggingValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), logging);
        assertEquals(LoggingMode.CLIENT_PROXY, sanitizedLogging.getMode());
        assertEquals(expectedCondition, sanitizedLogging.getCondition());
    }
}
