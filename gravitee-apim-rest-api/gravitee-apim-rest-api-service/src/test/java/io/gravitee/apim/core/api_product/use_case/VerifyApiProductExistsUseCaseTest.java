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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifyApiProductExistsUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private VerifyApiProductExistsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new VerifyApiProductExistsUseCase(apiProductQueryService);
    }

    @Test
    void should_pass_when_product_exists_in_environment() {
        ApiProduct product = ApiProduct.builder().id("p1").name("P").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(Collections.singletonList(product));

        assertThatCode(() -> cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "p1"))).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_product_missing() {
        assertThatThrownBy(() -> cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "missing"))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_throw_when_product_belongs_to_another_environment() {
        ApiProduct product = ApiProduct.builder().id("p1").name("P").environmentId("other-env").build();
        apiProductQueryService.initWith(Collections.singletonList(product));

        assertThatThrownBy(() -> cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "p1"))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }
}
