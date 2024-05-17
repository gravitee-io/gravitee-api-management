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

import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.domain_service.ApiCRDExportDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiCRDExportDomainServiceImplTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final String API_ID = "api-id";

    @Mock
    ApiImportExportService exportService;

    @Mock
    ApiService apiService;

    ApiCRDExportDomainService apiCRDExportDomainService;

    @BeforeEach
    void setUp() {
        apiCRDExportDomainService = new ApiCRDExportDomainServiceImpl(exportService, apiService);
    }

    @Test
    void should_export_as_a_crd_spec_and_generate_cross_id() {
        when(exportService.exportApi(new ExecutionContext(ORG_ID, ENV_ID), API_ID, null, Set.of()))
            .thenReturn(exportApiEntity(apiEntity().build()));

        var spec = apiCRDExportDomainService.export(
            API_ID,
            AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).actor(AuditActor.builder().userId(USER_ID).build()).build()
        );

        verify(apiService, times(1))
            .update(
                eq(new ExecutionContext(ORG_ID, ENV_ID)),
                eq(API_ID),
                argThat(api -> StringUtils.isNotBlank(api.getCrossId())),
                eq(USER_ID)
            );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isNotBlank();
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
        });
    }

    @Test
    void should_export_as_a_crd_spec_and_keep_cross_id() {
        when(exportService.exportApi(new ExecutionContext(ORG_ID, ENV_ID), API_ID, null, Set.of()))
            .thenReturn(exportApiEntity(apiEntity().crossId("cross-id").build()));

        var spec = apiCRDExportDomainService.export(
            API_ID,
            AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).actor(AuditActor.builder().userId(USER_ID).build()).build()
        );

        verify(apiService, never()).update(any(ExecutionContext.class), anyString(), any(UpdateApiEntity.class), anyString());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
        });
    }

    private static ExportApiEntity exportApiEntity(ApiEntity apiEntity) {
        return ExportApiEntity
            .builder()
            .apiEntity(apiEntity)
            .plans(Set.of(PlanEntity.builder().name("plan-name").id("plan-id").security(new PlanSecurity("key-less", "{}")).build()))
            .build();
    }

    private static ApiEntity.ApiEntityBuilder apiEntity() {
        return ApiEntity
            .builder()
            .name("api-name")
            .id(API_ID)
            .listeners(List.of(HttpListener.builder().paths(List.of(new Path("/api-path"))).build()))
            .endpointGroups(
                List.of(
                    EndpointGroup
                        .builder()
                        .name("default-group")
                        .type("http-proxy")
                        .sharedConfiguration("{}")
                        .endpoints(
                            List.of(
                                Endpoint
                                    .builder()
                                    .name("default-endpoint")
                                    .type("http-proxy")
                                    .inheritConfiguration(true)
                                    .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                                    .build()
                            )
                        )
                        .build()
                )
            );
    }
}
