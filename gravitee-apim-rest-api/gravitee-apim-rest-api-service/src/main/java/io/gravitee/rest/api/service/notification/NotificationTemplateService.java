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
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface NotificationTemplateService {
    NotificationTemplateEntity create(ExecutionContext executionContext, NotificationTemplateEntity newNotificationTemplate);

    NotificationTemplateEntity update(ExecutionContext executionContext, NotificationTemplateEntity updatingNotificationTemplate);

    NotificationTemplateEntity findById(String id);

    Set<NotificationTemplateEntity> findAll(String organizationId);

    Set<NotificationTemplateEntity> findByType(String organizationId, NotificationTemplateType type);

    Set<NotificationTemplateEntity> findByHookAndScope(String organizationId, String hook, String scope);

    String resolveTemplateWithParam(String organizationId, String templateName, Object params);

    /**
     * call {@link #resolveInlineTemplateWithParam(String, String, Object)} with ignoreTplException set to true
     * @param name
     * @param inlineTemplate
     * @param params
     * @return
     */
    default String resolveInlineTemplateWithParam(String name, String inlineTemplate, Object params) {
        return resolveInlineTemplateWithParam(name, inlineTemplate, params, true);
    }

    /**
     *
     * @param name
     * @param inlineTemplate
     * @param params
     * @param ignoreTplException if true, this method return empty string incase of TemplateException, otherwise a {@link io.gravitee.rest.api.service.exceptions.TemplateProcessingException} is thrown
     * @return
     */
    default String resolveInlineTemplateWithParam(String name, String inlineTemplate, Object params, boolean ignoreTplException) {
        return resolveInlineTemplateWithParam(name, new StringReader(inlineTemplate), params, ignoreTplException);
    }

    /**
     * call {@link #resolveInlineTemplateWithParam(String, Reader, Object)} with ignoreTplException set to true
     * @param name
     * @param inlineTemplateReader
     * @param params
     * @return
     */
    default String resolveInlineTemplateWithParam(String name, Reader inlineTemplateReader, Object params) {
        return resolveInlineTemplateWithParam(name, inlineTemplateReader, params, true);
    }

    /**
     *
     * @param name
     * @param inlineTemplateReader
     * @param params
     * @param ignoreTplException if true, this method return empty string incase of TemplateException, otherwise a {@link io.gravitee.rest.api.service.exceptions.TemplateProcessingException} is thrown
     * @return
     */
    String resolveInlineTemplateWithParam(String name, Reader inlineTemplateReader, Object params, boolean ignoreTplException);
}
