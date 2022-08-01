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

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.TimeBoundedCharSequence;
import io.gravitee.rest.api.service.exceptions.AllowOriginNotAllowedException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import io.gravitee.rest.api.service.v4.validation.LoggingValidationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class LoggingValidationServiceImpl extends TransactionalService implements LoggingValidationService {

    private static final Pattern LOGGING_MAX_DURATION_PATTERN = Pattern.compile(
        "(?<before>.*)\\#request.timestamp\\s*\\<\\=?\\s*(?<timestamp>\\d*)l(?<after>.*)"
    );
    private static final String LOGGING_MAX_DURATION_CONDITION = "#request.timestamp <= %dl";
    private static final String LOGGING_DELIMITER_BASE = "\\s+(\\|\\||\\&\\&)\\s+";
    private static final Duration REGEX_TIMEOUT = Duration.ofSeconds(2);

    private final ParameterService parameterService;

    public LoggingValidationServiceImpl(final ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public Logging validateAndSanitize(ExecutionContext executionContext, Logging logging) {
        if (logging != null && !LoggingMode.NONE.equals(logging.getMode())) {
            Optional<Long> optionalMaxDuration = parameterService
                .findAll(executionContext, Key.LOGGING_DEFAULT_MAX_DURATION, Long::valueOf, ParameterReferenceType.ORGANIZATION)
                .stream()
                .findFirst();
            if (optionalMaxDuration.isPresent() && optionalMaxDuration.get() > 0) {
                long maxEndDate = Instant.now().toEpochMilli() + optionalMaxDuration.get();

                // if no condition set, add one
                if (logging.getCondition() == null || logging.getCondition().isEmpty()) {
                    logging.setCondition("{" + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + "}");
                } else {
                    String conditionWithoutBraces = logging.getCondition().trim().replaceAll("\\{", "").replaceAll("\\}", "");
                    Matcher matcher = LOGGING_MAX_DURATION_PATTERN.matcher(
                        new TimeBoundedCharSequence(conditionWithoutBraces, REGEX_TIMEOUT)
                    );
                    if (matcher.matches()) {
                        String currentDurationAsStr = matcher.group("timestamp");
                        String before = formatExpression(matcher, "before");
                        String after = formatExpression(matcher, "after");
                        try {
                            final long currentDuration = Long.parseLong(currentDurationAsStr);
                            if (currentDuration > maxEndDate || (!before.isEmpty() || !after.isEmpty())) {
                                logging.setCondition(
                                    "{" + before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after + "}"
                                );
                            }
                        } catch (NumberFormatException nfe) {
                            log.error("Wrong format of the logging condition. Add the default one", nfe);
                            logging.setCondition("{" + before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after + "}");
                        }
                    } else {
                        logging.setCondition(
                            "{" + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + " && (" + conditionWithoutBraces + ")}"
                        );
                    }
                }
            }
        }
        return logging;
    }

    @SuppressWarnings("java:S5852") // do not warn about catastrophic backtracking as the matcher is bounded by a timeout
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
