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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.Proxy;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.*;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ApiLoggingConditionUpgraderTest {

    @InjectMocks
    @Spy
    private ApiLoggingConditionUpgrader upgrader = new ApiLoggingConditionUpgrader();

    @Mock
    private ApiService apiService;

    @Mock
    private EventService eventService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    private ExecutionContext executionContext;

    @Before
    public void setUp() throws TechnicalException {
        when(apiRepository.update(any(Api.class))).thenAnswer((Answer<Api>) invocation -> (Api) invocation.getArguments()[0]);
        this.executionContext = GraviteeContext.getExecutionContext();
        Environment environment = new Environment();
        environment.setId(executionContext.getEnvironmentId());
        when(environmentRepository.findAll()).thenReturn(Collections.singleton(environment));
    }

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(environmentRepository.findAll()).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(environmentRepository, times(1)).findAll();
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    public void upgrade_should_not_run_cause_already_executed_successfully() throws UpgraderException {
        boolean success = upgrader.upgrade();

        assertTrue(success);
    }

    @Test(expected = UpgraderException.class)
    public void upgrade_should_run_and_set_failure_status_on_exception() throws UpgraderException {
        doThrow(new RuntimeException("test exception")).when(upgrader).fixApis(any());

        upgrader.upgrade();
    }

    @Test
    public void upgrade_should_run_and_set_success_status() throws Exception {
        doNothing().when(upgrader).fixApis(any());

        boolean success = upgrader.upgrade();

        assertTrue(success);
    }

    @Test
    public void fixApis_should_fix_only_apis_with_wrong_condition() throws Exception {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
        doNothing().when(upgrader).fixLoggingCondition(any(), any(), any(), any());

        Api apiEmpty = new Api();
        apiEmpty.setId("api1");
        apiEmpty.setDefinition("{}");
        Api apiProxyEmpty = new Api();
        apiProxyEmpty.setId("api2");
        apiProxyEmpty.setDefinition("{\"proxy\": {} }");
        Api apiProxyLoggingEmpty = new Api();
        apiProxyLoggingEmpty.setId("api3");
        apiProxyLoggingEmpty.setDefinition("{\"proxy\": {\"logging\": {}} }");
        Api apiProxyLoggingCondition = new Api();
        apiProxyLoggingCondition.setId("api4");
        apiProxyLoggingCondition.setDefinition("{\"proxy\": {\"logging\": {\"condition\": \"{#request.timestamp < 1 }\"}}}");

        Api apiProxyLoggingWrongCondition = new Api();
        apiProxyLoggingWrongCondition.setId("api5");
        apiProxyLoggingWrongCondition.setDefinition("{\"proxy\": {\"logging\": {\"condition\": \"#request.timestamp < 1\"}}}");

        Api apiBooleanCondition = new Api();
        apiBooleanCondition.setId("api6");
        apiBooleanCondition.setDefinition("{\"proxy\": {\"logging\": {\"condition\": \"true\"}}}");
        when(apiRepository.search(any(), isNull(), isA(ApiFieldFilter.class))).thenReturn(
            Stream.of(
                apiEmpty,
                apiProxyEmpty,
                apiProxyLoggingEmpty,
                apiProxyLoggingCondition,
                apiProxyLoggingWrongCondition,
                apiBooleanCondition
            )
        );

        upgrader.fixApis(executionContext);

        verify(upgrader, never()).fixLoggingCondition(any(), same(apiEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(any(), same(apiProxyEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(any(), same(apiProxyLoggingEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(any(), same(apiProxyLoggingCondition), any(), any());

        verify(upgrader, times(1)).fixLoggingCondition(any(), same(apiProxyLoggingWrongCondition), any(), eq("#request.timestamp < 1"));
    }

    @Test
    public void fixLoggingCondition_should_update_api() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ReflectionTestUtils.setField(upgrader, "objectMapper", objectMapper);
        Api api = new Api();
        api.setId("api5");
        api.setDefinition("{\"proxy\": {\"logging\": {\"condition\": \"#request.timestamp < 1\"}}}");

        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        Proxy proxy = new Proxy();
        Logging logging = new Logging();
        logging.setCondition("#request.timestamp < 1");
        proxy.setLogging(logging);
        apiDefinition.setProxy(proxy);

        upgrader.fixLoggingCondition(executionContext, api, apiDefinition, logging.getCondition());

        verify(apiRepository, times(1)).update(argThat(apiUpdated -> apiUpdated.getDefinition().contains("{#request.timestamp < 1}")));
        verify(eventService, times(0)).createApiEvent(
            eq(executionContext),
            anySet(),
            anyString(),
            eq(PUBLISH_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getDefinition().contains("{#request.timestamp < 1}")),
            any()
        );
    }

    @Test
    public void fixLoggingCondition_should_update_deploy_api() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ReflectionTestUtils.setField(upgrader, "objectMapper", objectMapper);
        Api api = new Api();
        api.setId("api5");
        api.setDefinition("{\"proxy\": {\"logging\": {\"condition\": \"#request.timestamp < 1\"}}}");

        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        Proxy proxy = new Proxy();
        Logging logging = new Logging();
        logging.setCondition("#request.timestamp < 1");
        proxy.setLogging(logging);
        apiDefinition.setProxy(proxy);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        when(apiService.isSynchronized(executionContext, api.getId())).thenReturn(true);

        upgrader.fixLoggingCondition(executionContext, api, apiDefinition, logging.getCondition());

        verify(apiRepository, times(1)).update(argThat(apiUpdated -> apiUpdated.getDefinition().contains("{#request.timestamp < 1}")));
        verify(eventService, times(1)).createApiEvent(
            eq(executionContext),
            anySet(),
            anyString(),
            eq(PUBLISH_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getDefinition().contains("{#request.timestamp < 1}")),
            any()
        );
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.API_LOGGING_CONDITION_UPGRADER, upgrader.getOrder());
    }
}
