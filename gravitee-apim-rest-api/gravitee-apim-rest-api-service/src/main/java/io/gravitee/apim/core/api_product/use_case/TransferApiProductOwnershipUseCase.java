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

import static io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.permissions.RoleScope;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class TransferApiProductOwnershipUseCase {

    private final MembershipDomainService membershipDomainService;
    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductIndexerDomainService apiProductIndexerDomainService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    public record Input(TransferOwnership transferOwnership, String apiProductId, AuditInfo auditInfo) {}

    public record Output() {}

    public Output execute(Input input) {
        membershipDomainService.transferOwnership(input.transferOwnership(), RoleScope.API_PRODUCT, input.apiProductId());

        ApiProduct apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));

        PrimaryOwnerEntity primaryOwner = null;
        try {
            primaryOwner = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(
                input.auditInfo().organizationId(),
                apiProduct.getId()
            );
        } catch (ApiProductPrimaryOwnerNotFoundException ex) {
            log.debug("Failed to retrieve API Product primary owner, will index without primary owner", ex);
        }
        apiProductIndexerDomainService.index(oneShotIndexation(input.auditInfo()), apiProduct, primaryOwner);

        return new Output();
    }
}
