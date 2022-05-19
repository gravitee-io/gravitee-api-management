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
package io.gravitee.rest.api.service.impl.upgrade;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.Proxy;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ApiLoggingConditionUpgraderTest {

    @InjectMocks
    @Spy
    private ApiLoggingConditionUpgrader upgrader = new ApiLoggingConditionUpgrader();

    @Mock
    private InstallationService installationService;

    @Mock
    private ApiRepository apiRepository;

    @Test
    public void upgrade_should_not_run_cause_already_executed_successfull() {
        mockInstallationWithExecutionStatus("SUCCESS");

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_not_run_cause_already_running() {
        mockInstallationWithExecutionStatus("RUNNING");

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_run_and_set_failure_status_on_exception() throws Exception {
        InstallationEntity installation = mockInstallationWithExecutionStatus(null);
        doThrow(new Exception("test exception")).when(upgrader).fixApis();

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.API_LOGGING_CONDITION_UPGRADER_STATUS, "RUNNING");
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.API_LOGGING_CONDITION_UPGRADER_STATUS, "FAILURE");
        verify(installationService, times(2)).setAdditionalInformation(installation.getAdditionalInformation());
    }

    @Test
    public void upgrade_should_run_and_set_success_status() throws Exception {
        InstallationEntity installation = mockInstallationWithExecutionStatus(null);
        doNothing().when(upgrader).fixApis();

        boolean success = upgrader.upgrade();

        assertTrue(success);
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.API_LOGGING_CONDITION_UPGRADER_STATUS, "RUNNING");
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.API_LOGGING_CONDITION_UPGRADER_STATUS, "SUCCESS");
        verify(installationService, times(2)).setAdditionalInformation(installation.getAdditionalInformation());
    }

    @Test
    public void fixApis_should_fix_only_apis_with_wrong_condition() throws Exception {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
        doNothing().when(upgrader).fixLoggingCondition(any(), any(), any());

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
        when(apiRepository.findAll())
            .thenReturn(
                Set.of(
                    apiEmpty,
                    apiProxyEmpty,
                    apiProxyLoggingEmpty,
                    apiProxyLoggingCondition,
                    apiProxyLoggingWrongCondition,
                    apiBooleanCondition
                )
            );

        upgrader.fixApis();

        verify(upgrader, never()).fixLoggingCondition(same(apiEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(same(apiProxyEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(same(apiProxyLoggingEmpty), any(), any());
        verify(upgrader, never()).fixLoggingCondition(same(apiProxyLoggingCondition), any(), any());

        verify(upgrader, times(1)).fixLoggingCondition(same(apiProxyLoggingWrongCondition), any(), eq("#request.timestamp < 1"));
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

        upgrader.fixLoggingCondition(api, apiDefinition, logging.getCondition());

        verify(apiRepository, times(1)).update(argThat(apiUpdated -> apiUpdated.getDefinition().contains("{#request.timestamp < 1}")));
    }

    private InstallationEntity mockInstallationWithExecutionStatus(String status) {
        InstallationEntity installation = mock(InstallationEntity.class);
        Map<String, String> installationAdditionalInformations = mock(Map.class);
        when(installation.getAdditionalInformation()).thenReturn(installationAdditionalInformations);
        when(installationAdditionalInformations.get(InstallationService.API_LOGGING_CONDITION_UPGRADER_STATUS)).thenReturn(status);
        when(installationService.getOrInitialize()).thenReturn(installation);
        return installation;
    }
}
