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

import static java.util.Objects.requireNonNull;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ExportApiUseCase {

    private final ApiExportDomainService apiExportDomainService;

    public Output execute(Input input) {
        var exported = apiExportDomainService.export(input.apiId(), input.auditInfo(), input.excluded());
        return switch (exported) {
            case GraviteeDefinition.V4 v4 -> new Output(v4);
            case GraviteeDefinition.Native nativeV4 -> new Output(nativeV4);
            case null -> throw new ApiNotFoundException(input.apiId());
            default -> throw new ApiDefinitionVersionNotSupportedException(exported.api().definitionVersion().getLabel());
        };
    }

    public record Input(String apiId, AuditInfo auditInfo, @Nullable Collection<Excludable> excluded) {
        public static Input of(String apiId, AuditInfo auditInfo, @Nullable Collection<Excludable> excluded) {
            return new Input(
                requireNonNull(apiId, "apiId should not be null"),
                requireNonNull(auditInfo, "auditInfo should not be null"),
                excluded == null ? List.of() : excluded
            );
        }
    }

    public record Output(GraviteeDefinition definition, String filename) {
        public Output(GraviteeDefinition definition) {
            this(definition, getExportFilename(definition.api()));
        }
    }

    private static String getExportFilename(ApiDescriptor api) {
        return "%s-%s.json".formatted(api.name(), api.apiVersion()).trim().toLowerCase().replaceAll("[^\\w\\r\\n\\t\\f\\v.]+", "-");
    }
}
