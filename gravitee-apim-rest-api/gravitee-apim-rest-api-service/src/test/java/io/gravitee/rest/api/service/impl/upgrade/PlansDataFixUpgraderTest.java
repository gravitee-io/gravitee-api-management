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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PlansDataFixUpgraderTest {

    @InjectMocks
    @Spy
    private PlansDataFixUpgrader upgrader = new PlansDataFixUpgrader();

    @Mock
    private InstallationService installationService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private EmailService emailService;

    @Test
    public void upgrade_should_not_run_cause_not_enabled() {
        ReflectionTestUtils.setField(upgrader, "enabled", false);

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verifyNoInteractions(installationService);
    }

    @Test
    public void upgrade_should_not_run_cause_already_executed_successfull() {
        ReflectionTestUtils.setField(upgrader, "enabled", true);
        mockInstallationWithExecutionStatus("SUCCESS");

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_not_run_cause_already_running() {
        ReflectionTestUtils.setField(upgrader, "enabled", true);
        mockInstallationWithExecutionStatus("RUNNING");

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_run_and_set_failure_status_on_exception() throws Exception {
        ReflectionTestUtils.setField(upgrader, "enabled", true);
        InstallationEntity installation = mockInstallationWithExecutionStatus(null);
        doThrow(new Exception("test exception")).when(upgrader).processOneShotUpgrade();

        boolean success = upgrader.upgrade();

        assertFalse(success);
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "RUNNING");
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "FAILURE");
        verify(installationService, times(2)).setAdditionalInformation(installation.getAdditionalInformation());
    }

    @Test
    public void upgrade_should_run_and_set_success_status() throws Exception {
        ReflectionTestUtils.setField(upgrader, "enabled", true);
        InstallationEntity installation = mockInstallationWithExecutionStatus(null);
        doNothing().when(upgrader).processOneShotUpgrade();

        boolean success = upgrader.upgrade();

        assertTrue(success);
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "RUNNING");
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "SUCCESS");
        verify(installationService, times(2)).setAdditionalInformation(installation.getAdditionalInformation());
    }

    @Test
    public void upgrade_should_run_and_set_dry_success_status() throws Exception {
        ReflectionTestUtils.setField(upgrader, "enabled", true);
        ReflectionTestUtils.setField(upgrader, "dryRun", true);
        InstallationEntity installation = mockInstallationWithExecutionStatus(null);
        doNothing().when(upgrader).processOneShotUpgrade();

        boolean success = upgrader.upgrade();

        assertTrue(success);
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "RUNNING");
        verify(installation.getAdditionalInformation(), times(1)).put(InstallationService.PLANS_DATA_UPGRADER_STATUS, "DRY_SUCCESS");
        verify(installationService, times(2)).setAdditionalInformation(installation.getAdditionalInformation());
    }

    @Test
    public void fixPlansData_should_fix_only_v2_apis() throws Exception {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
        doNothing().when(upgrader).fixApiPlans(any(), any(), any());
        when(environmentRepository.findById(any())).thenAnswer(i -> Optional.of(buildTestEnvironment(i.getArgument(0), "testOrgId")));

        Api apiv1_1 = new Api();
        apiv1_1.setId("api1");
        apiv1_1.setDefinition("{}");
        apiv1_1.setEnvironmentId("envId");
        Api apiv2_1 = new Api();
        apiv2_1.setId("api2");
        apiv2_1.setDefinition("{\"gravitee\": \"2.0.0\"}");
        apiv2_1.setEnvironmentId("envId");
        Api apiv1_2 = new Api();
        apiv1_2.setId("api3");
        apiv1_2.setDefinition("{\"gravitee\": \"1.0.0\"}");
        apiv1_2.setEnvironmentId("envId");
        Api apiv2_2 = new Api();
        apiv2_2.setId("api4");
        apiv2_2.setDefinition("{\"gravitee\": \"2.0.0\"}");
        apiv2_2.setEnvironmentId("envId");
        when(apiRepository.findAll()).thenReturn(Set.of(apiv1_1, apiv2_1, apiv1_2, apiv2_2));

        upgrader.processOneShotUpgrade();

        verify(upgrader, times(1)).fixApiPlans(any(), same(apiv2_1), any());
        verify(upgrader, times(1)).fixApiPlans(any(), same(apiv2_2), any());
        verify(upgrader, never()).fixApiPlans(any(), same(apiv1_1), any());
        verify(upgrader, never()).fixApiPlans(any(), same(apiv1_2), any());
    }

    @Test
    public void fixPlansData_should_fix_apis_using_related_executionContext() throws Exception {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
        doNothing().when(upgrader).fixApiPlans(any(), any(), any());

        Api api1 = new Api();
        api1.setId("api1");
        api1.setEnvironmentId("env1");
        api1.setDefinition("{\"gravitee\": \"2.0.0\"}");
        Api api2 = new Api();
        api2.setId("api2");
        api2.setEnvironmentId("env2");
        api2.setDefinition("{\"gravitee\": \"2.0.0\"}");
        Api api3 = new Api();
        api3.setId("api3");
        api3.setEnvironmentId("env1");
        api3.setDefinition("{\"gravitee\": \"2.0.0\"}");

        when(apiRepository.findAll()).thenReturn(Set.of(api1, api2, api3));

        when(environmentRepository.findById("env1")).thenReturn(Optional.of(buildTestEnvironment("env1", "org1")));
        when(environmentRepository.findById("env2")).thenReturn(Optional.of(buildTestEnvironment("env2", "org2")));

        upgrader.processOneShotUpgrade();

        // assert that 2 environments have been retreived from repository
        verify(environmentRepository, times(1)).findById("env1");
        verify(environmentRepository, times(1)).findById("env2");
        verifyNoMoreInteractions(environmentRepository);

        // assert that each call to fixApiPlans has been done with the ExecutionContext related to API
        verify(upgrader, times(1))
            .fixApiPlans(argThat(e -> e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")), same(api1), any());
        verify(upgrader, times(1))
            .fixApiPlans(argThat(e -> e.getEnvironmentId().equals("env2") && e.getOrganizationId().equals("org2")), same(api2), any());
        verify(upgrader, times(1))
            .fixApiPlans(argThat(e -> e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")), same(api3), any());
    }

    @Test
    public void fixPlansData_should_not_fix_apis_if_environment_search_throws_exception() throws Exception {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());

        Api api1 = new Api();
        api1.setId("api1");
        api1.setEnvironmentId("env1");
        api1.setDefinition("{\"gravitee\": \"2.0.0\"}");

        when(apiRepository.findAll()).thenReturn(Set.of(api1));
        when(environmentRepository.findById(any())).thenThrow(new TechnicalException("this is a test exception"));

        upgrader.processOneShotUpgrade();

        // assert that fixApiPlans has never been called as environment search failed
        verify(upgrader, never()).fixApiPlans(any(), any(), any());
    }

    @Test
    public void closeExtraApiPlans_should_close_extra_plans() throws TechnicalException {
        Map<String, Plan> apiPlansMap = new HashMap<>(Map.of("plan12", new Plan(), "plan47", new Plan(), "plan78", new Plan()));
        apiPlansMap.get("plan12").setId("plan12");
        apiPlansMap.get("plan47").setId("plan47");
        apiPlansMap.get("plan78").setId("plan78");

        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap = Map.of(
            "plan78",
            new io.gravitee.definition.model.Plan(),
            "plan22",
            new io.gravitee.definition.model.Plan(),
            "plan12",
            new io.gravitee.definition.model.Plan(),
            "plan88",
            new io.gravitee.definition.model.Plan()
        );

        upgrader.closeExtraApiPlans(definitionPlansMap, apiPlansMap);

        verify(planRepository, times(1)).update(argThat(plan -> plan.getId().equals("plan47") && plan.getStatus() == Plan.Status.CLOSED));
        verifyNoMoreInteractions(planRepository);
    }

    @Test
    public void addMissingApiPlans_should_create_all_missing_plans() throws TechnicalException {
        Map<String, Plan> apiPlansMap = new HashMap<>(Map.of("plan12", new Plan(), "plan47", new Plan(), "plan78", new Plan()));
        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap = Map.of(
            "plan78",
            new io.gravitee.definition.model.Plan(),
            "plan22",
            new io.gravitee.definition.model.Plan(),
            "plan12",
            new io.gravitee.definition.model.Plan(),
            "plan88",
            new io.gravitee.definition.model.Plan()
        );
        definitionPlansMap.get("plan22").setName("name plan22");
        definitionPlansMap.get("plan88").setName("name plan88");

        Api api = new Api();
        api.setId("my-api-id");

        upgrader.createMissingApiPlans(definitionPlansMap, apiPlansMap, api);

        assertEquals(5, apiPlansMap.size());
        assertEquals(4, definitionPlansMap.size());
        assertTrue(apiPlansMap.containsKey("plan12"));
        assertTrue(apiPlansMap.containsKey("plan78"));
        assertTrue(apiPlansMap.containsKey("plan47"));
        assertTrue(apiPlansMap.containsKey("plan22"));
        assertTrue(apiPlansMap.containsKey("plan88"));
        verify(planRepository, times(2)).create(argThat(plan -> plan.getApi().equals("my-api-id")));
    }

    @Test
    public void sendEmailToApiOwner_should_retrieve_api_owner_from_apiService() {
        Api api = new Api();
        api.setId("my-api-id");

        when(primaryOwnerService.getPrimaryOwner(GraviteeContext.getExecutionContext(), "my-api-id")).thenReturn(new PrimaryOwnerEntity());

        upgrader.sendEmailToApiOwner(GraviteeContext.getExecutionContext(), api, Collections.emptyList(), Collections.emptyList());

        verify(primaryOwnerService, times(1)).getPrimaryOwner(GraviteeContext.getExecutionContext(), "my-api-id");
    }

    @Test
    public void sendEmailToApiOwner_send_email_to_owner_using_mailService() {
        Api api = new Api();
        api.setId("my-api-id");

        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity();
        primaryOwner.setEmail("primary-owner-email");
        when(primaryOwnerService.getPrimaryOwner(GraviteeContext.getExecutionContext(), "my-api-id")).thenReturn(primaryOwner);

        List<Plan> createdPlans = new ArrayList<>();
        List<Plan> closedPlans = new ArrayList<>();
        upgrader.sendEmailToApiOwner(GraviteeContext.getExecutionContext(), api, createdPlans, closedPlans);

        verify(emailService, times(1))
            .sendAsyncEmailNotification(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    notification ->
                        notification.getTo()[0].equals("primary-owner-email") &&
                        notification.getParams().get("api") == api &&
                        notification.getParams().get("createdPlans") == createdPlans &&
                        notification.getParams().get("closedPlans") == closedPlans
                )
            );
    }

    private InstallationEntity mockInstallationWithExecutionStatus(String status) {
        InstallationEntity installation = mock(InstallationEntity.class);
        Map<String, String> installationAdditionalInformations = mock(Map.class);
        when(installation.getAdditionalInformation()).thenReturn(installationAdditionalInformations);
        when(installationAdditionalInformations.get(InstallationService.PLANS_DATA_UPGRADER_STATUS)).thenReturn(status);
        when(installationService.getOrInitialize()).thenReturn(installation);
        return installation;
    }

    private Environment buildTestEnvironment(String environmentId, String organizationId) {
        Environment environment = new Environment();
        environment.setId(environmentId);
        environment.setOrganizationId(organizationId);
        return environment;
    }
}
