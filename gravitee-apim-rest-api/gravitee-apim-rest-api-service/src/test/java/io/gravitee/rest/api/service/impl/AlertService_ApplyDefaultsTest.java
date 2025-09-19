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

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.AlertTemplateInvalidException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_ApplyDefaultsTest extends AlertServiceTest {

    @Test
    public void applyDefaults_should_call_apiRepository_to_find_apis_using_environment_as_criteria()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity(true);
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findById("my-alert")).thenReturn(Optional.of(alertTrigger));

        when(
            apiRepository.searchIds(
                argThat(criteria -> criteria.get(0).getEnvironmentId().equals(GraviteeContext.getCurrentEnvironment())),
                any(Pageable.class),
                isNull()
            )
        ).thenReturn(new Page<>(null, 0, 1, 0));

        alertService.applyDefaults(GraviteeContext.getExecutionContext(), "my-alert", AlertReferenceType.API);
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_when_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.empty());

        alertService.applyDefaults(executionContext, alert.getId(), alert.getReferenceType());
    }

    @Test(expected = AlertTemplateInvalidException.class)
    public void must_throw_AlertTemplateInvalidException_when_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));

        alertService.applyDefaults(executionContext, alert.getId(), alert.getReferenceType());
    }

    @Test
    public void must_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity(true);
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        alertTrigger.setReferenceType(AlertReferenceType.API.name());
        alertTrigger.setDefinition(objectMapper.writeValueAsString(alert));

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        when(apiRepository.searchIds(anyList(), any(Pageable.class), isNull())).thenReturn(
            new Page<>(List.of(UUID.randomUUID().toString()), 0, 1, 1)
        );

        alertService.applyDefaults(executionContext, alert.getId(), alert.getReferenceType());
    }
}
