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

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationAlreadyExistException;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class IntegrationRemoteCreateUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final IntegrationQueryService integrationQueryService;

    //@Override
    public Output execute(final Input input) {
        var integrationToCreate = input.integration();
        Optional<IntegrationEntity> existingIntegrationOpt = integrationQueryService.findByEnvironmentIdAndRemoteId(
            integrationToCreate.getEnvironmentId(),
            integrationToCreate.getRemoteId()
        );
        if (existingIntegrationOpt.isPresent()) {
            IntegrationEntity integration = existingIntegrationOpt.get();
            if (!integration.getProvider().equals(integrationToCreate.getProvider())) {
                throw new IntegrationAlreadyExistException(integration.getRemoteId(), integration.getProvider());
            }
            return new Output(integration);
        } else {
            IntegrationEntity integrationCreated = integrationCrudService.create(integrationToCreate);
            return new Output(integrationCreated);
        }
    }

    @Builder
    public record Input(IntegrationEntity integration) {}

    record Output(IntegrationEntity createdIntegration) {}
}
