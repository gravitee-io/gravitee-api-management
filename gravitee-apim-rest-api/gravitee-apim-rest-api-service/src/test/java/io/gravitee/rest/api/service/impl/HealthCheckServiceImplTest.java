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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityResponse;
import io.gravitee.repository.healthcheck.query.log.ExtendedLog;
import io.gravitee.repository.healthcheck.query.log.LogsResponse;
import io.gravitee.repository.healthcheck.query.log.Step;
import io.gravitee.repository.healthcheck.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeResponse;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.healthcheck.ApiMetrics;
import io.gravitee.rest.api.model.healthcheck.Log;
import io.gravitee.rest.api.model.healthcheck.SearchLogResponse;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AnalyticsCalculateException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthCheckServiceImplTest {

    @Mock
    private HealthCheckRepository healthCheckRepository;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private InstanceService instanceService;

    @InjectMocks
    private HealthCheckServiceImpl cut;

    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    @Test
    public void should_throw_analytics_exception_for_getAvailability_metadata() throws Exception {
        when(apiSearchService.findGenericById(any(ExecutionContext.class), anyString())).thenReturn(newApiEntity());
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());
        ApiMetrics result = cut.getAvailability(EXECUTION_CONTEXT, "test_api", AvailabilityQuery.Field.ENDPOINT.name());

        assertNull(result);
        verify(apiSearchService, times(1)).findGenericById(any(ExecutionContext.class), anyString());
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void should_get_noResponse_for_getAvailability_metadata() throws AnalyticsException {
        when(apiSearchService.findGenericById(any(ExecutionContext.class), anyString())).thenReturn(new ApiEntity());
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(null);
        ApiMetrics result = cut.getAvailability(EXECUTION_CONTEXT, "test_api", AvailabilityQuery.Field.ENDPOINT.name());

        assertNull(result);
        verify(apiSearchService, times(1)).findGenericById(any(ExecutionContext.class), anyString());
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void should_getAvailability_metadata() throws Exception {
        FieldBucket fieldBucketEndpoint = new FieldBucket("endpoint");
        fieldBucketEndpoint.setValues(Collections.emptyList());

        FieldBucket fieldBucketDeleted = new FieldBucket("deleted");
        fieldBucketDeleted.setValues(Collections.emptyList());

        AvailabilityResponse availabilityResponse = new AvailabilityResponse();
        availabilityResponse.setEndpointAvailabilities(List.of(fieldBucketEndpoint, fieldBucketDeleted));

        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(availabilityResponse);

        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "apiId")).thenReturn(newApiEntity());

        ApiMetrics apiMetrics = cut.getAvailability(
            GraviteeContext.getExecutionContext(),
            "apiId",
            AvailabilityQuery.Field.ENDPOINT.name()
        );

        assertEquals(Map.of("target", "http"), apiMetrics.getMetadata().get("endpoint"));
        assertEquals(Map.of("deleted", "true"), apiMetrics.getMetadata().get("deleted"));
    }

    @Test(expected = AnalyticsCalculateException.class)
    public void shouldThrowExceptionForDateHistogramQuery() throws Exception {
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());

        cut.query(GraviteeContext.getExecutionContext(), new DateHistogramQuery());
    }

    @Test
    public void shouldFindNoAnalytics() throws AnalyticsException {
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(null);
        Analytics execute = cut.query(GraviteeContext.getExecutionContext(), new DateHistogramQuery());

        assertNull(execute);
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldFindAnalytics() throws AnalyticsException {
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(new DateHistogramResponse());
        Analytics execute = cut.query(GraviteeContext.getExecutionContext(), new DateHistogramQuery());

        assertNotNull(execute);
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldThrowExceptionForFindById() throws Exception {
        when(healthCheckRepository.findById(any(QueryContext.class), anyString())).thenThrow(new AnalyticsException());
        Log result = cut.findLog(GraviteeContext.getExecutionContext(), "test_api", "test_log");

        assertNull(result);
        verify(healthCheckRepository, times(1)).findById(any(QueryContext.class), anyString());
    }

    @Test
    public void shouldFindNoLogById() throws AnalyticsException {
        when(healthCheckRepository.findById(any(QueryContext.class), anyString())).thenReturn(null);
        Log result = cut.findLog(GraviteeContext.getExecutionContext(), "test_api", "test_log");

        assertNull(result);
        verify(healthCheckRepository, times(1)).findById(any(QueryContext.class), anyString());
    }

    @Test
    public void shouldReturnNullBecauseLogDoesNotBelongToApi() throws AnalyticsException {
        ExtendedLog log = newExtendedLog();
        when(healthCheckRepository.findById(any(QueryContext.class), anyString())).thenReturn(log);
        Log result = cut.findLog(GraviteeContext.getExecutionContext(), "Another_api", "test_log");

        assertNull(result);
        verify(healthCheckRepository, times(1)).findById(any(QueryContext.class), anyString());
    }

    @Test
    public void shouldFindLogById() throws AnalyticsException {
        ExtendedLog log = newExtendedLog();

        when(healthCheckRepository.findById(any(QueryContext.class), anyString())).thenReturn(log);
        Log result = cut.findLog(GraviteeContext.getExecutionContext(), "test_api", "test_log");

        assertNotNull(result);
        verify(healthCheckRepository, times(1)).findById(any(QueryContext.class), anyString());
    }

    @Test
    public void shouldThrowExceptionForFindByApi() throws Exception {
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());
        SearchLogResponse result = cut.findByApi(EXECUTION_CONTEXT, "test_api", new LogQuery(), true);

        assertNull(result);
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldFindNoLogByApi() throws AnalyticsException {
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(null);
        SearchLogResponse result = cut.findByApi(EXECUTION_CONTEXT, "test_api", new LogQuery(), true);

        assertNull(result);
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldFindLogByApi() throws AnalyticsException {
        LogsResponse response = new LogsResponse(1L);
        List<io.gravitee.repository.healthcheck.query.log.Log> logs = new ArrayList<>();
        logs.add(new io.gravitee.repository.healthcheck.query.log.Log());
        response.setLogs(logs);

        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(response);
        SearchLogResponse result = cut.findByApi(EXECUTION_CONTEXT, "test_api", new LogQuery(), true);

        assertNotNull(result);
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldThrowExceptionForGetResponseTime() throws Exception {
        when(apiSearchService.findGenericById(any(ExecutionContext.class), anyString())).thenReturn(newApiEntity());
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenThrow(new AnalyticsException());
        ApiMetrics result = cut.getResponseTime(EXECUTION_CONTEXT, "test_api", AvailabilityQuery.Field.ENDPOINT.name());

        assertNull(result);
        verify(apiSearchService, times(1)).findGenericById(any(ExecutionContext.class), anyString());
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldFindNoResponseTime() throws AnalyticsException {
        when(apiSearchService.findGenericById(any(ExecutionContext.class), anyString())).thenReturn(new ApiEntity());
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(null);
        ApiMetrics result = cut.getResponseTime(EXECUTION_CONTEXT, "test_api", AvailabilityQuery.Field.ENDPOINT.name());

        assertNull(result);
        verify(apiSearchService, times(1)).findGenericById(any(ExecutionContext.class), anyString());
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
    }

    @Test
    public void shouldFindGetResponseTime() throws AnalyticsException {
        AverageResponseTimeResponse response = new AverageResponseTimeResponse();
        List<FieldBucket<Long>> buckets = new ArrayList<>();
        FieldBucket<Long> testBucket = new FieldBucket<>("test_bucket");
        testBucket.setValues(new ArrayList<>());
        buckets.add(testBucket);

        response.setEndpointResponseTimes(buckets);

        when(apiSearchService.findGenericById(any(ExecutionContext.class), anyString())).thenReturn(new ApiEntity());
        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(response);
        when(instanceService.findById(any(ExecutionContext.class), anyString())).thenReturn(new InstanceEntity());
        ApiMetrics result = cut.getResponseTime(EXECUTION_CONTEXT, "test_api", AvailabilityQuery.Field.GATEWAY.name());

        assertNotNull(result);
        verify(apiSearchService, times(1)).findGenericById(any(ExecutionContext.class), anyString());
        verify(healthCheckRepository, times(1)).query(any(QueryContext.class), any());
        verify(instanceService, times(1)).findById(any(ExecutionContext.class), anyString());
    }

    @Test
    public void should_getAvailability_metadata_for_V2() throws Exception {
        FieldBucket fieldBucketEndpoint = new FieldBucket("endpoint");
        fieldBucketEndpoint.setValues(Collections.emptyList());

        AvailabilityResponse availabilityResponse = new AvailabilityResponse();
        availabilityResponse.setEndpointAvailabilities(List.of(fieldBucketEndpoint));

        when(healthCheckRepository.query(any(QueryContext.class), any())).thenReturn(availabilityResponse);
        when(apiSearchService.findGenericById(EXECUTION_CONTEXT, "apiId")).thenReturn(newApiEntityV2());

        ApiMetrics apiMetrics = cut.getAvailability(EXECUTION_CONTEXT, "apiId", AvailabilityQuery.Field.ENDPOINT.name());

        assertEquals(Map.of("target", "http://localhost"), apiMetrics.getMetadata().get("endpoint"));
    }

    @NonNull
    private static ApiEntity newApiEntity() {
        ApiEntity api = new ApiEntity();
        api.setId("apiId");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setEndpointGroups(List.of(newEndpointGroup()));
        return api;
    }

    @NonNull
    private static EndpointGroup newEndpointGroup() {
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setEndpoints(List.of(endpoint));
        return endpointGroup;
    }

    @NonNull
    private static ExtendedLog newExtendedLog() {
        ExtendedLog log = new ExtendedLog();
        List<Step> steps = new ArrayList<>();
        Step step = new Step();
        step.setMessage("test_message");
        steps.add(step);
        log.setSteps(steps);
        log.setApi("test_api");
        return log;
    }

    private static io.gravitee.rest.api.model.api.ApiEntity newApiEntityV2() {
        io.gravitee.rest.api.model.api.ApiEntity api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId("apiId");
        api.setGraviteeDefinitionVersion("2.0.0");
        io.gravitee.definition.model.Endpoint endpoint = new io.gravitee.definition.model.Endpoint();
        endpoint.setName("endpoint");
        endpoint.setTarget("http://localhost");
        io.gravitee.definition.model.EndpointGroup group = new io.gravitee.definition.model.EndpointGroup();
        group.setEndpoints(Set.of(endpoint));
        io.gravitee.definition.model.Proxy proxy = new io.gravitee.definition.model.Proxy();
        proxy.setGroups(Set.of(group));
        api.setProxy(proxy);

        return api;
    }
}
