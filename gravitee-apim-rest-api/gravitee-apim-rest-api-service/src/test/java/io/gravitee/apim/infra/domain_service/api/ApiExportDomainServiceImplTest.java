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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PlanExport;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiExportDomainServiceImplTest {

    @Mock
    ApiImportExportService exportService;

    @InjectMocks
    ApiExportDomainServiceImpl sut;

    @Test
    void exportServiceMustMapTypeWhenExportV4() {
        // Given
        String apiId = UUID.randomUUID().toString();
        ApiEntity api = new ApiEntity();
        api.setType(ApiType.PROXY);
        BasePlanEntity plan = new BasePlanEntity();
        plan.setId(UUID.randomUUID().toString());
        plan.setSecurity(new PlanSecurity().withType("api-key").withConfiguration("{}"));
        plan.setType(PlanType.API);
        when(exportService.exportApi(any(), any(), any(), any())).thenReturn(
            new ExportApiEntity(api, null, null, null, Set.of(plan), null)
        );

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build());

        // Then
        assertThat(export.getApi().getType()).isEqualTo(ApiType.PROXY);
        assertThat(export.getPlans()).map(PlanExport::getType).first().isEqualTo(Plan.PlanType.API);
        assertThat(export.getPlans()).map(PlanExport::getSecurity).first().isEqualTo(new PlanSecurity("API_KEY", "{}"));
    }
}
