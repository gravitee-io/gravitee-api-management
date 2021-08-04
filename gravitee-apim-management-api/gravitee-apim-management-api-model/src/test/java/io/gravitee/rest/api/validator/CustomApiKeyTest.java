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
package io.gravitee.rest.api.validator;

import java.util.Arrays;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class CustomApiKeyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomApiKeyTest.class);
    private Validator validator;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "Contains_at_least_8_chars", 0, null },
                { "less8", 1, "Should have length between 8 and 64 characters" },
                {
                    "VeryLongLengthTOHaveMoreThan64charsVeryLongLengthTOHaveMoreThan64chars",
                    1,
                    "Should have length between 8 and 64 characters",
                },
                { "No pattern compliant", 1, "Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)" },
            }
        );
    }

    @Parameterized.Parameter(0)
    public String customApiKeyParam;

    @Parameterized.Parameter(1)
    public int violationSize;

    @Parameterized.Parameter(2)
    public String message;

    @Before
    public void setUp() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void shoultTestCustomApiKeyValidation() {
        LOGGER.info("Exectute custom API key validation test for: " + this.customApiKeyParam);

        CustomApiKeyObject customApiKeyObject = new CustomApiKeyObject(this.customApiKeyParam);
        Set<ConstraintViolation<CustomApiKeyObject>> violations = validator.validate(customApiKeyObject);

        Assert.assertEquals(violations.size(), this.violationSize);

        if (violationSize > 0) {
            Assert.assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(message)));
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
