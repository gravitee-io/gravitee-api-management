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
package io.gravitee.apim.core.async_api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.async_api.exception.AsyncApiContentEmptyException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AsyncApiValidatorTest {

    private final AsyncApiValidator validator = new AsyncApiValidator();

    @Nested
    class ValidateNotEmpty {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "asyncapi: '3.0.0'\ninfo:\n  title: Test", "asyncapi: '2.6.0'\ninfo:\n  title: Test", "  \n  asyncapi: '3.0.0'  \n  ",
            }
        )
        void should_accept_valid_content(String validContent) {
            assertThatCode(() -> validator.validateNotEmpty(new AsyncApi(validContent))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "  \n  \t  " })
        void should_throw_exception_when_content_is_invalid(String invalidContent) {
            assertThatThrownBy(() -> validator.validateNotEmpty(new AsyncApi(invalidContent)))
                .isInstanceOf(AsyncApiContentEmptyException.class)
                .hasMessage("Content must not be null or empty");
        }
    }
}
