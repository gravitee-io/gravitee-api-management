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
package io.gravitee.management.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.AlertEventQuery;
import io.gravitee.management.model.alert.*;

import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AlertService {
    AlertTriggerEntity create(NewAlertTriggerEntity alert);
    AlertTriggerEntity update(UpdateAlertTriggerEntity alert);
    List<AlertTriggerEntity> findByReference(AlertReferenceType referenceType, String referenceId);
    void delete(String alertId, String referenceId);
    AlertStatusEntity getStatus();
    Page<AlertEventEntity> findEvents(String alertId, AlertEventQuery eventQuery);

    void createDefaults(AlertReferenceType referenceType, String referenceId);

    void applyDefaults(String alertId, AlertReferenceType referenceType);
}
