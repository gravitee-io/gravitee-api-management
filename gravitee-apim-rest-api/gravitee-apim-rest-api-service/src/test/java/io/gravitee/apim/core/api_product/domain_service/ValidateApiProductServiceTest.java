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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateApiProductServiceTest {

    private ValidateApiProductService cut;

    @BeforeEach
    void setUp() {
        cut = new ValidateApiProductService();
    }

    @Test
    void should_validate_successfully_when_name_and_version_are_present() {
        // Given
        ApiProduct apiProduct = ApiProduct.builder().name("My API Product").version("1.0.0").build();

        // When / Then
        assertThatCode(() -> cut.validate(apiProduct)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_exception_when_name_is_empty() {
        // Given
        var apiProduct = ApiProduct.builder().version("1.0.0").build();

        // When / Then
        assertThatThrownBy(() -> cut.validate(apiProduct))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("API Product name is required");
    }

    @Test
    void should_throw_exception_when_version_is_empty() {
        // Given
        var apiProduct = ApiProduct.builder().name("My API Product").build();

        // When / Then
        assertThatThrownBy(() -> cut.validate(apiProduct))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("API Product version is required");
    }
}
