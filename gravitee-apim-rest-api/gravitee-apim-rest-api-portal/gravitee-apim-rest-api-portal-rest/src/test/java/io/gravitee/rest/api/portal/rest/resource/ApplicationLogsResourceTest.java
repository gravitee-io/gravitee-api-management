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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import fixtures.repository.ConnectionLogDetailFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.model.LogsResponse;
import io.gravitee.rest.api.portal.rest.resource.param.Range;
import io.gravitee.rest.api.portal.rest.resource.param.ResponseTimeRange;
import io.gravitee.rest.api.portal.rest.resource.param.SearchApplicationLogsParam;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationLogsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @Autowired
    ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    PlanCrudServiceInMemory planCrudServiceInMemory;

    @Autowired
    ConnectionLogsCrudServiceInMemory connectionLogsCrudServiceInMemory;

    private static final String APPLICATION_ID = "my-application";
    private static final String LOG = "my-log";
    private static final ApplicationEntity APPLICATION = ApplicationEntity.builder().id(APPLICATION_ID).build();

    private static final String API_1_ID = "api-1";
    private static final String API_2_ID = "api-2";
    private static final Api API_1 = Api.builder().id(API_1_ID).name("API 1").version("1.1").build();
    private static final Api API_2 = Api.builder().id(API_2_ID).name("API 2").version("2.2").build();

    private static final String PLAN_1_ID = "plan-1";
    private static final String PLAN_2_ID = "plan-2";
    private static final Plan PLAN_1 = Plan.builder().id(PLAN_1_ID).definitionVersion(DefinitionVersion.V4).name("Plan 1").build();
    private static final Plan PLAN_2 = Plan.builder().id(PLAN_2_ID).definitionVersion(DefinitionVersion.V4).name("Plan 2").build();

    private static final Long FIRST_FEBRUARY_2020 = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long SECOND_FEBRUARY_2020 = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API_1_ID, APPLICATION_ID, PLAN_1_ID);
    ConnectionLogDetailFixtures connectionLogDetailFixtures = new ConnectionLogDetailFixtures(API_1_ID, LOG);

    private Map<String, Map<String, String>> metadata;
    private SearchLogResponse<ApplicationRequestItem> searchResponse;

    @Before
    public void init() {
        resetAllMocks();

        ApplicationRequestItem appLogItem1 = new ApplicationRequestItem();
        appLogItem1.setId("A");
        ApplicationRequestItem appLogItem2 = new ApplicationRequestItem();
        appLogItem2.setId("B");
        searchResponse = new SearchLogResponse<>(2);
        searchResponse.setLogs(Arrays.asList(appLogItem1, appLogItem2));

        metadata = new HashMap<String, Map<String, String>>();
        HashMap<String, String> appMetadata = new HashMap<String, String>();
        appMetadata.put(APPLICATION_ID, APPLICATION_ID);
        metadata.put(APPLICATION_ID, appMetadata);
        searchResponse.setMetadata(metadata);

        doReturn(searchResponse).when(logsService).findByApplication(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        doReturn(new Log()).when(logMapper).convert(any(ApplicationRequestItem.class));
        doReturn(new Log()).when(logMapper).convert(any(ApplicationRequest.class));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        when(logMapper.convert(eq(APPLICATION_ID), any(SearchApplicationLogsParam.class))).thenCallRealMethod();
        when(logMapper.convert(any(List.class))).thenCallRealMethod();
        applicationCrudServiceInMemory.initWith(List.of(APPLICATION));
        apiCrudServiceInMemory.initWith(List.of(API_1, API_2));
        planCrudServiceInMemory.initWith(List.of(PLAN_1, PLAN_2));
    }

    @After
    public void cleanUp() {
        Stream
            .of(applicationCrudServiceInMemory, apiCrudServiceInMemory, planCrudServiceInMemory, connectionLogsCrudServiceInMemory)
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    public void shouldGetLogs() {
        final Response response = target(APPLICATION_ID)
            .path("logs")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("query", APPLICATION_ID)
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("field", APPLICATION_ID)
            .queryParam("order", "ASC")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<LogQuery> logQueryCaptor = ArgumentCaptor.forClass(LogQuery.class);
        Mockito
            .verify(logsService)
            .findByApplication(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), logQueryCaptor.capture());
        final LogQuery logQuery = logQueryCaptor.getValue();
        assertEquals(APPLICATION_ID, logQuery.getField());
        assertEquals(0, logQuery.getFrom());
        assertEquals(1, logQuery.getPage());
        assertEquals(APPLICATION_ID, logQuery.getQuery());
        assertEquals(10, logQuery.getSize());
        assertEquals(100, logQuery.getTo());
        assertTrue(logQuery.isOrder());

        LogsResponse logsResponse = response.readEntity(LogsResponse.class);
        assertEquals(2, logsResponse.getData().size());

        Map<String, Map<String, Object>> logsMetadata = logsResponse.getMetadata();
        assertEquals(2, logsMetadata.size());
        assertEquals(APPLICATION_ID, logsMetadata.get(APPLICATION_ID).get(APPLICATION_ID));
        assertEquals(2, logsMetadata.get(AbstractResource.METADATA_DATA_KEY).get(AbstractResource.METADATA_DATA_TOTAL_KEY));

        Links links = logsResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetNoLogAndNoLink() {
        SearchLogResponse<ApplicationRequestItem> emptySearchResponse = new SearchLogResponse<>(0);
        emptySearchResponse.setLogs(Collections.emptyList());
        doReturn(emptySearchResponse)
            .when(logsService)
            .findByApplication(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        final Response response = target(APPLICATION_ID)
            .path("logs")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("query", APPLICATION_ID)
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("field", APPLICATION_ID)
            .queryParam("order", "ASC")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        LogsResponse logsResponse = response.readEntity(LogsResponse.class);
        assertEquals(0, logsResponse.getData().size());

        Links links = logsResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldGetLog() {
        final ApplicationRequest toBeReturned = new ApplicationRequest();
        toBeReturned.setId(LOG);
        doReturn(toBeReturned)
            .when(logsService)
            .findApplicationLog(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), eq(LOG), any());

        final Response response = target(APPLICATION_ID).path("logs").path(LOG).queryParam("timestamp", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(logMapper).convert(toBeReturned);
        Mockito.verify(logsService).findApplicationLog(GraviteeContext.getExecutionContext(), APPLICATION_ID, LOG, 1L);

        Log Log = response.readEntity(Log.class);
        assertNotNull(Log);
    }

    @Test
    public void shouldExportLogs() {
        doReturn("EXPORT").when(logsService).exportAsCsv(eq(GraviteeContext.getExecutionContext()), any());
        final Response response = target(APPLICATION_ID)
            .path("logs")
            .path("_export")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("query", APPLICATION_ID)
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("field", APPLICATION_ID)
            .queryParam("order", "DESC")
            .request()
            .post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(logsService).exportAsCsv(GraviteeContext.getExecutionContext(), searchResponse);

        String exportString = response.readEntity(String.class);
        assertEquals("EXPORT", exportString);
        final MultivaluedMap<String, Object> headers = response.getHeaders();
        assertTrue(((String) headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment;filename=logs-" + APPLICATION_ID));
    }

    /**
     * Search Application Logs
     */

    @Test
    public void should_not_allow_invalid_to_and_from_search() {
        var body = SearchApplicationLogsParam.builder().to(0).from(100).build();
        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        var errorResponse = response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse.getErrors());
        assertEquals(1, errorResponse.getErrors().size());
        assertEquals("'from' query parameter value must not be greater than 'to'", errorResponse.getErrors().getFirst().getMessage());
    }

    @Test
    public void should_not_allow_status_less_than_100_in_search() {
        var body = SearchApplicationLogsParam.builder().to(100).from(0).statuses(Set.of(100, 1)).build();
        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        var errorResponse = response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse.getErrors());
        assertEquals(1, errorResponse.getErrors().size());
        assertNotNull(errorResponse.getErrors().getFirst().getMessage());
        assertTrue(
            errorResponse.getErrors().getFirst().getMessage().contains("'statuses' must contain values greater than or equal to 100")
        );
    }

    @Test
    public void should_not_allow_status_greater_than_599_in_search() {
        var body = SearchApplicationLogsParam.builder().to(100).from(0).statuses(Set.of(100, 900)).build();
        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        var errorResponse = response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse.getErrors());
        assertEquals(1, errorResponse.getErrors().size());
        assertNotNull(errorResponse.getErrors().getFirst().getMessage());
        assertTrue(
            errorResponse.getErrors().getFirst().getMessage().contains("'statuses' must contain values lesser than or equal to 599")
        );
    }

    @Test
    public void should_return_empty_list_of_logs_in_search() {
        var body = SearchApplicationLogsParam.builder().to(100).from(0).build();
        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(0, logsResponse.getData().size());
        assertEquals(Map.of("data", Map.of("total", 0)), logsResponse.getMetadata());
    }

    @Test
    public void should_return_list_of_logs_in_search() {
        // Given
        connectionLogsCrudServiceInMemory.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1"),
                connectionLogFixtures.aConnectionLog().toBuilder().applicationId("other-application").build()
            )
        );

        // When
        var body = SearchApplicationLogsParam.builder().to(SECOND_FEBRUARY_2020).from(FIRST_FEBRUARY_2020).build();

        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(1, logsResponse.getData().size());
        assertEquals(
            Map.of(
                "data",
                Map.of("total", 1),
                API_1_ID,
                Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                PLAN_1_ID,
                Map.of("name", PLAN_1.getName())
            ),
            logsResponse.getMetadata()
        );
    }

    @Test
    public void should_return_list_of_logs_in_search_with_unknown_api_and_unknown_plan() {
        // Given
        connectionLogsCrudServiceInMemory.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().apiId("unknown-api").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().planId("unknown-plan").build()
            )
        );

        // When
        var body = SearchApplicationLogsParam.builder().to(SECOND_FEBRUARY_2020).from(FIRST_FEBRUARY_2020).build();

        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(2, logsResponse.getData().size());
        assertEquals(
            Map.of(
                "data",
                Map.of("total", 2),
                API_1_ID,
                Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                "unknown-api",
                Map.of("name", "Unknown API (not found)", "unknown", "true"),
                PLAN_1_ID,
                Map.of("name", PLAN_1.getName()),
                "unknown-plan",
                Map.of("name", "Unknown plan", "unknown", "true")
            ),
            logsResponse.getMetadata()
        );
    }

    @Test
    public void should_return_filtered_list_of_logs_in_search() {
        // Given
        connectionLogsCrudServiceInMemory.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1"),
                connectionLogFixtures.aConnectionLog().toBuilder().apiId(API_2_ID).build(),
                connectionLogFixtures.aConnectionLog().toBuilder().apiId("other-api").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().planId(PLAN_2_ID).build(),
                connectionLogFixtures.aConnectionLog().toBuilder().planId("other-plan").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().method(HttpMethod.DELETE).build(),
                connectionLogFixtures.aConnectionLog().toBuilder().status(400).build()
            )
        );

        // When
        var body = SearchApplicationLogsParam
            .builder()
            .to(SECOND_FEBRUARY_2020)
            .from(FIRST_FEBRUARY_2020)
            .apiIds(Set.of(API_1_ID, API_2_ID))
            .planIds(Set.of(PLAN_1_ID, PLAN_2_ID))
            .methods(
                Set.of(io.gravitee.rest.api.portal.rest.model.HttpMethod.GET, io.gravitee.rest.api.portal.rest.model.HttpMethod.CONNECT)
            )
            .statuses(Set.of(200, 201))
            .build();

        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(4, logsResponse.getData().size());
        assertEquals(
            Map.of(
                "data",
                Map.of("total", 4),
                API_1_ID,
                Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                PLAN_1_ID,
                Map.of("name", PLAN_1.getName()),
                API_2_ID,
                Map.of("name", API_2.getName(), "version", API_2.getVersion()),
                PLAN_2_ID,
                Map.of("name", PLAN_2.getName())
            ),
            logsResponse.getMetadata()
        );
    }

    @Test
    public void should_get_response_times_of_logs_in_search() {
        // Given
        connectionLogsCrudServiceInMemory.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().gatewayResponseTime(10L).build(),
                connectionLogFixtures.aConnectionLog().toBuilder().gatewayResponseTime(200L).build(),
                connectionLogFixtures.aConnectionLog().toBuilder().gatewayResponseTime(500L).build()
            )
        );

        // When
        var body = SearchApplicationLogsParam
            .builder()
            .to(SECOND_FEBRUARY_2020)
            .from(FIRST_FEBRUARY_2020)
            .responseTimeRanges(
                List.of(ResponseTimeRange.builder().to(100L).build(), ResponseTimeRange.builder().from(400L).to(600L).build())
            )
            .build();

        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(2, logsResponse.getData().size());
        assertEquals(
            Map.of(
                "data",
                Map.of("total", 2),
                API_1_ID,
                Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                PLAN_1_ID,
                Map.of("name", PLAN_1.getName())
            ),
            logsResponse.getMetadata()
        );
    }

    @Test
    public void should_return_filtered_list_of_logs_with_body_text_in_search() {
        // Given
        connectionLogsCrudServiceInMemory.initWithConnectionLogDetails(
            List.of(
                connectionLogDetailFixtures
                    .aConnectionLogDetail("req1")
                    .toBuilder()
                    .entrypointResponse(ConnectionLogDetail.Response.builder().body("my-curl").build())
                    .build(),
                connectionLogDetailFixtures
                    .aConnectionLogDetail("req2")
                    .toBuilder()
                    .apiId(API_2_ID)
                    .entrypointResponse(ConnectionLogDetail.Response.builder().body("my-curl").build())
                    .build(),
                connectionLogDetailFixtures
                    .aConnectionLogDetail("req3")
                    .toBuilder()
                    .entrypointResponse(ConnectionLogDetail.Response.builder().body("not-found").build())
                    .build()
            )
        );

        connectionLogsCrudServiceInMemory.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1"),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().apiId(API_2_ID).build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().method(HttpMethod.DELETE).build()
            )
        );

        // When
        var body = SearchApplicationLogsParam
            .builder()
            .to(SECOND_FEBRUARY_2020)
            .from(FIRST_FEBRUARY_2020)
            .apiIds(Set.of(API_1_ID, API_2_ID))
            .methods(Set.of(io.gravitee.rest.api.portal.rest.model.HttpMethod.DELETE))
            .bodyText("curl")
            .build();

        final Response response = target(APPLICATION_ID)
            .path("logs/_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(body));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var logsResponse = response.readEntity(LogsResponse.class);
        assertNotNull(logsResponse.getData());
        assertEquals(2, logsResponse.getData().size());
        assertEquals(
            Map.of(
                "data",
                Map.of("total", 2),
                API_1_ID,
                Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                PLAN_1_ID,
                Map.of("name", PLAN_1.getName()),
                API_2_ID,
                Map.of("name", API_2.getName(), "version", API_2.getVersion())
            ),
            logsResponse.getMetadata()
        );
    }
}
