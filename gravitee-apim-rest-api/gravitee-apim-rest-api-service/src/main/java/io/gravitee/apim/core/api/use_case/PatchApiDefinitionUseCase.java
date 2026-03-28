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

import static java.util.Collections.emptyList;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.rest.api.model.JsonPatch;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.JsonPatchTestFailedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class PatchApiDefinitionUseCase {

    private final ApiExportDomainService apiExportDomainService;
    private final JsonPatchService jsonPatchService;
    private final GraviteeDefinitionSerializer graviteeDefinitionSerializer;

    public Result execute(Input input) {
        var exported = apiExportDomainService.export(input.apiId(), input.auditInfo(), emptyList());
        var definition = switch (exported) {
            case GraviteeDefinition.V4 v4 -> v4;
            case GraviteeDefinition.Native nativeV4 -> nativeV4;
            case null -> throw new ApiNotFoundException(input.apiId());
            default -> throw new ApiDefinitionVersionNotSupportedException(exported.api().definitionVersion().getLabel());
        };

        String definitionJson = exportDefinitionToJson(definition);
        try {
            String definitionModified = jsonPatchService.execute(definitionJson, input.patches());
            if (input.dryRun()) {
                return new Result.DryRun(definitionModified);
            }
            return new Result.ApplyPatch(definitionModified);
        } catch (JsonPatchTestFailedException e) {
            log.debug("JSON Patch definition test did not apply for API [{}]", input.apiId(), e);
            return new Result.PatchTestFailed();
        }
    }

    private String exportDefinitionToJson(GraviteeDefinition definition) {
        try {
            return graviteeDefinitionSerializer.serialize(definition);
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Failed to serialize API definition", e);
        }
    }

    public record Input(String apiId, AuditInfo auditInfo, Collection<JsonPatch> patches, boolean dryRun) {}

    public sealed interface Result permits Result.PatchTestFailed, Result.DryRun, Result.ApplyPatch {
        record PatchTestFailed() implements Result {}

        record DryRun(String patchedExportJson) implements Result {}

        record ApplyPatch(String patchedExportJson) implements Result {}
    }
}
