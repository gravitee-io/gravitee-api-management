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
package io.gravitee.rest.api.model.settings.logging;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Duration;

public class MessageSamplingSettingsValidator implements ConstraintValidator<ValidMessageSamplingSettings, MessageSampling> {

    @Override
    public boolean isValid(MessageSampling messageSampling, ConstraintValidatorContext constraintValidatorContext) {
        final boolean validCountSettings = validateCountSettings(messageSampling, constraintValidatorContext);
        final boolean validProbabilisticSettings = validateProbabilisticSettings(messageSampling, constraintValidatorContext);
        final boolean validTemporalSettings = validateTemporalSettings(messageSampling, constraintValidatorContext);

        return validCountSettings && validProbabilisticSettings && validTemporalSettings;
    }

    private static boolean validateCountSettings(MessageSampling messageSampling, ConstraintValidatorContext constraintValidatorContext) {
        final MessageSampling.Count count = messageSampling.getCount();
        if (count.getDefaultValue() < count.getLimit()) {
            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Invalid count default value, 'default' should be greater than 'limit'")
                .addConstraintViolation();
            return false;
        }
        return true;
    }

    private static boolean validateProbabilisticSettings(
        MessageSampling messageSampling,
        ConstraintValidatorContext constraintValidatorContext
    ) {
        final MessageSampling.Probabilistic probabilistic = messageSampling.getProbabilistic();
        if (probabilistic.getDefaultValue() > probabilistic.getLimit()) {
            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Invalid probabilistic default value, 'default' should be lower than 'limit'")
                .addConstraintViolation();
            return false;
        }
        return true;
    }

    private static boolean validateTemporalSettings(
        MessageSampling messageSampling,
        ConstraintValidatorContext constraintValidatorContext
    ) {
        final MessageSampling.Temporal temporal = messageSampling.getTemporal();

        boolean hasValidationConstraint = false;
        Duration defaultDuration = null;
        Duration limitDuration = null;

        try {
            defaultDuration = Duration.parse(temporal.getDefaultValue());
        } catch (Exception e) {
            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Invalid temporal default value, 'default' should be a valid ISO-8601 date")
                .addConstraintViolation();
            hasValidationConstraint = true;
        }
        try {
            limitDuration = Duration.parse(temporal.getLimit());
        } catch (Exception e) {
            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Invalid temporal limit value, 'limit' should be a valid ISO-8601 date")
                .addConstraintViolation();
            hasValidationConstraint = true;
        }

        if (defaultDuration != null && limitDuration != null && defaultDuration.getSeconds() < limitDuration.getSeconds()) {
            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Invalid temporal default value, 'default' should be greater than 'limit'")
                .addConstraintViolation();
            hasValidationConstraint = true;
        }

        return !hasValidationConstraint;
    }
}
