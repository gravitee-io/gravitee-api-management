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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportApiUseCaseTest {

    @Mock
    ApiExportDomainService apiExportDomainService;

    @InjectMocks
    ExportApiUseCase sut;

    AuditInfo auditInfo = AuditInfo.builder().build();

    @ParameterizedTest
    @CsvSource(
        delimiterString = "|",
        textBlock = """
        MyAPI     |  3.14.159  | myapi-3.14.159.json
        My API    |  3.14 .159 | my-api-3.14-.159.json
        My    API |  3.14.159  | my-api-3.14.159.json
     """
    )
    void shouldExportApi(String name, String version, String expectedFilename) {
        // Given
        var exportedDefinition = buildDefinition("apiId", name, version);
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(exportedDefinition);

        // When
        var output = sut.execute(exportedDefinition.api().id(), auditInfo, Set.of());

        // Then
        assertThat(output.filename()).isEqualTo(expectedFilename);
        assertThat(output.definition()).isSameAs(exportedDefinition);
    }

    @Test
    void shouldExportNativeApi() {
        // Given
        var exportedDefinition = buildNativeDefinition("apiId", "MyAPI", "3.14.159");
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(exportedDefinition);

        // When
        var output = sut.execute(exportedDefinition.api().id(), auditInfo, Set.of());

        // Then
        assertThat(output.definition()).isSameAs(exportedDefinition);
    }

    @Test
    void shouldNotExportFederatedApi() {
        // Given
        var exportedDefinition = buildFederatedDefinition("apiId", "MyAPI", "3.14.159");
        when(apiExportDomainService.export(any(), any(), any())).thenReturn(exportedDefinition);

        // When
        var throwable = catchThrowable(() -> sut.execute(exportedDefinition.api().id(), auditInfo, Set.of()));

        // Then
        assertThat(throwable).isInstanceOf(ApiDefinitionVersionNotSupportedException.class);
    }

    private static GraviteeDefinition.V4 buildDefinition(String id, String apiName, String apiVersion) {
        var api = ApiDescriptor.ApiDescriptorV4.builder().id(id).name(apiName).apiVersion(apiVersion).build();
        return GraviteeDefinition.V4.builder().api(api).build();
    }

    private static GraviteeDefinition.Native buildNativeDefinition(String id, String apiName, String apiVersion) {
        var api = ApiDescriptor.ApiDescriptorNative.builder().id(id).name(apiName).apiVersion(apiVersion).build();
        return GraviteeDefinition.Native.builder().api(api).build();
    }

    private static GraviteeDefinition.Federated buildFederatedDefinition(String id, String apiName, String apiVersion) {
        var api = ApiDescriptor.ApiDescriptorFederated.builder().id(id).name(apiName).apiVersion(apiVersion).build();
        return GraviteeDefinition.Federated.builder().api(api).build();
    }
}
