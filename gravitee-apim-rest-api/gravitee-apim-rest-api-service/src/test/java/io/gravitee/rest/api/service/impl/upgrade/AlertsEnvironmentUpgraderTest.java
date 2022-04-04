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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.List;
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
public class AlertsEnvironmentUpgraderTest {

    @InjectMocks
    AlertsEnvironmentUpgrader upgrader;

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    AlertTriggerRepository alertTriggerRepository;

    @Test
    public void testUpgrade() throws TechnicalException {
        when(alertTriggerRepository.findAll())
            .thenReturn(
                Set.of(
                    buildTestAlert("alert-id-1", "alert-name-1", "API"),
                    buildTestAlert("alert-id-2", "alert-name-2", "PLATFORM"),
                    buildTestAlert("alert-id-3", "alert-name-3", "APPLICATION"),
                    buildTestAlert("alert-id-4", "alert-name-4", "PLATFORM")
                )
            );

        when(environmentRepository.findAll())
            .thenReturn(Set.of(buildTestEnvironment("DEFAULT"), buildTestEnvironment("env-id-1"), buildTestEnvironment("env-id-2")));

        upgrader.processOneShotUpgrade(GraviteeContext.getExecutionContext());

        // existing platform alert have been updated and linked to DEFAULT env
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-2", "alert-name-2", "ENVIRONMENT", "DEFAULT")));
        verify(alertTriggerRepository, times(1))
            .update(argThat(alert -> alertMatches(alert, "alert-id-4", "alert-name-4", "ENVIRONMENT", "DEFAULT")));

        // same alerts have been created on env-id-1
        verify(alertTriggerRepository, times(1))
            .create(argThat(alert -> alertMatches(alert, null, "alert-name-2", "ENVIRONMENT", "env-id-1")));
        verify(alertTriggerRepository, times(1))
            .create(argThat(alert -> alertMatches(alert, null, "alert-name-4", "ENVIRONMENT", "env-id-1")));

        // same alerts have been created on env-id-2
        verify(alertTriggerRepository, times(1))
            .create(argThat(alert -> alertMatches(alert, null, "alert-name-2", "ENVIRONMENT", "env-id-2")));
        verify(alertTriggerRepository, times(1))
            .create(argThat(alert -> alertMatches(alert, null, "alert-name-4", "ENVIRONMENT", "env-id-2")));

        verify(alertTriggerRepository, times(1)).findAll();
        verifyNoMoreInteractions(alertTriggerRepository);
    }

    private boolean alertMatches(AlertTrigger alert, String id, String name, String referenceType, String referenceId) {
        return (
            ((alert.getId() == null && id == null) || id.equals(alert.getId())) &&
            alert.getName().equals(name) &&
            alert.getReferenceType().equals(referenceType) &&
            alert.getReferenceId().equals(referenceId)
        );
    }

    private Environment buildTestEnvironment(String id) {
        Environment environment = new Environment();
        environment.setId(id);
        return environment;
    }

    private AlertTrigger buildTestAlert(String id, String name, String referenceType) {
        AlertTrigger alert = new AlertTrigger();
        alert.setId(id);
        alert.setReferenceType(referenceType);
        alert.setName(name);
        return alert;
    }
}
