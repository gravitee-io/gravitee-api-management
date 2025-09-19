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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.PlanSecurityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.log.model.Request;
import io.gravitee.repository.log.model.Response;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.*;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.LogNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogsServiceTest {

    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();
    private static final String LOG_ID = "2560f540-c0f1-4c12-a76e-0da89ace6911";
    private static final String LOG_API_ID = "e7fa7f51-1540-490a-8134-31e965efdc09";
    private static final String LOG_API_KEY = "e6ca30d1-8ba4-4170-8105-d9338a33bcad";
    private static final String LOG_SUBSCRIPTION_ID = "e6ca30d1-8ba4-4170-8105-d9338a33bcad";
    private static final String LOG_APPLICATION_ID = "ca7ea3dc-1434-4ee5-a25f-cc57327d28cf";
    private static final String LOG_APPLICATION_NAME = "default";
    private static final String LOG_PLAN_ID = "81d3dc39-0e5f-4c1c-94cc-ec48c3609b5f";
    private static final String LOG_URI = "/echo";
    private static final Long LOG_TIMESTAMP = Instant.now().toEpochMilli();
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Mock
    private LogRepository logRepository;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    ApplicationService applicationService;

    @Mock
    ApiKeyService apiKeyService;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    ParameterService parameterService;

    @Mock
    ApiSearchService apiSearchService;

    @InjectMocks
    private final LogsService logService = new LogsServiceImpl();

    @Test
    void findApiLogShouldThrowNotFoundExceptionBecause() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(KEY_LESS));
        when(apiSearchService.findRepositoryApiById(EXECUTION_CONTEXT, LOG_API_ID)).thenThrow(new ApiNotFoundException(LOG_API_ID));
        assertThrows(LogNotFoundException.class, () -> logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP));
    }

    @Test
    void findApiLogShouldThrowException() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenThrow(new AnalyticsException());

        assertThrows(TechnicalManagementException.class, () -> logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP));
    }

    @Test
    void findApiLogShouldReturnNull() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(null);

        ApiRequest result = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);
        assertThat(result).isNull();
        verify(logRepository, times(1)).findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP));
    }

    @Test
    void findApiLogShouldFindWithKeyLessPlan() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(KEY_LESS));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(KEY_LESS));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(KEY_LESS.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
    }

    @Test
    void findApiLogShouldFindWithApiKeyPlan() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(API_KEY));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(API_KEY));
        when(apiKeyService.findByKeyAndApi(EXECUTION_CONTEXT, LOG_API_KEY, LOG_API_ID)).thenReturn(newApiKey());

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(API_KEY.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isEqualTo(LOG_SUBSCRIPTION_ID);
    }

    @Test
    void findApiLogShouldFindWithJwtPlan() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(JWT));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(JWT));
        when(subscriptionService.findByApplicationAndPlan(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_PLAN_ID)).thenReturn(
            Set.of(newSubscription())
        );

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(JWT.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isEqualTo(LOG_SUBSCRIPTION_ID);
    }

    @Test
    void findApiLogShouldHaveEmptySubscriptionWithNoSecurity() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(null));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(null));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isNull();
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isNull();
    }

    @Test
    void findApiLogShouldHaveEmptySubscriptionWithNoSecurityInV4Plan() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(null));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlanV4(null));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isNull();
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isNull();
    }

    @Test
    void findApiLogShouldNotFailOnApiKeyNotFoundException() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(API_KEY));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(API_KEY));
        when(apiKeyService.findByKeyAndApi(EXECUTION_CONTEXT, LOG_API_KEY, LOG_API_ID)).thenThrow(new ApiKeyNotFoundException());

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(API_KEY.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isNull();
    }

    @Test
    void findApiLogShouldNotFailOnPlanNotFoundException() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(JWT));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenThrow(new PlanNotFoundException(LOG_PLAN_ID));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(JWT.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
    }

    @Test
    void findApiLogShouldNotFailOnDuplicatedSubscription() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), eq(LOG_TIMESTAMP))).thenReturn(newLog(JWT));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(JWT));
        when(subscriptionService.findByApplicationAndPlan(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_PLAN_ID)).thenReturn(
            Set.of(newSubscription(), new SubscriptionEntity())
        );

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(JWT.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isNull();
    }

    @Test
    void findLogByApiShouldThrowException() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());

        LogQuery query = new LogQuery();

        assertThrows(TechnicalManagementException.class, () -> logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, query));
    }

    @Test
    void findLogByApiShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenReturn(null);

        SearchLogResponse<ApiRequestItem> result = logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isNull();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findLogByApiShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any(QueryContext.class), any())).thenReturn(tabularResponse);

        SearchLogResponse<ApiRequestItem> result = logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isEmpty();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findLogByApplicationShouldThrowException() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());
        LogQuery query = new LogQuery();

        assertThrows(TechnicalManagementException.class, () -> logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, query));
    }

    @Test
    void findLogByApplicationShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenReturn(null);

        SearchLogResponse<ApplicationRequestItem> result = logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isNull();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findLogByApplicationShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any(QueryContext.class), any())).thenReturn(tabularResponse);

        SearchLogResponse<ApplicationRequestItem> result = logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isEmpty();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findLogByPlatformShouldThrowException() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());

        LogQuery query = new LogQuery();

        assertThrows(TechnicalManagementException.class, () -> logService.findPlatform(EXECUTION_CONTEXT, query));
    }

    @Test
    void findLogByPlatformShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any(QueryContext.class), any())).thenReturn(null);

        SearchLogResponse<PlatformRequestItem> result = logService.findPlatform(EXECUTION_CONTEXT, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isNull();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findLogByPlatformShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any(QueryContext.class), any())).thenReturn(tabularResponse);

        SearchLogResponse<PlatformRequestItem> result = logService.findPlatform(EXECUTION_CONTEXT, new LogQuery());

        assertThat(result).isNotNull();
        assertThat(result.getLogs()).isEmpty();
        verify(logRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    void findApplicationLogShouldThrowNotFoundExceptionBecauseLogDoesNotBelongToApplication() throws Exception {
        ExtendedLog log = newLog(JWT);
        log.setClientRequest(newRequest());
        log.setClientResponse(newResponse());
        log.setApplication("Another_application");
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), anyLong())).thenReturn(log);

        long timestamp = Instant.now().toEpochMilli();

        assertThrows(LogNotFoundException.class, () ->
            logService.findApplicationLog(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_ID, timestamp)
        );
    }

    @Test
    void findApplicationLogShouldThrowException() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), anyLong())).thenThrow(new AnalyticsException("test_error"));
        long timestamp = Instant.now().toEpochMilli();

        assertThrows(TechnicalManagementException.class, () ->
            logService.findApplicationLog(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_ID, timestamp)
        );
    }

    @Test
    void findApplicationLogShouldReturnNull() throws Exception {
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), anyLong())).thenReturn(null);

        ApplicationRequest result = logService.findApplicationLog(
            EXECUTION_CONTEXT,
            LOG_APPLICATION_ID,
            LOG_ID,
            Instant.now().toEpochMilli()
        );

        assertThat(result).isNull();
        verify(logRepository, times(1)).findById(any(QueryContext.class), eq(LOG_ID), anyLong());
    }

    @Test
    void findApplicationLogShouldReturnSearchLogResponse() throws Exception {
        ExtendedLog log = newLog(JWT);
        log.setClientRequest(newRequest());
        log.setClientResponse(newResponse());
        when(logRepository.findById(any(QueryContext.class), eq(LOG_ID), anyLong())).thenReturn(log);
        when(apiSearchService.findGenericById(any(), anyString())).thenReturn(newApiEntity());
        when(planSearchService.findById(any(), anyString())).thenReturn(newPlan(KEY_LESS));

        ApplicationRequest result = logService.findApplicationLog(
            EXECUTION_CONTEXT,
            LOG_APPLICATION_ID,
            LOG_ID,
            Instant.now().toEpochMilli()
        );

        assertThat(result).isNotNull();
        verify(logRepository, times(1)).findById(any(QueryContext.class), eq(LOG_ID), anyLong());
        verify(apiSearchService, times(1)).findGenericById(any(), anyString());
        verify(planSearchService, times(1)).findById(any(), anyString());
    }

    @Test
    void exportAsCsvShouldReturnProperlyFormattedStringWithNoLog() {
        String exportAsCsv = logService.exportAsCsv(EXECUTION_CONTEXT, new SearchLogResponse<>(0));
        assertThat(exportAsCsv).isEmpty();
    }

    @Test
    void exportAsCsvShouldReturnProperlyFormattedStringWithApiRequestItems() {
        SearchLogResponse<LogItem> searchLogResponse = new SearchLogResponse<>(0);
        ApiRequestItem item1 = createApiRequestItem(
            1689838996000L,
            "id1",
            "transactionId1",
            "/path1",
            200,
            100L,
            "app1",
            HttpMethod.GET,
            "plan1"
        );
        ApiRequestItem item2 = createApiRequestItem(
            1689838999000L,
            "id2",
            "transactionId2",
            "/path2",
            201,
            523L,
            "app2",
            HttpMethod.POST,
            "plan2"
        );
        Map<String, Map<String, String>> metadata = new HashMap<>();
        metadata.put("plan1", Map.of("name", "PLAN 1"));
        metadata.put("app1", Map.of("name", "APP 1"));
        metadata.put("plan2", Map.of("name", "PLAN 2"));
        metadata.put("app2", Map.of("name", "APP 2"));

        searchLogResponse.setLogs(List.of(item1, item2));
        searchLogResponse.setMetadata(metadata);
        String exportAsCsv = logService.exportAsCsv(EXECUTION_CONTEXT, searchLogResponse);
        assertThat(exportAsCsv).isEqualTo(
            "Date;Request Id;Transaction Id;Method;Path;Status;Response Time;Plan;Application\n" +
                dateFormat.format(1689838996000L) +
                ";id1;transactionId1;GET;/path1;200;100;PLAN 1;APP 1\n" +
                dateFormat.format(1689838999000L) +
                ";id2;transactionId2;POST;/path2;201;523;PLAN 2;APP 2\n"
        );
    }

    @Test
    void exportAsCsvShouldEscapeContentToAvoidMaliciousOnes() {
        SearchLogResponse<LogItem> searchLogResponse = new SearchLogResponse<>(0);

        ApiRequestItem item1 = createApiRequestItem(
            LOG_TIMESTAMP,
            "id1",
            "transactionId1",
            "/path1;=cmd|'/Ccalc'!A0",
            200,
            100L,
            "app1",
            HttpMethod.GET,
            "plan1"
        );
        Map<String, Map<String, String>> metadata = new HashMap<>();
        metadata.put("plan1", Map.of("name", "=1+2\";=1+2"));
        metadata.put("app1", Map.of("name", "=1+3"));

        searchLogResponse.setLogs(List.of(item1));
        searchLogResponse.setMetadata(metadata);
        String exportAsCsv = logService.exportAsCsv(EXECUTION_CONTEXT, searchLogResponse);
        assertThat(exportAsCsv).isEqualTo(
            "Date;Request Id;Transaction Id;Method;Path;Status;Response Time;Plan;Application\n" +
                dateFormat.format(LOG_TIMESTAMP) +
                ";id1;transactionId1;GET;\"/path1;=cmd|'/Ccalc'!A0\";200;100;\"'=1+2\"\";=1+2\";\"'=1+3\"\n"
        );
    }

    @NotNull
    private static ApiRequestItem createApiRequestItem(
        long timestamp,
        String id,
        String transactionId,
        String path,
        int status,
        long responseTime,
        String application,
        HttpMethod httpMethod,
        String plan
    ) {
        ApiRequestItem item = new ApiRequestItem();
        item.setTimestamp(timestamp);
        item.setId(id);
        item.setTransactionId(transactionId);
        item.setPath(path);
        item.setStatus(status);
        item.setResponseTime(responseTime);
        item.setApplication(application);
        item.setMethod(httpMethod);
        item.setPlan(plan);
        return item;
    }

    private static ApiKeyEntity newApiKey() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setSubscriptions(Set.of(newSubscription()));
        return apiKey;
    }

    private static SubscriptionEntity newSubscription() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setApi(LOG_API_ID);
        subscription.setId(LOG_SUBSCRIPTION_ID);
        return subscription;
    }

    private static PlanEntity newPlan(PlanSecurityType securityType) {
        PlanEntity plan = new PlanEntity();
        plan.setSecurity(securityType);
        return plan;
    }

    private static io.gravitee.rest.api.model.v4.plan.PlanEntity newPlanV4(PlanSecurity security) {
        var plan = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        plan.setSecurity(security);
        return plan;
    }

    private static ExtendedLog newLog(PlanSecurityType securityType) {
        ExtendedLog log = new ExtendedLog();
        log.setApi(LOG_API_ID);
        log.setApplication(LOG_APPLICATION_ID);
        log.setPlan(LOG_PLAN_ID);
        if (securityType != null) {
            log.setSecurityType(securityType.name());
        }
        log.setUri(LOG_URI);
        log.setSecurityToken(LOG_API_KEY);
        return log;
    }

    private static ApplicationEntity newApplication() {
        ApplicationEntity application = new ApplicationEntity();
        application.setName(LOG_APPLICATION_NAME);
        return application;
    }

    private static Request newRequest() {
        Request request = new Request();
        request.setBody("");
        request.setMethod(HttpMethod.GET);
        request.setUri(LOG_URI);
        request.setHeaders(new HttpHeaders());

        return request;
    }

    private static Response newResponse() {
        Response response = new Response();
        response.setBody("");
        response.setStatus(200);
        response.setHeaders(new HttpHeaders());

        return response;
    }

    private ApiEntity newApiEntity() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setName(LOG_API_ID);
        apiEntity.setApiVersion("v1.0");

        return apiEntity;
    }
}
