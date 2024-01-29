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

package io.gravitee.apim.core.integration.use_case;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class IntegrationImportUseCase {

    private final IntegrationDomainService integrationDomainService;
    private final IntegrationCrudService integrationCrudService;

    public IntegrationImportUseCase.Output execute(IntegrationImportUseCase.Input input) {
        var integrationId = input.integrationId();
        var auditInfo = input.auditInfo();

        var integration = integrationCrudService.findById(integrationId);

        return new Output(
            integrationDomainService
                .fetchEntities(integration, input.entities())
                .map(integrationEntity -> integrationDomainService.importApi(integrationEntity, auditInfo, integration))
                .ignoreElements()
        );
    }

    @Builder
    public record Input(String integrationId, List<IntegrationEntity> entities, AuditInfo auditInfo) {}

    public record Output(Completable completable) {}
}
