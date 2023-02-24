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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.exception.AnalyticsMessageSamplingValueInvalidException;
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
    public void should_add_default_analytics_if_no_analytics() {
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
    public void should_not_add_default_condition_if_none_logging() {
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
    public void should_not_add_default_condition_if_wrong_condition_and_no_settings() {
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
    public void should_add_default_condition_if_wrong_condition_and_with_settings() {
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
    public void should_override_timestamp_case_timestamp_less_or_equal() {
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
    public void should_override_timestamp_case_timestamp_less() {
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
    public void should_override_timestamp_case_timestamp_and_after() {
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
    public void should_override_timestamp_case_before_and_timestamp() {
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
    public void should_handle_after_double_parenthesis() {
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
    public void should_handle_before_double_parenthesis() {
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
    public void should_handle_before_multiple_parenthesis() {
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
    public void should_override_timestamp_case_before_and_timestamp_and_after() {
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
    public void should_not_override_timestamp_if_before_threshold() {
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
    public void should_override_timestamp_case_greater_or_equals() {
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
    public void should_override_timestamp_case_greater() {
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
    public void should_override_timestamp_case_greater_or_equals_in_the_past() {
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
    public void should_override_timestamp_case_greater_or_equals_in_the_past_with_or_condition() {
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

    @Test
    public void should_set_default_analytics_without_sampling_when_sync_api() {
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.SYNC,
            null
        );
        assertEquals(new Analytics(), sanitizedAnalytics);
    }

    @Test
    public void should_set_default_analytics_with_sampling_when_async_api() {
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.ASYNC,
            null
        );
        Analytics expected = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("10");
        expected.setMessageSampling(messageSampling);
        assertEquals(expected, sanitizedAnalytics);
    }

    @Test
    public void should_set_default_sampling_when_async_api_and_analytics_enabled() {
        Analytics analytics = new Analytics();
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.ASYNC,
            analytics
        );
        Analytics expected = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("10");
        expected.setMessageSampling(messageSampling);
        assertEquals(expected, sanitizedAnalytics);
    }

    @Test
    public void should_validate_analytics_with_sampling_when_async_api() {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("10");
        analytics.setMessageSampling(messageSampling);
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.ASYNC,
            analytics
        );
        assertEquals(analytics, sanitizedAnalytics);
    }

    @Test
    public void should_throw_exception_when_validating_analytics_with_wrong_sampling_when_async_api() {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("0");
        analytics.setMessageSampling(messageSampling);
        assertThrows(
            AnalyticsMessageSamplingValueInvalidException.class,
            () -> analyticsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), ApiType.ASYNC, analytics)
        );
    }
}
