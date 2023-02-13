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

import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.TimeBoundedCharSequence;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.exception.AnalyticsIncompatibleApiTypeConfigurationException;
import io.gravitee.rest.api.service.v4.exception.AnalyticsMessageSamplingValueInvalidException;
import io.gravitee.rest.api.service.v4.exception.LoggingInvalidConfigurationException;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class AnalyticsValidationServiceImpl extends TransactionalService implements AnalyticsValidationService {

    private static final Pattern LOGGING_MAX_DURATION_PATTERN = Pattern.compile(
        "(?<before>.*)\\#request.timestamp\\s*\\<\\=?\\s*(?<timestamp>\\d*)l(?<after>.*)"
    );
    private static final String LOGGING_MAX_DURATION_CONDITION = "#request.timestamp <= %dl";
    private static final String LOGGING_DELIMITER_BASE = "\\s+(\\|\\||\\&\\&)\\s+";
    private static final Duration REGEX_TIMEOUT = Duration.ofSeconds(2);

    private final ParameterService parameterService;

    public AnalyticsValidationServiceImpl(final ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public Analytics validateAndSanitize(final ExecutionContext executionContext, final ApiType type, final Analytics analytics) {
        if (analytics == null) {
            Analytics defaultAnalytics = new Analytics();
            if (type == ApiType.ASYNC) {
                setDefaultMessageSampling(defaultAnalytics);
            }
            return defaultAnalytics;
        } else if (analytics.isEnabled()) {
            validateAndSanitizeSampling(type, analytics);
            validateAndSanitizeLogging(executionContext, type, analytics);
            return analytics;
        }
        return analytics;
    }

    private static void setDefaultMessageSampling(final Analytics analytics) {
        Sampling countSampling = new Sampling();
        countSampling.setType(SamplingType.COUNT);
        countSampling.setValue("10");
        analytics.setMessageSampling(countSampling);
    }

    private void validateAndSanitizeSampling(final ApiType type, final Analytics analytics) {
        if (ApiType.SYNC.equals(type) && analytics.getMessageSampling() != null) {
            throw new AnalyticsIncompatibleApiTypeConfigurationException(Map.of("analytics.messageSampling", "invalid"));
        }
        if (ApiType.ASYNC.equals(type)) {
            if (analytics.getMessageSampling() == null) {
                setDefaultMessageSampling(analytics);
            }
            Sampling messageSampling = analytics.getMessageSampling();
            if (!messageSampling.getType().validate(messageSampling.getValue())) {
                throw new AnalyticsMessageSamplingValueInvalidException(messageSampling);
            }
        }
    }

    private void validateAndSanitizeLogging(final ExecutionContext executionContext, final ApiType type, final Analytics analytics) {
        Logging logging = analytics.getLogging();
        if (logging != null && logging.getMode().isEnabled()) {
            if (logging.getPhase() == null || logging.getContent() == null) {
                throw new LoggingInvalidConfigurationException();
            }

            // Validate logging option according to api
            if (
                (
                    type == ApiType.SYNC &&
                    (
                        logging.getContent().isMessageHeaders() ||
                        logging.getContent().isMessagePayload() ||
                        logging.getContent().isMessageMetadata() ||
                        logging.getMessageCondition() != null
                    )
                ) ||
                (type == ApiType.ASYNC && logging.getContent().isPayload())
            ) {
                throw new LoggingInvalidConfigurationException();
            }

            logging.setCondition(computeMaxDurationCondition(executionContext, logging.getCondition()));
        }
        analytics.setLogging(logging);
    }

    @Override
    public io.gravitee.definition.model.Logging validateAndSanitize(
        final ExecutionContext executionContext,
        final io.gravitee.definition.model.Logging logging
    ) {
        if (logging != null && !LoggingMode.NONE.equals(logging.getMode())) {
            logging.setCondition(computeMaxDurationCondition(executionContext, logging.getCondition()));
        }
        return logging;
    }

    private String computeMaxDurationCondition(final ExecutionContext executionContext, final String existingCondition) {
        Optional<Long> optionalMaxDuration = parameterService
            .findAll(executionContext, Key.LOGGING_DEFAULT_MAX_DURATION, Long::valueOf, ParameterReferenceType.ORGANIZATION)
            .stream()
            .findFirst();
        if (optionalMaxDuration.isPresent() && optionalMaxDuration.get() > 0) {
            long maxEndDate = Instant.now().toEpochMilli() + optionalMaxDuration.get();

            // if no condition set, add one
            if (existingCondition == null || existingCondition.isEmpty()) {
                return "{" + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + "}";
            } else {
                String conditionWithoutBraces = existingCondition.trim().replaceAll("\\{", "").replaceAll("\\}", "");
                Matcher matcher = LOGGING_MAX_DURATION_PATTERN.matcher(new TimeBoundedCharSequence(conditionWithoutBraces, REGEX_TIMEOUT));
                if (matcher.matches()) {
                    String currentDurationAsStr = matcher.group("timestamp");
                    String before = formatExpression(matcher, "before");
                    String after = formatExpression(matcher, "after");
                    try {
                        final long currentDuration = Long.parseLong(currentDurationAsStr);
                        if (currentDuration > maxEndDate || (!before.isEmpty() || !after.isEmpty())) {
                            return "{" + before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after + "}";
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Wrong format of the logging condition. Add the default one", nfe);
                        return "{" + before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after + "}";
                    }
                } else {
                    return "{" + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + " && (" + conditionWithoutBraces + ")}";
                }
            }
        }
        return existingCondition;
    }

    @SuppressWarnings("java:S5852")
    // do not warn about catastrophic backtracking as the matcher is bounded by a timeout
    private String formatExpression(final Matcher matcher, final String group) {
        String matchedExpression = Optional.ofNullable(matcher.group(group)).orElse("");
        final boolean expressionBlank = "".equals(matchedExpression);
        final boolean after = "after".equals(group);

        String expression;
        if (after) {
            if (matchedExpression.startsWith(" && (") && matchedExpression.endsWith(")")) {
                matchedExpression = matchedExpression.substring(5, matchedExpression.length() - 1);
            }
            expression = expressionBlank ? "" : " && (" + matchedExpression + ")";
            expression = expression.replaceAll("\\(" + LOGGING_DELIMITER_BASE, "\\(");
        } else {
            if (matchedExpression.startsWith("(") && matchedExpression.endsWith(") && ")) {
                matchedExpression = matchedExpression.substring(1, matchedExpression.length() - 5);
            }
            expression = expressionBlank ? "" : "(" + matchedExpression + ") && ";
            expression = expression.replaceAll(LOGGING_DELIMITER_BASE + "\\)", "\\)");
        }
        return expression;
    }
}
