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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.exception.AnalyticsMessageSamplingValueInvalidException;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AnalyticsValidationServiceImplTest {

    private ParameterService parameterService;

    private AnalyticsValidationService analyticsValidationService;

    MockedStatic<Instant> mockedStaticInstant;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC));
        mockedStaticInstant = mockStatic(Instant.class);
        mockedStaticInstant.when(Instant::now).thenReturn(instant);

        parameterService = mock(ParameterService.class);
        lenient()
            .when(
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

    @AfterEach
    void tearDown() {
        mockedStaticInstant.close();
    }

    @Test
    void should_add_default_analytics_if_no_analytics() {
        // No analytics
        assertThat(analyticsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), ApiType.PROXY, null)).isNotNull();

        // Default logging
        Analytics analytics = new Analytics();
        Logging defaultLogging = new Logging();
        analytics.setLogging(defaultLogging);
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(sanitizedAnalytics.getLogging()).isSameAs(defaultLogging);
    }

    @Test
    void should_not_add_default_condition_if_none_logging() {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(false).endpoint(false).build());
        logging.setCondition("wrong");
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(logging).isSameAs(sanitizedAnalytics.getLogging());
    }

    @Test
    void should_not_add_default_condition_if_wrong_condition_and_no_settings() {
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
        ).thenReturn(singletonList(0L));

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(sanitizedAnalytics.getLogging()).isSameAs(logging);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        {
            "if_wrong_condition_and_with_settings,true,{#request.timestamp <= 1l && (true)}",
            "timestamp_case_timestamp_less,{#request.timestamp < 2550166583090l},{#request.timestamp <= 1l}",
            "double_parenthesis,{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')},{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
        }
    )
    void should_add_default_condition_if_wrong_condition_and_with_settings(String name, String condition, String expectedCondition) {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(condition);
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(sanitizedAnalytics.getLogging().getMode()).isEqualTo(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        assertThat(sanitizedAnalytics.getLogging().getCondition()).isEqualTo(expectedCondition);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        {
            "timestamp_less_or_equal,{#request.timestamp <= 2550166583090l},{#request.timestamp <= 1l}",
            "before_and_timestamp_after,{#request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2},{#request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2)}",
            "double_parenthesis,{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')},{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            "before_multiple_parenthesis,{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')},{((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            "before_and_timestamp_and_after,{#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2'},{(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')}",
            "greater_or_equals,{#request.timestamp >= 5l},{#request.timestamp <= 1l && (#request.timestamp >= 5l)}",
            "greater,{#request.timestamp > 5l},{#request.timestamp <= 1l && (#request.timestamp > 5l)}",
            "greater_or_equals_in_the_past,{#request.timestamp >= 0l},{#request.timestamp <= 1l && (#request.timestamp >= 0l)}",
        }
    )
    void should_override_timestamp(String name, String condition, String expectedCondition) {
        Analytics analytics = new Analytics();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition(condition);
        analytics.setLogging(logging);

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(LoggingMode.builder().entrypoint(true).endpoint(true).build()).isEqualTo(sanitizedAnalytics.getLogging().getMode());
        assertThat(sanitizedAnalytics.getLogging().getCondition()).isEqualTo(expectedCondition);
    }

    @Test
    void should_not_override_timestamp_if_before_threshold() {
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
        ).thenReturn(singletonList(3L));

        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            analytics
        );
        assertThat(LoggingMode.builder().entrypoint(true).endpoint(true).build()).isEqualTo(sanitizedAnalytics.getLogging().getMode());
        assertThat(sanitizedAnalytics.getLogging().getCondition()).isEqualTo("{#request.timestamp <= 2l}");
    }

    @Test
    void should_override_timestamp_case_greater_or_equals_in_the_past_with_or_condition() {
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
            ApiType.PROXY,
            analytics
        );
        assertThat(LoggingMode.builder().entrypoint(true).endpoint(true).build()).isEqualTo(sanitizedAnalytics.getLogging().getMode());
        assertThat(expectedCondition).isEqualTo(sanitizedAnalytics.getLogging().getCondition());
    }

    @Test
    void should_set_default_analytics_without_sampling_when_sync_api() {
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.PROXY,
            null
        );
        assertThat(sanitizedAnalytics).isEqualTo(new Analytics());
    }

    @Test
    void should_set_default_analytics_with_sampling_when_async_api() {
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.MESSAGE,
            null
        );
        Analytics expected = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.defaultValue());
        expected.setMessageSampling(messageSampling);
        assertThat(sanitizedAnalytics).isEqualTo(expected);
    }

    @Test
    void should_set_default_analytics_with_sampling_when_async_api_from_custom_settings() {
        when(parameterService.findAll(any(), eq(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT), any(), any())).thenReturn(List.of("77"));
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.MESSAGE,
            null
        );
        Analytics expected = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("77");
        expected.setMessageSampling(messageSampling);
        assertThat(sanitizedAnalytics).isEqualTo(expected);
    }

    @Test
    void should_set_default_sampling_when_async_api_and_analytics_enabled() {
        Analytics analytics = new Analytics();
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.MESSAGE,
            analytics
        );
        Analytics expected = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.defaultValue());
        expected.setMessageSampling(messageSampling);
        assertThat(expected).isEqualTo(sanitizedAnalytics);
    }

    @Test
    void should_validate_analytics_with_sampling_when_async_api() {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        messageSampling.setValue("10");
        analytics.setMessageSampling(messageSampling);
        Analytics sanitizedAnalytics = analyticsValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            ApiType.MESSAGE,
            analytics
        );
        assertThat(sanitizedAnalytics).isEqualTo(analytics);
    }

    @ParameterizedTest
    @CsvSource({ "COUNT,         0", "TEMPORAL,      PT0S", "PROBABILITY,   0.8", "WINDOWED_COUNT, 2/1PTS" })
    void should_throw_exception_when_validating_analytics_with_wrong_sampling_when_async_api(String type, String value) {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.valueOf(type));
        messageSampling.setValue(value);
        analytics.setMessageSampling(messageSampling);
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        assertThatCode(() -> analyticsValidationService.validateAndSanitize(executionContext, ApiType.MESSAGE, analytics)).isInstanceOf(
            AnalyticsMessageSamplingValueInvalidException.class
        );
    }
}
