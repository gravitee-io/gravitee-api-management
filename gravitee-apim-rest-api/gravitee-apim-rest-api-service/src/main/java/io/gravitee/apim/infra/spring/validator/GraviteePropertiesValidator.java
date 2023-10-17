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
package io.gravitee.apim.infra.spring.validator;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.logging.MessageSampling;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GraviteePropertiesValidator {

    private final Configuration configuration;
    private final Validator validator;

    public GraviteePropertiesValidator(Configuration configuration) {
        this.configuration = configuration;
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            this.validator = validatorFactory.getValidator();
        }
    }

    @PostConstruct
    public void validateProperties() {
        final Set<ConstraintViolation<MessageSampling>> constraintViolations = validateMessageSamplingSettings();

        if (constraintViolations != null && !constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    private Set<ConstraintViolation<MessageSampling>> validateMessageSamplingSettings() {
        final MessageSampling.MessageSamplingBuilder builder = MessageSampling.builder();

        final MessageSampling.Count.CountBuilder countBuilder = MessageSampling.Count.builder();

        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.key(), Integer.class) != null) {
            countBuilder.defaultValue(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.key(), Integer.class));
        }
        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT.key(), Integer.class) != null) {
            countBuilder.limit(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT.key(), Integer.class));
        }

        final MessageSampling.Probabilistic.ProbabilisticBuilder probabilisticSettingsBuilder = MessageSampling.Probabilistic.builder();

        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT.key(), Double.class) != null) {
            probabilisticSettingsBuilder.defaultValue(
                configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT.key(), Double.class)
            );
        }
        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT.key(), Double.class) != null) {
            probabilisticSettingsBuilder.limit(
                configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT.key(), Double.class)
            );
        }

        final MessageSampling.Temporal.TemporalBuilder temporalSettingsBuilder = MessageSampling.Temporal.builder();
        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT.key(), String.class) != null) {
            temporalSettingsBuilder.defaultValue(
                configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT.key(), String.class)
            );
        }
        if (configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT.key(), String.class) != null) {
            temporalSettingsBuilder.limit(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT.key(), String.class));
        }

        final MessageSampling messageSampling = builder
            .count(countBuilder.build())
            .probabilistic(probabilisticSettingsBuilder.build())
            .temporal(temporalSettingsBuilder.build())
            .build();

        return validator.validate(messageSampling);
    }
}
