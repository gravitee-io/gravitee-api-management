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
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.model.ApiProductKindFilter;
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
    void should_throw_when_the_product_is_of_a_kind_the_caller_does_not_manage() {
        ApiProduct workspace = ApiProduct.builder()
            .id("p1")
            .name("An AI workspace")
            .environmentId(ENV_ID)
            .kind(ApiProductKind.AI_WORKSPACE)
            .build();
        apiProductQueryService.initWith(Collections.singletonList(workspace));

        // Absent rather than forbidden, as for another environment: a surface that cannot manage a product
        // should not confirm that it exists either.
        assertThatThrownBy(() ->
            cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "p1", ApiProductKindFilter.classicOnly()))
        ).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_pass_a_classic_product_to_a_caller_that_manages_only_those() {
        ApiProduct classic = ApiProduct.builder().id("p1").name("P").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(Collections.singletonList(classic));

        // A classic product carries no kind, which is what classicOnly() matches.
        assertThatCode(() ->
            cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "p1", ApiProductKindFilter.classicOnly()))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_pass_any_kind_when_the_caller_names_no_filter() {
        ApiProduct workspace = ApiProduct.builder()
            .id("p1")
            .name("An AI workspace")
            .environmentId(ENV_ID)
            .kind(ApiProductKind.AI_WORKSPACE)
            .build();
        apiProductQueryService.initWith(Collections.singletonList(workspace));

        // The two-argument form is what every pre-existing caller uses, and it must keep answering for all kinds.
        assertThatCode(() -> cut.execute(new VerifyApiProductExistsUseCase.Input(ENV_ID, "p1"))).doesNotThrowAnyException();
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
