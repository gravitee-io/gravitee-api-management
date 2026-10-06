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
import io.gravitee.apim.core.api.domain_service.ApiCRDExportDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Export every V4 API of an environment as a CRD spec, each one exactly as {@link ExportApiCRDUseCase} would.
 * EDGE and AUTHZ APIs are not part of the automation contract and are left out.
 *
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ExportEnvironmentApiCRDsUseCase {

    private static final List<ApiType> UNSUPPORTED_TYPES = List.of(ApiType.EDGE, ApiType.AUTHZ);

    private final ApiQueryService apiQueryService;
    private final ApiCRDExportDomainService exportDomainService;

    public record Input(AuditInfo auditInfo, IDExportStrategy idExportStrategy, boolean exportNotifications) {}

    public record Output(List<ApiCRDSpec> specs) {}

    public Output execute(Input input) {
        var criteria = ApiSearchCriteria.builder()
            .environmentId(input.auditInfo().environmentId())
            .definitionVersion(List.of(DefinitionVersion.V4))
            .notApiTypes(UNSUPPORTED_TYPES)
            .build();
        var specs = apiQueryService
            .search(criteria, null, ApiFieldFilter.builder().definitionExcluded(true).pictureExcluded(true).build())
            // The automation mapper rejects these types, so never let a lenient query hand them over.
            .filter(api -> api.getType() == null || !UNSUPPORTED_TYPES.contains(api.getType()))
            .sorted(Comparator.comparing(Api::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(Api::getId))
            .map(Api::getId)
            .map(id -> exportDomainService.export(id, input.idExportStrategy(), input.auditInfo(), input.exportNotifications()))
            .toList();
        return new Output(specs);
    }
}
