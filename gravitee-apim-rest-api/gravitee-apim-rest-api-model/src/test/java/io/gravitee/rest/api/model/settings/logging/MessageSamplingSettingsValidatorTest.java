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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageSamplingSettingsValidatorTest {

    private MessageSamplingSettingsValidator cut;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @BeforeEach
    void setUp() {
        cut = new MessageSamplingSettingsValidator();
    }

    @Test
    void should_accept_valid_message_sampling() {
        final MessageSampling messageSampling = MessageSampling.builder()
            .count(MessageSampling.Count.builder().defaultValue(40).limit(15).build())
            .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.25).limit(0.75).build())
            .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT2S").build())
            .build();

        assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isTrue();
        verify(constraintViolationBuilder, never()).addConstraintViolation();
    }

    @Test
    void should_fail_when_count_settings_default_lower_than_limit() {
        final MessageSampling messageSampling = MessageSampling.builder()
            .count(MessageSampling.Count.builder().defaultValue(40).limit(150).build())
            .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.25).limit(0.75).build())
            .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT2S").build())
            .build();

        ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
            constraintViolationBuilder
        );
        assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
        verify(constraintViolationBuilder).addConstraintViolation();
        assertThat(violationCaptor.getAllValues())
            .hasSize(1)
            .first()
            .hasToString("Invalid count default value, 'default' should be greater than 'limit'");
    }

    @Test
    void should_fail_when_probabilistic_settings_default_greater_than_limit() {
        final MessageSampling messageSampling = MessageSampling.builder()
            .count(MessageSampling.Count.builder().defaultValue(40).limit(15).build())
            .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(1.0).limit(0.75).build())
            .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT2S").build())
            .build();

        ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
            constraintViolationBuilder
        );
        assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
        verify(constraintViolationBuilder).addConstraintViolation();
        assertThat(violationCaptor.getAllValues())
            .hasSize(1)
            .first()
            .hasToString("Invalid probabilistic default value, 'default' should be lower than 'limit'");
    }

    @Nested
    class Temporal {

        @Test
        void should_fail_when_temporal_settings_default_is_not_iso_8601() {
            final MessageSampling messageSampling = MessageSampling.builder()
                .count(MessageSampling.Count.builder().defaultValue(40).limit(15).build())
                .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.25).limit(0.75).build())
                .temporal(MessageSampling.Temporal.builder().defaultValue("PT-five-seconds").limit("PT2S").build())
                .build();

            ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
            when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
                constraintViolationBuilder
            );
            assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
            verify(constraintViolationBuilder).addConstraintViolation();
            assertThat(violationCaptor.getAllValues())
                .hasSize(1)
                .first()
                .hasToString("Invalid temporal default value, 'default' should be a valid ISO-8601 date");
        }

        @Test
        void should_fail_when_temporal_settings_limit_is_not_iso_8601() {
            final MessageSampling messageSampling = MessageSampling.builder()
                .count(MessageSampling.Count.builder().defaultValue(40).limit(15).build())
                .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.25).limit(0.75).build())
                .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT-two-seconds").build())
                .build();

            ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
            when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
                constraintViolationBuilder
            );
            assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
            verify(constraintViolationBuilder).addConstraintViolation();
            assertThat(violationCaptor.getAllValues())
                .hasSize(1)
                .first()
                .hasToString("Invalid temporal limit value, 'limit' should be a valid ISO-8601 date");
        }

        @Test
        void should_fail_when_temporal_settings_default_lower_than_limit() {
            final MessageSampling messageSampling = MessageSampling.builder()
                .count(MessageSampling.Count.builder().defaultValue(40).limit(15).build())
                .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.25).limit(0.75).build())
                .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT20S").build())
                .build();

            ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
            when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
                constraintViolationBuilder
            );
            assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
            verify(constraintViolationBuilder).addConstraintViolation();
            assertThat(violationCaptor.getAllValues())
                .hasSize(1)
                .first()
                .hasToString("Invalid temporal default value, 'default' should be greater than 'limit'");
        }
    }

    @Test
    void should_fail_when_multiple_settings_are_not_valid() {
        final MessageSampling messageSampling = MessageSampling.builder()
            .count(MessageSampling.Count.builder().defaultValue(40).limit(105).build())
            .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(1.0).limit(0.75).build())
            .temporal(MessageSampling.Temporal.builder().defaultValue("PT5S").limit("PT20S").build())
            .build();

        ArgumentCaptor<String> violationCaptor = ArgumentCaptor.forClass(String.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(violationCaptor.capture())).thenReturn(
            constraintViolationBuilder
        );
        assertThat(cut.isValid(messageSampling, constraintValidatorContext)).isFalse();
        verify(constraintViolationBuilder, times(3)).addConstraintViolation();
        assertThat(violationCaptor.getAllValues())
            .hasSize(3)
            .containsExactly(
                "Invalid count default value, 'default' should be greater than 'limit'",
                "Invalid probabilistic default value, 'default' should be lower than 'limit'",
                "Invalid temporal default value, 'default' should be greater than 'limit'"
            );
    }
}
