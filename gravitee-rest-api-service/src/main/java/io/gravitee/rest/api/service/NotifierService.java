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

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.PortalHook;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface NotifierService {
     void trigger(final ApiHook hook, final String apiId, Map<String, Object> params);
     void trigger(final ApplicationHook hook, final String applicationId, Map<String, Object> params);
     void trigger(final PortalHook hook, Map<String, Object> params);
     List<NotifierEntity> list(NotificationReferenceType referenceType, String referenceId);
     Set<NotifierEntity> findAll();
     NotifierEntity findById(String notifier);
     String getSchema(String notifier);
}
