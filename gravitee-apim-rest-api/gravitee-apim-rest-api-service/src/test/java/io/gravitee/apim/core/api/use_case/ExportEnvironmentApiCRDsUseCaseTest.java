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

import fixtures.core.model.ApiCRDFixtures;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiCRDExportDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportEnvironmentApiCRDsUseCaseTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("organization-id").environmentId(ENVIRONMENT_ID).build();

    private record Export(String apiId, IDExportStrategy strategy, AuditInfo auditInfo, boolean exportNotifications) {}

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final List<Export> exports = new ArrayList<>();
    private final ApiCRDExportDomainService exportDomainService = (apiId, strategy, auditInfo, exportNotifications) -> {
        exports.add(new Export(apiId, strategy, auditInfo, exportNotifications));
        return ApiCRDFixtures.newBaseSpec().id(apiId).hrid(null).build();
    };

    private ExportEnvironmentApiCRDsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new ExportEnvironmentApiCRDsUseCase(apiQueryService, exportDomainService);
    }

    private static Api api(Api base, String id, String name) {
        return base.toBuilder().id(id).name(name).environmentId(ENVIRONMENT_ID).build();
    }

    private ExportEnvironmentApiCRDsUseCase.Output execute() {
        return cut.execute(new ExportEnvironmentApiCRDsUseCase.Input(AUDIT_INFO, IDExportStrategy.ALL, true));
    }

    @Test
    void should_export_one_crd_per_v4_api_of_the_environment() {
        apiQueryService.initWith(
            List.of(api(ApiFixtures.aProxyApiV4(), "proxy-api", "Proxy"), api(ApiFixtures.aMessageApiV4(), "message-api", "Message"))
        );

        var output = execute();

        assertThat(output.specs()).extracting(ApiCRDSpec::getId).containsExactly("message-api", "proxy-api");
    }

    @Test
    void should_leave_out_v2_apis() {
        apiQueryService.initWith(List.of(api(ApiFixtures.aProxyApiV4(), "v4-api", "V4"), api(ApiFixtures.aProxyApiV2(), "v2-api", "V2")));

        var output = execute();

        assertThat(output.specs()).extracting(ApiCRDSpec::getId).containsExactly("v4-api");
    }

    @Test
    void should_leave_out_edge_and_authz_apis() {
        apiQueryService.initWith(
            List.of(
                api(ApiFixtures.aProxyApiV4(), "proxy-api", "Proxy"),
                api(ApiFixtures.aProxyApiV4().toBuilder().type(ApiType.EDGE).build(), "edge-api", "Edge"),
                api(ApiFixtures.aProxyApiV4().toBuilder().type(ApiType.AUTHZ).build(), "authz-api", "Authz")
            )
        );

        var output = execute();

        assertThat(output.specs()).extracting(ApiCRDSpec::getId).containsExactly("proxy-api");
    }

    @Test
    void should_leave_out_apis_of_other_environments() {
        apiQueryService.initWith(
            List.of(
                api(ApiFixtures.aProxyApiV4(), "my-api", "Mine"),
                api(ApiFixtures.aProxyApiV4(), "other-api", "Other").toBuilder().environmentId("other-environment").build()
            )
        );

        var output = execute();

        assertThat(output.specs()).extracting(ApiCRDSpec::getId).containsExactly("my-api");
    }

    @Test
    void should_hand_the_export_strategy_audit_and_notification_flag_to_every_export() {
        apiQueryService.initWith(List.of(api(ApiFixtures.aProxyApiV4(), "proxy-api", "Proxy")));

        cut.execute(new ExportEnvironmentApiCRDsUseCase.Input(AUDIT_INFO, IDExportStrategy.HRID, false));

        assertThat(exports).containsExactly(new Export("proxy-api", IDExportStrategy.HRID, AUDIT_INFO, false));
    }

    @Test
    void should_return_an_empty_list_when_the_environment_has_no_api() {
        apiQueryService.initWith(List.of());

        var output = execute();

        assertThat(output.specs()).isEmpty();
        assertThat(exports).isEmpty();
    }

    @Test
    void should_order_apis_by_name_ignoring_case_then_by_id() {
        apiQueryService.initWith(
            List.of(
                api(ApiFixtures.aProxyApiV4(), "api-c", "zeta"),
                api(ApiFixtures.aProxyApiV4(), "api-b", "Alpha"),
                api(ApiFixtures.aProxyApiV4(), "api-a", "alpha"),
                api(ApiFixtures.aProxyApiV4(), "api-d", "Beta")
            )
        );

        var output = execute();

        assertThat(output.specs()).extracting(ApiCRDSpec::getId).containsExactly("api-a", "api-b", "api-d", "api-c");
    }
}
