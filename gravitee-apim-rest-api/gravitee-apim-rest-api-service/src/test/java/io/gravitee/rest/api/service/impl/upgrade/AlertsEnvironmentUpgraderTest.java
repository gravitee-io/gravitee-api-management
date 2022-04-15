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

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertsEnvironmentUpgraderTest {

    @InjectMocks
    AlertsEnvironmentUpgrader upgrader;

    @Mock
    AlertTriggerRepository alertTriggerRepository;

    @Mock
    ApiRepository apiRepository;

    @Mock
    ApplicationRepository applicationRepository;

    AlertTriggerConverter alertTriggerConverter = new AlertTriggerConverter(new ObjectMapper());

    @Before
    public void setup() {
        upgrader = new AlertsEnvironmentUpgrader(alertTriggerRepository, alertTriggerConverter, apiRepository, applicationRepository);
    }

    @Test
    public void upgrade_should_set_environment_id_of_api_alerts() throws Exception {
        when(alertTriggerRepository.findAll())
            .thenReturn(
                Set.of(
                    buildTestAlert("alert-id-2", "alert-name-2", "API", "api-id-1"),
                    buildTestAlert("alert-id-3", "alert-name-4", "API", "api-id-2"),
                    buildTestAlert("alert-id-4", "alert-name-4", "API", "api-id-3")
                )
            );

        Api api1 = new Api();
        api1.setEnvironmentId("env-id-1");
        when(apiRepository.findById("api-id-1")).thenReturn(Optional.of(api1));

        when(apiRepository.findById("api-id-2")).thenReturn(Optional.empty());

        Api api3 = new Api();
        api3.setEnvironmentId("env-id-2");
        when(apiRepository.findById("api-id-3")).thenReturn(Optional.of(api3));

        upgrader.processOneShotUpgrade();

        // 2 alerts have been updated with API environment
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-2", "alert-name-2", "API", "api-id-1", "env-id-1")));
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-4", "alert-name-4", "API", "api-id-3", "env-id-2")));

        verify(alertTriggerRepository, times(1)).findAll();
        verifyNoMoreInteractions(alertTriggerRepository);
    }

    @Test
    public void upgrade_should_set_environment_id_of_application_alerts() throws Exception {
        when(alertTriggerRepository.findAll())
            .thenReturn(
                Set.of(
                    buildTestAlert("alert-id-2", "alert-name-2", "APPLICATION", "app-id-1"),
                    buildTestAlert("alert-id-3", "alert-name-4", "APPLICATION", "app-id-2"),
                    buildTestAlert("alert-id-4", "alert-name-4", "APPLICATION", "app-id-3")
                )
            );

        Application application1 = new Application();
        application1.setEnvironmentId("env-id-1");
        when(applicationRepository.findById("app-id-1")).thenReturn(Optional.of(application1));

        when(applicationRepository.findById("app-id-2")).thenReturn(Optional.empty());

        Application application3 = new Application();
        application3.setEnvironmentId("env-id-2");
        when(applicationRepository.findById("app-id-3")).thenReturn(Optional.of(application3));

        upgrader.processOneShotUpgrade();

        // 2 alerts have been updated with applications environment
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-2", "alert-name-2", "APPLICATION", "app-id-1", "env-id-1")));
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-4", "alert-name-4", "APPLICATION", "app-id-3", "env-id-2")));

        verify(alertTriggerRepository, times(1)).findAll();
        verifyNoMoreInteractions(alertTriggerRepository);
    }

    @Test
    public void upgrade_should_update_platform_alerts() throws Exception {
        when(alertTriggerRepository.findAll())
            .thenReturn(
                Set.of(buildTestAlert("alert-id-2", "alert-name-2", "PLATFORM"), buildTestAlert("alert-id-4", "alert-name-4", "PLATFORM"))
            );

        upgrader.processOneShotUpgrade();

        // existing platform alert have been updated and linked to DEFAULT env
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-2", "alert-name-2", "ENVIRONMENT", "DEFAULT", "DEFAULT")));
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-4", "alert-name-4", "ENVIRONMENT", "DEFAULT", "DEFAULT")));

        verify(alertTriggerRepository, times(1)).findAll();
        verifyNoMoreInteractions(alertTriggerRepository);
    }

    private boolean alertMatches(
        AlertTrigger alert,
        String id,
        String name,
        String referenceType,
        String referenceId,
        String environmentId
    ) {
        return alertMatches(alert, name, referenceType, referenceId, environmentId) && id.equals(alert.getId());
    }

    private boolean alertMatches(AlertTrigger alert, String name, String referenceType, String referenceId, String environmentId) {
        return (
            alert.getName().equals(name) &&
            alert.getReferenceType().equals(referenceType) &&
            alert.getReferenceId().equals(referenceId) &&
            alert.getEnvironmentId().equals(environmentId)
        );
    }

    private AlertTrigger buildTestAlert(String id, String name, String referenceType) {
        AlertTrigger alert = new AlertTrigger();
        alert.setId(id);
        alert.setReferenceType(referenceType);
        alert.setName(name);

        JsonNode definitionNode = new ObjectMapper().valueToTree(alert);
        ((ObjectNode) definitionNode).put("source", "this is mandatory in definition");
        alert.setDefinition(definitionNode.toString());

        return alert;
    }

    private AlertTrigger buildTestAlert(String id, String name, String referenceType, String referenceId) {
        AlertTrigger alert = buildTestAlert(id, name, referenceType);
        alert.setReferenceId(referenceId);
        return alert;
    }
}
