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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.JsonPatch;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.exceptions.JsonPatchTestFailedException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatchApiDefinitionUseCaseTest {

    private static final String API_ID = "api-id";

    @Mock
    private ApiExportDomainService apiExportDomainService;

    @Mock
    private JsonPatchService jsonPatchService;

    @Mock
    private GraviteeDefinitionSerializer graviteeDefinitionSerializer;

    private PatchApiDefinitionUseCase useCase;

    @BeforeEach
    void set_up() {
        useCase = new PatchApiDefinitionUseCase(apiExportDomainService, jsonPatchService, graviteeDefinitionSerializer);
    }

    @Test
    void should_return_dry_run_with_patched_json() throws JsonProcessingException {
        var definition = minimalExport("n1");
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(definition);
        when(graviteeDefinitionSerializer.serialize(any())).thenReturn("{}");
        when(jsonPatchService.execute(anyString(), any())).thenReturn("{\"patched\":true}");

        var patches = List.<JsonPatch>of();
        var result = useCase.execute(new PatchApiDefinitionUseCase.Input(API_ID, auditInfo(), patches, true));

        assertThat(result).isInstanceOf(PatchApiDefinitionUseCase.Result.DryRun.class);
        assertThat(((PatchApiDefinitionUseCase.Result.DryRun) result).patchedExportJson()).isEqualTo("{\"patched\":true}");
    }

    @Test
    void should_return_apply_patch_when_not_dry_run() throws JsonProcessingException {
        var definition = minimalExport("n1");
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(definition);
        when(graviteeDefinitionSerializer.serialize(any())).thenReturn("{}");
        when(jsonPatchService.execute(anyString(), any())).thenReturn("{\"patched\":true}");

        var result = useCase.execute(new PatchApiDefinitionUseCase.Input(API_ID, auditInfo(), List.of(), false));

        assertThat(result).isInstanceOf(PatchApiDefinitionUseCase.Result.ApplyPatch.class);
        assertThat(((PatchApiDefinitionUseCase.Result.ApplyPatch) result).patchedExportJson()).isEqualTo("{\"patched\":true}");
    }

    @Test
    void should_return_patch_test_failed_when_json_patch_service_throws() throws JsonProcessingException {
        var definition = minimalExport("n1");
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(definition);
        when(graviteeDefinitionSerializer.serialize(any())).thenReturn("{}");
        var failed = new JsonPatchTestFailedException(new JsonPatch());
        when(jsonPatchService.execute(anyString(), any())).thenThrow(failed);

        var result = useCase.execute(new PatchApiDefinitionUseCase.Input(API_ID, auditInfo(), List.of(), false));

        assertThat(result).isInstanceOf(PatchApiDefinitionUseCase.Result.PatchTestFailed.class);
        verify(jsonPatchService).execute(anyString(), any());
    }

    private static AuditInfo auditInfo() {
        return new AuditInfo("o", "e", AuditActor.builder().userId("u").userSource("source").userSourceId("sid").build());
    }

    private static GraviteeDefinition.V4 minimalExport(String name) {
        return (GraviteeDefinition.V4) GraviteeDefinition.from(
            ApiDescriptor.ApiDescriptorV4.builder().id(API_ID).crossId("c").name(name).apiVersion("1.0").type(ApiType.PROXY).build(),
            Set.of(),
            List.of(),
            List.of(),
            List.<PlanDescriptor.V4>of(),
            List.of(),
            null,
            null
        );
    }
}
