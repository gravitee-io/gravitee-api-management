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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.AllowInApiProductDomainService;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import lombok.CustomLog;

@UseCase
@CustomLog
public class ImportApiDefinitionUseCase {

    public record Input(ImportDefinition importDefinition, AuditInfo auditInfo) {}

    public record Output(ApiWithFlows apiWithFlows) {}

    private final ApiCrudService apiCrudService;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;
    private final ApiProductQueryService apiProductQueryService;
    private final AllowInApiProductDomainService allowInApiProductDomainService;

    public ImportApiDefinitionUseCase(
        ApiCrudService apiCrudService,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService,
        ApiProductQueryService apiProductQueryService,
        AllowInApiProductDomainService allowInApiProductDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
        this.apiProductQueryService = apiProductQueryService;
        this.allowInApiProductDomainService = allowInApiProductDomainService;
    }

    public Output execute(Input input) {
        ensureIsV4Api(input.importDefinition().getApiExport());
        ensureApiDoesNotExist(input);
        normalizeAllowInApiProduct(input.importDefinition().getApiExport());

        var createdApi = importDefinitionCreateDomainService.create(input.auditInfo, input.importDefinition);
        return new Output(createdApi);
    }

    /**
     * Check no API with the same ID already exist, else throw an exception. Then create the API with the ID from input.
     */
    private void ensureApiDoesNotExist(Input input) {
        var apiId = input.importDefinition().getApiExport().getId();
        if (apiId != null && apiCrudService.existsById(apiId)) {
            throw new ApiAlreadyExistsException(apiId);
        }
    }

    private void ensureIsV4Api(ApiExport api) {
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(api.getDefinitionVersion().getLabel());
        }
    }

    private void normalizeAllowInApiProduct(ApiExport apiExport) {
        if (apiExport.getType() != ApiType.PROXY) {
            return;
        }

        String targetApiId = apiExport.getId();
        boolean isAlreadyInProduct = targetApiId != null && !apiProductQueryService.findByApiId(targetApiId).isEmpty();

        Boolean normalized = allowInApiProductDomainService.normalizeForImport(apiExport.getAllowInApiProduct(), isAlreadyInProduct);
        apiExport.setAllowInApiProduct(normalized);
    }
}
