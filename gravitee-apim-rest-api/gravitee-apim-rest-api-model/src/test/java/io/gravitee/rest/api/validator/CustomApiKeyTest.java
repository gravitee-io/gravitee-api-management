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
package io.gravitee.rest.api.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomApiKeyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomApiKeyTest.class);
    private Validator validator;

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "Contains_at_least_8_chars", 0, null },
                { "less8", 1, "Should have length between 8 and 128 characters" },
                {
                    "VeryLongLengthTOHaveMoreThan128charsVeryLongLengthTOHaveMoreThan128charsVeryLongLengthTOHaveMoreThan128charsVeryLongLengthTOHaveMoreThan128chars",
                    1,
                    "Should have length between 8 and 128 characters",
                },
                { "No pattern compliant", 1, "Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)" },
            }
        );
    }

    @BeforeEach
    public void setUp() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void shouldTestCustomApiKeyValidation(String customApiKeyParam, int violationSize, String message) {
        LOGGER.info("Execute custom API Key validation test for: " + customApiKeyParam);

        CustomApiKeyObject customApiKeyObject = new CustomApiKeyObject(customApiKeyParam);
        Set<ConstraintViolation<CustomApiKeyObject>> violations = validator.validate(customApiKeyObject);

        Assertions.assertEquals(violations.size(), violationSize);

        if (violationSize > 0) {
            Assertions.assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(message)));
        }
    }

    public static class CustomApiKeyObject {

        @CustomApiKey
        private String apiKey;

        public CustomApiKeyObject(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
