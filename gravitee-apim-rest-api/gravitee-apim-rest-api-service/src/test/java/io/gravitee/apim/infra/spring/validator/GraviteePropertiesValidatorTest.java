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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.rest.api.model.parameters.Key;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteePropertiesValidatorTest {

    private GraviteePropertiesValidator cut;

    @Mock
    private Configuration configuration;

    @Mock
    private ValidatorFactory validatorFactory;

    @Mock
    private Validator validator;

    @Test
    void should_not_throw_if_configuration_is_valid() {
        assertWithMockedValidation(() -> assertDoesNotThrow(() -> cut.validateProperties()));
    }

    @Test
    void should_not_throw_if_configuration_from_properties_is_valid() {
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT.key(), Double.class)).thenReturn(0.1);
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT.key(), Double.class)).thenReturn(0.15);

        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.key(), Integer.class)).thenReturn(100);
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT.key(), Integer.class)).thenReturn(20);

        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT.key(), String.class)).thenReturn("PT10S");
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT.key(), String.class)).thenReturn("PT5S");

        assertWithMockedValidation(() -> assertDoesNotThrow(() -> cut.validateProperties()));
    }

    @Test
    void should_throw_if_configuration_invalid() {
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT.key(), Double.class)).thenReturn(0.1);
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT.key(), Double.class)).thenReturn(0.015);

        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT.key(), Integer.class)).thenReturn(100);
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT.key(), Integer.class)).thenReturn(200);

        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT.key(), String.class)).thenReturn("PT10S");
        when(configuration.getProperty(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT.key(), String.class)).thenReturn("PT50S");
        assertWithMockedValidation(() -> {
            final ConstraintViolation constraintViolationTemporal = prepareConstraintViolation(
                "Invalid temporal default value, 'default' should be greater than 'limit'"
            );
            final ConstraintViolation constraintViolationCount = prepareConstraintViolation(
                "Invalid count default value, 'default' should be greater than 'limit'"
            );
            final ConstraintViolation constraintViolationProbabilistic = prepareConstraintViolation(
                "Invalid probabilistic default value, 'default' should be lower than 'limit'"
            );
            when(validator.validate(any())).thenReturn(
                Set.of(constraintViolationTemporal, constraintViolationCount, constraintViolationProbabilistic)
            );
            assertThatThrownBy(() -> cut.validateProperties())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContainingAll(
                    "Invalid temporal default value, 'default' should be greater than 'limit'",
                    "Invalid count default value, 'default' should be greater than 'limit'",
                    "Invalid probabilistic default value, 'default' should be lower than 'limit'"
                );
        });
    }

    private void assertWithMockedValidation(Runnable testCase) {
        try (MockedStatic<Validation> validationMock = Mockito.mockStatic(Validation.class)) {
            validationMock.when(Validation::buildDefaultValidatorFactory).thenReturn(validatorFactory);
            when(validatorFactory.getValidator()).thenReturn(validator);

            cut = new GraviteePropertiesValidator(configuration);

            testCase.run();
        }
    }

    private static ConstraintViolation prepareConstraintViolation(String message) {
        final ConstraintViolation constraintViolationTemporal = mock(ConstraintViolation.class);
        when(constraintViolationTemporal.getMessage()).thenReturn(message);
        return constraintViolationTemporal;
    }
}
