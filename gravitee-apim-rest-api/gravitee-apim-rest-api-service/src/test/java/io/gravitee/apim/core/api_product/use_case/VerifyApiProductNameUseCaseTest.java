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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifyApiProductNameUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private VerifyApiProductNameUseCase verifyApiProductNameUseCase;

    @BeforeEach
    void setUp() {
        verifyApiProductNameUseCase = new VerifyApiProductNameUseCase(apiProductQueryService);
    }

    @Test
    void should_return_name_if_available() {
        var input = new VerifyApiProductNameUseCase.Input(ENV_ID, "Unique Name", null);
        var output = verifyApiProductNameUseCase.execute(input);
        assertThat(output.sanitizedName()).isEqualTo("Unique Name");
    }

    @Test
    void should_throw_exception_if_name_is_empty() {
        var input = new VerifyApiProductNameUseCase.Input(ENV_ID, "", null);
        Assertions.assertThatThrownBy(() -> verifyApiProductNameUseCase.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("cannot be empty");
    }

    @Test
    void should_throw_exception_if_name_exists() {
        ApiProduct existing = ApiProduct.builder().id("id1").name("Existing Name").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(Collections.singletonList(existing));
        var input = new VerifyApiProductNameUseCase.Input(ENV_ID, "Existing Name", null);
        Assertions.assertThatThrownBy(() -> verifyApiProductNameUseCase.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("already exists");
    }
}
