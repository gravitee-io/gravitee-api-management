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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyticsValidationServiceImplTest {

    @Mock
    private ParameterService parameterService;

    private AnalyticsValidationService analyticsValidationService;

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

        analyticsValidationService = new AnalyticsValidationServiceImpl(parameterService);
    }

    @After
    public void tearDown() throws Exception {
        mockedStaticInstant.close();
    }

    @Test
    public void shouldAddDefaultAnalyticsIfNoAnalytics() {
        // No analytics
        assertNotNull(analyticsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), ApiType.SYNC, null));

        // Default logging
        Analytics analytics = new Analytics();
        Logging defaultLogging = new Logging();
        analytics.setLogging(defaultLogging);
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertSame(defaultLogging, sanitizedAnalytics.getLogging());
    }

    @Test
    public void shouldNotAddDefaultConditionIfNoneLogging() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(false).endpoint(false).build());
        logging.setCondition("wrong");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertSame(logging, sanitizedAnalytics.getLogging());
    }

    @Test
    public void shouldNotAddDefaultConditionIfWrongConditionAndNoSettings() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("true");
        analytics.setLogging(logging);

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.LOGGING_DEFAULT_MAX_DURATION),
                any(Function.class),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(singletonList(0L));

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertSame(logging, sanitizedAnalytics.getLogging());
    }

    @Test
    public void shouldAddDefaultConditionIfWrongConditionAndWithSettings() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("true");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l && (true)}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLessOrEqual() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp <= 2550166583090l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLess() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp < 2550166583090l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampAndAfter() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{#request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2)}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestamp() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldHandleAfter_doubleParenthesis() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(
            "{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldHandleBefore_doubleParenthesis() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldHandleBefore_multipleParenthesis() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(
            "{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}"
        );
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestampAndAfter() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(
            "{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2'}"
        );
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(
            "{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            sanitizedAnalytics.getLogging().getCondition()
        );
    }

    @Test
    public void shouldNotOverrideTimestampIfBeforeThreshold() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp <= 2l}");
        analytics.setLogging(logging);

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.LOGGING_DEFAULT_MAX_DURATION),
                any(Function.class),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(singletonList(3L));

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 2l}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEquals() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp >= 5l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp >= 5l)}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreater() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp > 5l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp > 5l)}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePast() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp >= 0l}");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals("{#request.timestamp <= 1l && (#request.timestamp >= 0l)}", sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePastWithOrCondition() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("{#request.timestamp >= 0l}");
        analytics.setLogging(logging);

        checkCondition(analytics, "true || #request.timestamp <= 2l", "{(true) && #request.timestamp <= 1l}");
        checkCondition(analytics, "#request.timestamp <= 2l || true", "{#request.timestamp <= 1l && (true)}");
        checkCondition(
            analytics,
            "{#request.timestamp <= 2l || #request.timestamp >= 1l}",
            "{#request.timestamp <= 1l && (#request.timestamp >= 1l)}"
        );
        checkCondition(
            analytics,
            "{#request.timestamp <= 1234l  || #request.timestamp > 2l}",
            "{#request.timestamp <= 1l && (#request.timestamp > 2l)}"
        );
        checkCondition(analytics, "#request.timestamp <= 1l || true", "{#request.timestamp <= 1l && (true)}");
        checkCondition(analytics, "{#request.timestamp <= 0l}", "{#request.timestamp <= 0l}");
    }

    private void checkCondition(final Analytics analytics, final String condition, final String expectedCondition) {
        analytics.getLogging().setCondition(condition);
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            analytics
        );
        assertEquals(LoggingMode.builder().entrypoint(true).endpoint(true).build(), sanitizedAnalytics.getLogging().getMode());
        assertEquals(expectedCondition, sanitizedAnalytics.getLogging().getCondition());
    }
}
