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
import io.gravitee.apim.core.api.domain_service.FetchApiDefinitionFromUrlDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import lombok.CustomLog;

@UseCase
@CustomLog
public class ImportApiDefinitionFromUrlUseCase {

    public record Input(String url, java.util.List<String> whitelist, boolean allowPrivate, AuditInfo auditInfo) {}

    public record Output(String apiDefinitionContent) {}

    private final FetchApiDefinitionFromUrlDomainService fetchApiDefinitionFromUrlDomainService;

    public ImportApiDefinitionFromUrlUseCase(FetchApiDefinitionFromUrlDomainService fetchApiDefinitionFromUrlDomainService) {
        this.fetchApiDefinitionFromUrlDomainService = fetchApiDefinitionFromUrlDomainService;
    }

    public Output execute(Input input) {
        try {
            String apiDefinitionContent = fetchApiDefinitionFromUrlDomainService.fetch(
                input.url(),
                input.whitelist(),
                input.allowPrivate()
            );

            return new Output(apiDefinitionContent);
        } catch (UrlForbiddenException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException("Failed to fetch API definition from URL: " + e.getMessage(), e);
        }
    }
}
