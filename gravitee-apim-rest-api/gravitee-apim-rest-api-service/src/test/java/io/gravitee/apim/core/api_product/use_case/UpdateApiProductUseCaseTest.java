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
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.UpdateApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private UpdateApiProductUseCase updateApiProductUseCase;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        updateApiProductUseCase = new UpdateApiProductUseCase(apiProductCrudService, auditService, apiProductQueryService);
    }

    @Test
    void should_update_api_product() {
        ApiProduct existing = ApiProduct.builder()
            .id("api-product-id")
            .name("Old Name")
            .version("1.0.0")
            .description("desc")
            .apiIds(Set.of("api-1"))
            .environmentId(ENV_ID)
            .build();
        apiProductCrudService.initWith(List.of(existing));
        apiProductQueryService.initWith(List.of(existing));

        var toUpdate = UpdateApiProduct.builder().name("New Name").version("2.0.0").description("new desc").apiIds(Set.of("api-2")).build();

        var input = new UpdateApiProductUseCase.Input("api-product-id", toUpdate, AUDIT_INFO);
        var output = updateApiProductUseCase.execute(input);

        assertAll(
            () -> assertThat(output.apiProduct().getId()).isEqualTo("api-product-id"),
            () -> assertThat(output.apiProduct().getName()).isEqualTo("New Name"),
            () -> assertThat(output.apiProduct().getVersion()).isEqualTo("2.0.0"),
            () -> assertThat(output.apiProduct().getDescription()).isEqualTo("new desc"),
            () -> assertThat(output.apiProduct().getApiIds()).containsExactly("api-2")
        );
    }

    @Test
    void should_throw_exception_if_api_product_not_found() {
        var toUpdate = UpdateApiProduct.builder().name("Name").version("1.0.0").build();
        var input = new UpdateApiProductUseCase.Input("missing-id", toUpdate, AUDIT_INFO);
        Assertions.assertThatThrownBy(() -> updateApiProductUseCase.execute(input))
            .isInstanceOf(ApiProductNotFoundException.class)
            .hasMessageContaining("API Product not found");
    }
}
