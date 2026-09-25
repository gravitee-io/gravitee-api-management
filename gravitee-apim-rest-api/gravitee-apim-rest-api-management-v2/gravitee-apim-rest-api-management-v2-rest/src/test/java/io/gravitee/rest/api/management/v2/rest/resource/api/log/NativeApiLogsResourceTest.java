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
package io.gravitee.rest.api.management.v2.rest.resource.api.log;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.NativeApiLogFixtures.*;
import static io.gravitee.common.http.HttpStatusCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.NativeApiLogCrudService;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLog;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLogsSummary;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiLogsResourceTest extends ApiResourceTest {

    @Inject
    AnalyticsRepository analyticsRepository;

    @Inject
    NativeApiLogCrudService nativeApiLogCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/logs/native";
    }

    @BeforeEach
    public void initNativeLogs() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        reset(analyticsRepository);
        reset(nativeApiLogCrudService);
        reset(permissionService);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Test
    void summarize_returns_count_by_connection_status_when_permitted() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        when(analyticsRepository.searchNativeApiFacets(any(), any())).thenReturn(facetsResult(CONNECTION_STATUS_COUNTS));

        Response response = summarize(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLogsSummary.class);
        assertThat(body.getCountByConnectionStatus()).containsExactlyInAnyOrderEntriesOf(CONNECTION_STATUS_COUNTS);
    }

    @Test
    void summarize_returns_empty_when_no_data() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        when(analyticsRepository.searchNativeApiFacets(any(), any())).thenReturn(new FacetsResult(List.of()));

        Response response = summarize(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLogsSummary.class);
        assertThat(body.getCountByConnectionStatus()).isEmpty();
    }

    @Test
    void summarize_returns_400_when_from_or_to_missing() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);

        Response response = rootTarget().path("summary").request().get();

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void summarize_returns_403_when_permission_denied() {
        givenPermission(RolePermission.API_NATIVE_LOG, false);

        Response response = summarize(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void summarize_returns_403_when_only_native_analytics_granted() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, true);

        Response response = summarize(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void getLog_returns_unique_log_when_permitted() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, true);
        when(
            nativeApiLogCrudService.findLog(any(ExecutionContext.class), eq(API), eq(REQUEST_ID), eq(FROM_MILLIS), eq(TO_MILLIS))
        ).thenReturn(optionalLog());

        Response response = getLog(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLog.class);
        assertThat(body.getApiId()).isEqualTo(API);
        assertThat(body.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(body.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(body.getTimestamp()).isEqualTo(TIMESTAMP_UTC);
        assertThat(body.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(body.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(body.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(body.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
        assertThat(body.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
        assertThat(body.getGateway()).isEqualTo(GATEWAY);
        assertThat(body.getRemoteAddress()).isEqualTo(REMOTE_ADDRESS);
        assertThat(body.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
        assertThat(body.getHost()).isEqualTo(HOST);
        assertThat(body.getErrorKey()).isEqualTo(ERROR_KEY);
        assertThat(body.getErrorMessage()).isEqualTo(MESSAGE);
        assertThat(body.getConnectionStatus()).isEqualTo(NativeApiLog.ConnectionStatusEnum.CONNECTION_ERROR);
        assertThat(body.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(body.getBrokerId()).isEqualTo(BROKER_ID);
        assertThat(body.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);
    }

    @Test
    void getLog_returns_400_when_from_or_to_missing() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, true);

        Response response = rootTarget().path(REQUEST_ID).request().get();

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void getLog_returns_404_when_log_not_found() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, true);
        when(
            nativeApiLogCrudService.findLog(any(ExecutionContext.class), eq(API), eq(REQUEST_ID), eq(FROM_MILLIS), eq(TO_MILLIS))
        ).thenReturn(Optional.empty());

        Response response = getLog(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void getLog_returns_403_when_permission_denied() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, false);

        Response response = getLog(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void getLog_returns_403_when_only_native_log_granted() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);

        Response response = getLog(FROM_MILLIS, TO_MILLIS);

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void getFilteredLogs_never_links_to_a_page_the_store_would_refuse() {
        // track_total_hits makes the count honest, and on a busy API the honest count runs past the window
        // Elasticsearch will page to. A `last` link computed from the count alone points at a page that comes
        // back as a 400, so the links are bounded by what is reachable while the count stays as it is.
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        when(nativeApiLogCrudService.searchLogs(any(ExecutionContext.class), eq(API), any(), eq(1), eq(10))).thenReturn(
            new SearchLogsResponse<>(50_000L, filteredLogs(), 10_000L)
        );

        Response response = rootTarget()
            .queryParam("from", FROM_MILLIS)
            .queryParam("to", TO_MILLIS)
            .queryParam("perPage", 10)
            .request()
            .get();

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLogsResponse.class);
        assertThat(body.getPagination().getTotalCount()).as("the count stays truthful").isEqualTo(50_000L);
        assertThat(body.getLinks().getLast()).as("the last link stops at the last servable page").contains("page=1000");
    }

    @Test
    void getFilteredLogs_rounds_the_bound_down_to_a_whole_page() {
        // A page size that does not divide the window leaves a partial last page: it starts inside the window
        // and ends past it, which the store refuses. At perPage=30 the bound is 9 990, not 10 000 — otherwise
        // `last` is page 334, whose from=9990 plus 30 reaches past the window and answers 400.
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        when(nativeApiLogCrudService.searchLogs(any(ExecutionContext.class), eq(API), any(), eq(1), eq(30))).thenReturn(
            new SearchLogsResponse<>(50_000L, filteredLogs(), 10_000L)
        );

        Response response = rootTarget()
            .queryParam("from", FROM_MILLIS)
            .queryParam("to", TO_MILLIS)
            .queryParam("perPage", 30)
            .request()
            .get();

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLogsResponse.class);
        assertThat(body.getLinks().getLast()).as("the last page must fit entirely inside the window").contains("page=333");
        assertThat(body.getReachableCount()).isEqualTo(9_990L);
    }

    @Test
    void getFilteredLogs_links_to_every_page_when_the_store_sets_no_window() {
        // The no-op repository, or any store that pages without a window, reports no bound at all.
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        when(nativeApiLogCrudService.searchLogs(any(ExecutionContext.class), eq(API), any(), eq(1), eq(10))).thenReturn(
            new SearchLogsResponse<>(50_000L, filteredLogs())
        );

        Response response = rootTarget()
            .queryParam("from", FROM_MILLIS)
            .queryParam("to", TO_MILLIS)
            .queryParam("perPage", 10)
            .request()
            .get();

        var body = response.readEntity(NativeApiLogsResponse.class);
        assertThat(body.getLinks().getLast()).contains("page=5000");
    }

    @Test
    void getFilteredLogs_returns_filtered_logs_when_permitted() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);
        var logs = filteredLogs();
        var filterCaptor = ArgumentCaptor.forClass(NativeApiLogCrudService.Filter.class);
        when(nativeApiLogCrudService.searchLogs(any(ExecutionContext.class), eq(API), filterCaptor.capture(), eq(1), eq(10))).thenReturn(
            new SearchLogsResponse<>(2L, logs)
        );

        Response response = rootTarget()
            .queryParam("from", FROM_MILLIS)
            .queryParam("to", TO_MILLIS)
            .queryParam("perPage", 10)
            .queryParam("applicationIds", APPLICATION_ID + ",app-2")
            .queryParam("planIds", PLAN_ID)
            .queryParam("connectionStatuses", STATUS_CONNECTED + "," + STATUS_CONNECTION_ERROR)
            .request()
            .get();

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(NativeApiLogsResponse.class);
        assertThat(body.getData()).hasSize(2).extracting(NativeApiLog::getRequestId).containsExactly(REQUEST_ID, "r2");
        assertThat(body.getPagination().getTotalCount()).isEqualTo(2L);

        var first = body.getData().get(0);
        assertThat(first.getApiId()).isEqualTo(API);
        assertThat(first.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(first.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(first.getTimestamp()).isEqualTo(TIMESTAMP_UTC);
        assertThat(first.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(first.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(first.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
        assertThat(first.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
        assertThat(first.getConnectionStatus()).isEqualTo(NativeApiLog.ConnectionStatusEnum.CONNECTION_ERROR);
        assertThat(first.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);

        // Hidden from API_NATIVE_LOG: REST endpoint must not leak them.
        assertThat(first.getSubscriptionId()).isNull();
        assertThat(first.getGateway()).isNull();
        assertThat(first.getRemoteAddress()).isNull();
        assertThat(first.getLocalAddress()).isNull();
        assertThat(first.getHost()).isNull();
        assertThat(first.getErrorKey()).isNull();
        assertThat(first.getErrorMessage()).isNull();
        assertThat(first.getClientId()).isNull();
        assertThat(first.getBrokerId()).isNull();

        var capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter.from()).isEqualTo(FROM_MILLIS);
        assertThat(capturedFilter.to()).isEqualTo(TO_MILLIS);
        assertThat(capturedFilter.applicationIds()).containsExactlyInAnyOrder(APPLICATION_ID, "app-2");
        assertThat(capturedFilter.planIds()).containsExactly(PLAN_ID);
        assertThat(capturedFilter.connectionStatuses()).containsExactlyInAnyOrder(
            NativeConnectionStatus.CONNECTED,
            NativeConnectionStatus.CONNECTION_ERROR
        );
    }

    @Test
    void getFilteredLogs_returns_400_when_from_or_to_missing() {
        givenPermission(RolePermission.API_NATIVE_LOG, true);

        Response response = rootTarget().request().get();

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void getFilteredLogs_returns_403_when_permission_denied() {
        givenPermission(RolePermission.API_NATIVE_LOG, false);

        Response response = rootTarget().queryParam("from", FROM_MILLIS).queryParam("to", TO_MILLIS).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void getFilteredLogs_returns_403_when_only_native_analytics_granted() {
        givenPermission(RolePermission.API_NATIVE_ANALYTICS, true);

        Response response = rootTarget().queryParam("from", FROM_MILLIS).queryParam("to", TO_MILLIS).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    private Response summarize(long from, long to) {
        return rootTarget().path("summary").queryParam("from", from).queryParam("to", to).request().get();
    }

    private Response getLog(long from, long to) {
        return rootTarget().path(REQUEST_ID).queryParam("from", from).queryParam("to", to).request().get();
    }

    private void givenPermission(RolePermission permission, boolean granted) {
        when(permissionService.hasPermission(GraviteeContext.getExecutionContext(), permission, API, RolePermissionAction.READ)).thenReturn(
            granted
        );
    }

    private Optional<io.gravitee.apim.core.log.model.NativeApiLog> optionalLog() {
        return Optional.of(buildNativeApiErrorLog(API, REQUEST_ID));
    }

    private List<io.gravitee.apim.core.log.model.NativeApiLog> filteredLogs() {
        var log2 = io.gravitee.apim.core.log.model.NativeApiLog.builder().apiId(API).requestId("r2").timestamp(TIMESTAMP_ISO).build();
        return List.of(buildNativeApiLog(API, REQUEST_ID), log2);
    }

    private static FacetsResult facetsResult(Map<String, Long> countsByStatus) {
        var buckets = countsByStatus
            .entrySet()
            .stream()
            .map(entry -> new FacetBucketResult(entry.getKey(), List.of(), Map.of(Measure.COUNT, entry.getValue())))
            .toList();
        return new FacetsResult(List.of(new MetricFacetsResult(Metric.NATIVE_CONNECTIONS_SUMMARY, buckets)));
    }
}
