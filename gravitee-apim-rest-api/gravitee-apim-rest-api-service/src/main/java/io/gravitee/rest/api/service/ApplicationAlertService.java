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
package io.gravitee.rest.api.service;

import io.gravitee.common.event.Event;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

public interface ApplicationAlertService {
    AlertTriggerEntity create(ExecutionContext executionContext, String applicationId, NewAlertTriggerEntity alert);

    List<AlertTriggerEntity> findByApplication(String applicationId);

    AlertTriggerEntity update(ExecutionContext executionContext, String applicationId, UpdateAlertTriggerEntity alert);

    void delete(String alertId, String applicationId);

    AlertStatusEntity getStatus(ExecutionContext executionContext);

    void addMemberToApplication(ExecutionContext executionContext, String applicationId, String email);

    void deleteMemberFromApplication(ExecutionContext executionContext, String applicationId, String email);

    void deleteAll(String applicationId);

    void handleEvent(Event<ApplicationAlertEventType, Object> event);
}
