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
package io.gravitee.rest.api.management.v2.rest.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.CreateIntegration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreateIntegrationValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String NAME_FIELD = "name";
    private static final String DESCRIPTION_FIELD = "description";
    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 250;

    @ParameterizedTest
    @MethodSource("acceptedCreateIntegrations")
    void should_accept_name_and_description_within_limits(CreateIntegration createIntegration) {
        assertThat(VALIDATOR.validate(createIntegration)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rejectedCreateIntegrations")
    void should_reject_invalid_field_with_its_name(CreateIntegration createIntegration, String invalidField) {
        assertThat(VALIDATOR.validate(createIntegration))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactly(invalidField);
    }

    static Stream<Arguments> acceptedCreateIntegrations() {
        return Stream.of(
            Arguments.of(Named.of("name at max length", anIntegration("n".repeat(NAME_MAX_LENGTH), null))),
            Arguments.of(Named.of("name at min length", anIntegration("n", null))),
            Arguments.of(Named.of("description at max length", anIntegration("n", "d".repeat(DESCRIPTION_MAX_LENGTH))))
        );
    }

    static Stream<Arguments> rejectedCreateIntegrations() {
        return Stream.of(
            Arguments.of(Named.of("missing name", anIntegration(null, null)), NAME_FIELD),
            Arguments.of(Named.of("name below min length", anIntegration("", null)), NAME_FIELD),
            Arguments.of(Named.of("name over max length", anIntegration("n".repeat(NAME_MAX_LENGTH + 1), null)), NAME_FIELD),
            Arguments.of(
                Named.of("description over max length", anIntegration("n", "d".repeat(DESCRIPTION_MAX_LENGTH + 1))),
                DESCRIPTION_FIELD
            )
        );
    }

    private static CreateIntegration anIntegration(String name, String description) {
        return new CreateIntegration().name(name).description(description).provider("test-provider");
    }
}
