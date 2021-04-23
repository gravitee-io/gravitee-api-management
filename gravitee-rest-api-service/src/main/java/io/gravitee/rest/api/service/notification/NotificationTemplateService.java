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

import freemarker.template.TemplateException;
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

    /**
     * call {@link #resolveInlineTemplateWithParam(String, String, Object, boolean)} with ignoreTplException set to true
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
    String resolveInlineTemplateWithParam(String name, String inlineTemplate, Object params, boolean ignoreTplException);

    /**
     * call {@link #resolveInlineTemplateWithParam(String, Reader, Object, boolean)} with ignoreTplException set to true
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
