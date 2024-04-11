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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.AssociatedApisFoundException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class DeleteIntegrationUseCase {

    IntegrationCrudService integrationCrudService;
    ApiQueryService apiQueryService;

    public DeleteIntegrationUseCase(IntegrationCrudService integrationCrudService, ApiQueryService apiQueryService) {
        this.integrationCrudService = integrationCrudService;
        this.apiQueryService = apiQueryService;
    }

    public void execute(Input input) {
        apiQueryService
            .search(
                ApiSearchCriteria.builder().integrationId(input.integrationId).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .findAny()
            .ifPresentOrElse(
                api -> {
                    throw new AssociatedApisFoundException(input.integrationId);
                },
                () ->
                    integrationCrudService
                        .findById(input.integrationId)
                        .ifPresentOrElse(
                            integration -> integrationCrudService.delete(integration.getId()),
                            () -> {
                                throw new IntegrationNotFoundException(input.integrationId);
                            }
                        )
            );
    }

    @Builder
    public record Input(String integrationId) {}
}
