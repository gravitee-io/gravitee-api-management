/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
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
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static io.gravitee.rest.api.model.PlanSecurityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class LogsServiceITest extends TestCase {

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

    @Test(expected = TechnicalManagementException.class)
    public void findApiLogShouldThrowException() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenThrow(new AnalyticsException());

        logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);
    }

    @Test
    public void findApiLogShouldReturnNull() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(null);

        ApiRequest result = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);
        assertNull(result);
        verify(logRepository, times(1)).findById(eq(LOG_ID), eq(LOG_TIMESTAMP));
    }

    @Test
    public void findApiLogShouldFindWithKeyLessPlan() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(KEY_LESS));
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
    public void findApiLogShouldFindWithApiKeyPlan() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(API_KEY));
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
    public void findApiLogShouldFindWithJwtPlan() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(JWT));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(JWT));
        when(subscriptionService.findByApplicationAndPlan(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_PLAN_ID))
            .thenReturn(Set.of(newSubscription()));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(JWT.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isEqualTo(LOG_SUBSCRIPTION_ID);
    }

    @Test
    public void findApiLogShouldNotFailOnApiKeyNotFoundException() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(API_KEY));
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
    public void findApiLogShouldNotFailOnPlanNotFoundException() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(JWT));
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
    public void findApiLogShouldNotFailOnDuplicatedSubscription() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog(JWT));
        when(applicationService.findById(EXECUTION_CONTEXT, LOG_APPLICATION_ID)).thenReturn(newApplication());
        when(planSearchService.findById(EXECUTION_CONTEXT, LOG_PLAN_ID)).thenReturn(newPlan(JWT));
        when(subscriptionService.findByApplicationAndPlan(EXECUTION_CONTEXT, LOG_APPLICATION_ID, LOG_PLAN_ID))
            .thenReturn(Set.of(newSubscription(), new SubscriptionEntity()));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getSecurityType()).isEqualTo(JWT.name());
        assertThat(apiLog.getApi()).isEqualTo(LOG_API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(LOG_APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(LOG_PLAN_ID);
        assertThat(apiLog.getSubscription()).isNull();
    }

    @Test(expected = TechnicalManagementException.class)
    public void findLogByApiShouldThrowException() throws Exception {
        when(logRepository.query(any())).thenThrow(new AnalyticsException());

        logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());
    }

    @Test
    public void findLogByApiShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any())).thenReturn(null);

        SearchLogResponse<ApiRequestItem> result = logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertNotNull(result);
        assertNull(result.getLogs());
        verify(logRepository, times(1)).query(any());
    }

    @Test
    public void findLogByApiShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any())).thenReturn(tabularResponse);

        SearchLogResponse<ApiRequestItem> result = logService.findByApi(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertNotNull(result);
        assertNotNull(result.getLogs());
        assertTrue(result.getLogs().isEmpty());
        verify(logRepository, times(1)).query(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findLogByApplicationShouldThrowException() throws Exception {
        when(logRepository.query(any())).thenThrow(new AnalyticsException());

        logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());
    }

    @Test
    public void findLogByApplicationShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any())).thenReturn(null);

        SearchLogResponse<ApplicationRequestItem> result = logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertNotNull(result);
        assertNull(result.getLogs());
        verify(logRepository, times(1)).query(any());
    }

    @Test
    public void findLogByApplicationShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any())).thenReturn(tabularResponse);

        SearchLogResponse<ApplicationRequestItem> result = logService.findByApplication(EXECUTION_CONTEXT, LOG_API_ID, new LogQuery());

        assertNotNull(result);
        assertNotNull(result.getLogs());
        assertTrue(result.getLogs().isEmpty());
        verify(logRepository, times(1)).query(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findLogByPlatformShouldThrowException() throws Exception {
        when(logRepository.query(any())).thenThrow(new AnalyticsException());

        logService.findPlatform(EXECUTION_CONTEXT, new LogQuery());
    }

    @Test
    public void findLogByPlatformShouldReturnEmptyResult() throws Exception {
        when(logRepository.query(any())).thenReturn(null);

        SearchLogResponse<PlatformRequestItem> result = logService.findPlatform(EXECUTION_CONTEXT, new LogQuery());

        assertNotNull(result);
        assertNull(result.getLogs());
        verify(logRepository, times(1)).query(any());
    }

    @Test
    public void findLogByPlatformShouldReturnSearchLogResponse() throws Exception {
        TabularResponse tabularResponse = new TabularResponse(0L);
        tabularResponse.setLogs(new ArrayList<>());

        when(logRepository.query(any())).thenReturn(tabularResponse);

        SearchLogResponse<PlatformRequestItem> result = logService.findPlatform(EXECUTION_CONTEXT, new LogQuery());

        assertNotNull(result);
        assertNotNull(result.getLogs());
        assertTrue(result.getLogs().isEmpty());
        verify(logRepository, times(1)).query(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findApplicationLogShouldThrowException() throws Exception {
        when(logRepository.findById(eq(LOG_ID), anyLong())).thenThrow(new AnalyticsException("test_error"));

        logService.findApplicationLog(EXECUTION_CONTEXT, LOG_ID, Instant.now().toEpochMilli());
    }

    @Test
    public void findApplicationLogShouldReturnNull() throws Exception {
        when(logRepository.findById(eq(LOG_ID), anyLong())).thenReturn(null);

        ApplicationRequest result = logService.findApplicationLog(EXECUTION_CONTEXT, LOG_ID, Instant.now().toEpochMilli());

        assertNull(result);
        verify(logRepository, times(1)).findById(eq(LOG_ID), anyLong());
    }

    @Test
    public void findApplicationLogShouldReturnSearchLogResponse() throws Exception {
        ExtendedLog log = newLog(JWT);
        log.setClientRequest(newRequest());
        log.setClientResponse(newResponse());
        when(logRepository.findById(eq(LOG_ID), anyLong())).thenReturn(log);
        when(apiSearchService.findGenericById(any(), anyString())).thenReturn(newApiEntity());
        when(planSearchService.findById(any(), anyString())).thenReturn(newPlan(KEY_LESS));

        ApplicationRequest result = logService.findApplicationLog(EXECUTION_CONTEXT, LOG_ID, Instant.now().toEpochMilli());

        assertNotNull(result);
        verify(logRepository, times(1)).findById(eq(LOG_ID), anyLong());
        verify(apiSearchService, times(1)).findGenericById(any(), anyString());
        verify(planSearchService, times(1)).findById(any(), anyString());
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

    private static ExtendedLog newLog(PlanSecurityType securityType) {
        ExtendedLog log = new ExtendedLog();
        log.setApi(LOG_API_ID);
        log.setApplication(LOG_APPLICATION_ID);
        log.setPlan(LOG_PLAN_ID);
        log.setSecurityType(securityType.name());
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
