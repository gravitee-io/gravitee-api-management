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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.domain_service.DeployApiProductDomainService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Publishes DEPLOY_API_PRODUCT event to trigger gateway sync.
 * Use when an API Product needs to be deployed or redeployed without updating its definition.
 */
@UseCase
@RequiredArgsConstructor
public class DeployApiProductUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final LicenseDomainService licenseDomainService;
    private final ValidateApiProductService validateApiProductService;
    private final DeployApiProductDomainService deployApiProductDomainService;

    public Output execute(Input input) {
        if (!licenseDomainService.isApiProductDeploymentAllowed(input.auditInfo().organizationId())) {
            throw new ForbiddenFeatureException("api-product");
        }
        Optional<ApiProduct> apiProductOpt = apiProductQueryService.findById(input.apiProductId());
        if (apiProductOpt.isEmpty()) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }
        ApiProduct apiProduct = apiProductOpt.get();
        validateApiProductService.validateForDeploy(apiProduct);
        deployApiProductDomainService.deploy(input.auditInfo(), apiProduct);
        return new Output(apiProduct);
    }

    public record Input(String apiProductId, AuditInfo auditInfo) {}

    public record Output(ApiProduct apiProduct) {}
}
