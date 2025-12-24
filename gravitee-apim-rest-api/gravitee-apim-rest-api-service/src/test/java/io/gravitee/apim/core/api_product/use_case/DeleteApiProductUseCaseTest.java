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
import inmemory.ApiProductCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private DeleteApiProductUseCase deleteApiProductUseCase;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        deleteApiProductUseCase = new DeleteApiProductUseCase(apiProductCrudService, auditService);
    }

    @Test
    void should_delete_api_product() {
        var productId = "api-product-id";
        apiProductCrudService.create(
            io.gravitee.apim.core.api_product.model.ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).build()
        );
        var input = new DeleteApiProductUseCase.Input(productId, AUDIT_INFO);
        deleteApiProductUseCase.execute(input);
        assertThat(apiProductCrudService.findById(productId)).isEmpty();
    }

    @Test
    void should_not_fail_if_product_does_not_exist() {
        var input = new DeleteApiProductUseCase.Input("missing-id", AUDIT_INFO);
        deleteApiProductUseCase.execute(input);
        assertThat(apiProductCrudService.findById("missing-id")).isEmpty();
    }
}
