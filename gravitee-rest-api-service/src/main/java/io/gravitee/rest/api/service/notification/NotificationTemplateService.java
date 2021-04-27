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
package io.gravitee.rest.api.service.notification;

import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateType;
import java.io.Reader;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface NotificationTemplateService {
    NotificationTemplateEntity create(NotificationTemplateEntity newNotificationTemplate);

    NotificationTemplateEntity update(NotificationTemplateEntity updatingNotificationTemplate);

    NotificationTemplateEntity findById(String id);

    Set<NotificationTemplateEntity> findAll();

    Set<NotificationTemplateEntity> findByType(NotificationTemplateType type);

    Set<NotificationTemplateEntity> findByHookAndScope(String hook, String scope);

    String resolveTemplateWithParam(String templateName, Object params);

    String resolveInlineTemplateWithParam(String name, String inlineTemplate, Object params);

    String resolveInlineTemplateWithParam(String name, Reader inlineTemplateReader, Object params);
}
