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
package io.gravitee.apim.infra.domain_service.logs_engine;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.logs_engine.model.ApiKeyMode;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ApiLogDiagnostic;
import io.gravitee.apim.core.logs_engine.model.BaseApplication;
import io.gravitee.apim.core.logs_engine.model.BasePlan;
import io.gravitee.apim.core.logs_engine.model.HttpMethod;
import io.gravitee.apim.core.logs_engine.model.MembershipMemberType;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.PlanMode;
import io.gravitee.apim.core.logs_engine.model.PlanSecurity;
import io.gravitee.apim.core.logs_engine.model.PlanSecurityType;
import io.gravitee.apim.core.logs_engine.model.PrimaryOwner;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.user.model.UserContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogNamesPostProcessorImplTest {

    private static final Pagination PAGINATION = new Pagination(1, 10, 1, 1, 1L);
    private static final UserContext BASE_CONTEXT = new UserContext(AuditInfoFixtures.anAuditInfo("org", "env", "user"));

    private LogNamesPostProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new LogNamesPostProcessorImpl();
    }

    private static ApiLog aLogWith(String apiId, BasePlan plan, BaseApplication application) {
        return aLogWith(apiId, plan, application, null);
    }

    private static ApiLog aLogWith(String apiId, BasePlan plan, BaseApplication application, String gateway) {
        return new ApiLog(
            apiId,
            null,
            null,
            null,
            null,
            null,
            null,
            plan,
            application,
            null,
            null,
            null,
            null,
            gateway,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            Map.of()
        );
    }

    private static BasePlan aPlan(String planId) {
        return new BasePlan(planId, null, null, null, null, null);
    }

    private static BaseApplication anApplication(String appId) {
        return new BaseApplication(appId, null, null, null, null, null, null);
    }

    @Nested
    class WhenAllNamesArePresent {

        @Test
        void should_resolve_api_name() {
            var context = BASE_CONTEXT.withApiNamesById(Map.of("api-1", "My API"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().apiName()).isEqualTo("My API");
        }

        @Test
        void should_resolve_plan_name() {
            var context = BASE_CONTEXT.withPlanNameById(Map.of("plan-1", "My Plan"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", aPlan("plan-1"), null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().plan().name()).isEqualTo("My Plan");
        }

        @Test
        void should_resolve_application_name() {
            var context = BASE_CONTEXT.withApplicationNameById(Map.of("app-1", "My App"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, anApplication("app-1"))), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().application().name()).isEqualTo("My App");
        }

        @Test
        void should_resolve_all_names_at_once() {
            var context = BASE_CONTEXT.withApiNamesById(Map.of("api-1", "My API"))
                .withPlanNameById(Map.of("plan-1", "My Plan"))
                .withApplicationNameById(Map.of("app-1", "My App"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", aPlan("plan-1"), anApplication("app-1"))), PAGINATION);

            var result = processor.mapLogNames(context, response);

            var log = result.data().getFirst();
            assertThat(log.apiName()).isEqualTo("My API");
            assertThat(log.plan().name()).isEqualTo("My Plan");
            assertThat(log.application().name()).isEqualTo("My App");
        }
    }

    @Nested
    class WhenNamesAreMissingFromMaps {

        @Test
        void should_return_null_api_name_when_id_not_in_map() {
            var context = BASE_CONTEXT.withApiNamesById(Map.of("other-api", "Other API"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().apiName()).isNull();
        }

        @Test
        void should_return_null_plan_name_when_id_not_in_map() {
            var context = BASE_CONTEXT.withPlanNameById(Map.of("other-plan", "Other Plan"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", aPlan("plan-1"), null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().plan().name()).isNull();
        }

        @Test
        void should_return_unknown_application_name_when_id_not_in_map() {
            var context = BASE_CONTEXT.withApplicationNameById(Map.of("other-app", "Other App"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, anApplication("app-1"))), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().application().name()).isEqualTo("Unknown");
        }
    }

    @Nested
    class WhenPlanOrApplicationIsNull {

        @Test
        void should_keep_plan_null_without_npe() {
            var context = BASE_CONTEXT.withPlanNameById(Map.of("plan-1", "My Plan"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().plan()).isNull();
        }

        @Test
        void should_keep_application_null_without_npe() {
            var context = BASE_CONTEXT.withApplicationNameById(Map.of("app-1", "My App"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().application()).isNull();
        }
    }

    @Nested
    class WhenMapsAreEmptyOptionals {

        @Test
        void should_return_null_names_when_context_has_empty_optionals() {
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", aPlan("plan-1"), anApplication("app-1"))), PAGINATION);

            var result = processor.mapLogNames(BASE_CONTEXT, response);

            var log = result.data().getFirst();
            assertThat(log.apiName()).isNull();
            assertThat(log.plan().name()).isNull();
            assertThat(result.data().getFirst().application().name()).isEqualTo("Unknown");
        }
    }

    @Nested
    class WhenFieldPreservation {

        @Test
        void should_preserve_all_non_name_fields_through_enrichment() {
            var timestamp = OffsetDateTime.parse("2026-01-15T10:30:00Z");
            var security = new PlanSecurity(PlanSecurityType.API_KEY, "config");
            var primaryOwner = new PrimaryOwner("owner-1", "owner@test.com", "Owner", MembershipMemberType.USER);
            var warning = new ApiLogDiagnostic("comp-type", "comp-name", "warn-key", "warn-msg");

            var originalPlan = new BasePlan("plan-1", "old-plan-name", "plan-desc", "api-1", security, PlanMode.STANDARD);
            var originalApp = new BaseApplication(
                "app-1",
                "old-app-name",
                "app-desc",
                "domain.com",
                "web",
                primaryOwner,
                ApiKeyMode.SHARED
            );
            var originalLog = new ApiLog(
                "api-1",
                "old-api-name",
                timestamp,
                "log-id",
                "req-1",
                HttpMethod.GET,
                "client-1",
                originalPlan,
                originalApp,
                "tx-1",
                200,
                true,
                42,
                "gw-1",
                "/path",
                "http://backend",
                "msg",
                "err-key",
                "err-comp",
                "err-type",
                List.of(warning),
                Map.of("k", "v")
            );

            var context = BASE_CONTEXT.withApiNamesById(Map.of("api-1", "New API"))
                .withPlanNameById(Map.of("plan-1", "New Plan"))
                .withApplicationNameById(Map.of("app-1", "New App"))
                .withGatewayHostnameById(Map.of("gw-1", "resolved-hostname"));
            var response = new SearchLogsResponse(List.of(originalLog), PAGINATION);

            var result = processor.mapLogNames(context, response);
            var log = result.data().getFirst();

            // Names should be enriched
            assertThat(log.apiName()).isEqualTo("New API");
            assertThat(log.plan().name()).isEqualTo("New Plan");
            assertThat(log.application().name()).isEqualTo("New App");

            // All other ApiLog fields must survive unchanged
            assertThat(log.apiId()).isEqualTo("api-1");
            assertThat(log.timestamp()).isEqualTo(timestamp);
            assertThat(log.id()).isEqualTo("log-id");
            assertThat(log.requestId()).isEqualTo("req-1");
            assertThat(log.method()).isEqualTo(HttpMethod.GET);
            assertThat(log.clientIdentifier()).isEqualTo("client-1");
            assertThat(log.transactionId()).isEqualTo("tx-1");
            assertThat(log.status()).isEqualTo(200);
            assertThat(log.requestEnded()).isTrue();
            assertThat(log.gatewayResponseTime()).isEqualTo(42);
            assertThat(log.gateway()).isEqualTo("resolved-hostname");
            assertThat(log.uri()).isEqualTo("/path");
            assertThat(log.endpoint()).isEqualTo("http://backend");
            assertThat(log.message()).isEqualTo("msg");
            assertThat(log.errorKey()).isEqualTo("err-key");
            assertThat(log.errorComponentName()).isEqualTo("err-comp");
            assertThat(log.errorComponentType()).isEqualTo("err-type");
            assertThat(log.warnings()).containsExactly(warning);
            assertThat(log.additionalMetrics()).containsEntry("k", "v");

            // Plan non-name fields must survive unchanged
            assertThat(log.plan().id()).isEqualTo("plan-1");
            assertThat(log.plan().description()).isEqualTo("plan-desc");
            assertThat(log.plan().apiId()).isEqualTo("api-1");
            assertThat(log.plan().security()).isEqualTo(security);
            assertThat(log.plan().mode()).isEqualTo(PlanMode.STANDARD);

            // Application non-name fields must survive unchanged
            assertThat(log.application().id()).isEqualTo("app-1");
            assertThat(log.application().description()).isEqualTo("app-desc");
            assertThat(log.application().domain()).isEqualTo("domain.com");
            assertThat(log.application().type()).isEqualTo("web");
            assertThat(log.application().primaryOwner()).isEqualTo(primaryOwner);
            assertThat(log.application().apiKeyMode()).isEqualTo(ApiKeyMode.SHARED);
        }
    }

    @Nested
    class WhenMultipleLogs {

        @Test
        void should_enrich_multiple_logs_independently() {
            var context = BASE_CONTEXT.withApiNamesById(Map.of("api-1", "API One", "api-2", "API Two"))
                .withPlanNameById(Map.of("plan-1", "Plan One", "plan-2", "Plan Two"))
                .withApplicationNameById(Map.of("app-1", "App One", "app-2", "App Two"));
            var log1 = aLogWith("api-1", aPlan("plan-1"), anApplication("app-1"));
            var log2 = aLogWith("api-2", aPlan("plan-2"), anApplication("app-2"));
            var response = new SearchLogsResponse(List.of(log1, log2), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data()).hasSize(2);
            assertThat(result.data().get(0).apiName()).isEqualTo("API One");
            assertThat(result.data().get(0).plan().name()).isEqualTo("Plan One");
            assertThat(result.data().get(0).application().name()).isEqualTo("App One");
            assertThat(result.data().get(1).apiName()).isEqualTo("API Two");
            assertThat(result.data().get(1).plan().name()).isEqualTo("Plan Two");
            assertThat(result.data().get(1).application().name()).isEqualTo("App Two");
        }
    }

    @Nested
    class WhenGatewayHostnameResolution {

        @Test
        void should_resolve_gateway_hostname() {
            var context = BASE_CONTEXT.withGatewayHostnameById(Map.of("gw-1", "gateway-host-1"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null, "gw-1")), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().gateway()).isEqualTo("gateway-host-1");
        }

        @Test
        void should_fall_back_to_raw_id_when_hostname_not_in_map() {
            var context = BASE_CONTEXT.withGatewayHostnameById(Map.of("other-gw", "other-host"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null, "gw-1")), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().gateway()).isEqualTo("gw-1");
        }

        @Test
        void should_fall_back_to_raw_id_when_map_is_empty_optional() {
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null, "gw-1")), PAGINATION);

            var result = processor.mapLogNames(BASE_CONTEXT, response);

            assertThat(result.data().getFirst().gateway()).isEqualTo("gw-1");
        }

        @Test
        void should_keep_gateway_null_when_log_has_no_gateway() {
            var context = BASE_CONTEXT.withGatewayHostnameById(Map.of("gw-1", "gateway-host-1"));
            var response = new SearchLogsResponse(List.of(aLogWith("api-1", null, null, null)), PAGINATION);

            var result = processor.mapLogNames(context, response);

            assertThat(result.data().getFirst().gateway()).isNull();
        }
    }

    @Test
    void should_preserve_pagination() {
        var pagination = new Pagination(3, 5, 10, 5, 50L);
        var response = new SearchLogsResponse(List.of(), pagination);

        var result = processor.mapLogNames(BASE_CONTEXT, response);

        assertThat(result.data()).isEmpty();
        assertThat(result.pagination()).isEqualTo(pagination);
    }
}
