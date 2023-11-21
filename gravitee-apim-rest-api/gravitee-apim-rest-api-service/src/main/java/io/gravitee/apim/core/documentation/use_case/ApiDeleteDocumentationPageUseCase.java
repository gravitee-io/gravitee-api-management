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
package io.gravitee.apim.core.documentation.use_case;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.DeleteApiDocumentationDomainService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

public class ApiDeleteDocumentationPageUseCase {

    private final DeleteApiDocumentationDomainService deleteApiDocumentationDomainService;
    private final ApiCrudService apiCrudService;

    public ApiDeleteDocumentationPageUseCase(
        DeleteApiDocumentationDomainService deleteApiDocumentationDomainService,
        ApiCrudService apiCrudService
    ) {
        this.deleteApiDocumentationDomainService = deleteApiDocumentationDomainService;
        this.apiCrudService = apiCrudService;
    }

    public void execute(Input input) {
        final Api api = apiCrudService.get(input.apiId);
        deleteApiDocumentationDomainService.delete(api, input.pageId, input.auditInfo);
    }

    public record Input(String apiId, String pageId, AuditInfo auditInfo) {}
}
