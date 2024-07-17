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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateFederatedApiUseCase {

    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final UpdateFederatedApiDomainService apiUpdateFederatedApiDomainService;

    public Output execute(Input input) {
        var updateApi = input.apiToUpdate;
        var auditInfo = input.auditInfo;

        PrimaryOwnerEntity primaryOwnerEntity = apiPrimaryOwnerDomainService.getApiPrimaryOwner(
            auditInfo.organizationId(),
            updateApi.getId()
        );

        var updating = update(input.apiToUpdate);

        var updated = apiUpdateFederatedApiDomainService.update(input.apiToUpdate.getId(), updating, auditInfo, primaryOwnerEntity);

        return new Output(updated, primaryOwnerEntity);
    }

    @Builder
    public record Input(Api apiToUpdate, AuditInfo auditInfo) {}

    public record Output(Api updatedApi, PrimaryOwnerEntity primaryOwnerEntity) {}

    static UnaryOperator<Api> update(Api newOne) {
        return currentApi ->
            currentApi
                .toBuilder()
                .name(newOne.getName())
                .description(newOne.getDescription())
                .version(newOne.getVersion())
                .apiLifecycleState(newOne.getApiLifecycleState())
                .visibility(newOne.getVisibility())
                .labels(newOne.getLabels())
                .categories(newOne.getCategories())
                .groups(newOne.getGroups())
                .build();
    }
}
