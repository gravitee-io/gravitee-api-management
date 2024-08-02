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
package io.gravitee.apim.core.category.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.CategoryQueryServiceInMemory;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateCategoryIdsDomainServiceTest {

    CategoryQueryServiceInMemory categoryQueryService = new CategoryQueryServiceInMemory();

    ValidateCategoryIdsDomainService cut = new ValidateCategoryIdsDomainService(categoryQueryService);

    @BeforeEach
    void setUp() {
        categoryQueryService.initWith(List.of(Category.builder().key("key").id("id").build()));
    }

    @Test
    void should_return_same_input_and_no_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("id"));
        cut.validateAndSanitize(input).peek(sanitized -> assertThat(sanitized).isEqualTo(input), errors -> assertThat(errors).isEmpty());
    }

    @Test
    void should_return_input_with_ids_and_no_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("key"));
        var expected = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("id"));
        cut.validateAndSanitize(input).peek(sanitized -> assertThat(sanitized).isEqualTo(expected), errors -> assertThat(errors).isEmpty());
    }

    @Test
    void should_return_input_with_empty_ids_and_no_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", null);
        var expected = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of());
        cut.validateAndSanitize(input).peek(sanitized -> assertThat(sanitized).isEqualTo(expected), errors -> assertThat(errors).isEmpty());
    }

    @Test
    void should_clean_input_from_unknown_ids_with_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("id", "unknown"));
        var expected = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("id"));
        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> assertThat(sanitized).usingRecursiveComparison().isEqualTo(expected),
                errors ->
                    assertThat(errors)
                        .isEqualTo(List.of(Validator.Error.warning("category [unknown] is not defined in environment [DEFAULT]")))
            );
    }

    @Test
    void should_clean_input_from_unknown_keys_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("key", "unknown"));
        var expected = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("id"));
        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> assertThat(sanitized).usingRecursiveComparison().isEqualTo(expected),
                errors ->
                    assertThat(errors)
                        .isEqualTo(List.of(Validator.Error.warning("category [unknown] is not defined in environment [DEFAULT]")))
            );
    }

    @Test
    void should_return_empty_ids_and_warning() {
        var input = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of("unknown"));
        var expected = new ValidateCategoryIdsDomainService.Input("DEFAULT", Set.of());
        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> assertThat(sanitized).usingRecursiveComparison().isEqualTo(expected),
                errors ->
                    assertThat(errors)
                        .isEqualTo(List.of(Validator.Error.warning("category [unknown] is not defined in environment [DEFAULT]")))
            );
    }
}
