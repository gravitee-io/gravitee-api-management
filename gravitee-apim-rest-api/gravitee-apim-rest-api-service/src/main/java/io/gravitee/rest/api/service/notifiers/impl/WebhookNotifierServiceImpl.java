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
package io.gravitee.rest.api.service.notifiers.impl;

import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.*;

import io.gravitee.apim.core.notification.model.*;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class WebhookNotifierServiceImpl implements WebhookNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(WebhookNotifierServiceImpl.class);

    @Autowired
    WebNotifierService webNotifierService;

    @Override
    public void trigger(final Hook hook, GenericNotificationConfig genericNotificationConfig, final Map<String, Object> params) {
        //body
        String body = toJson(hook, params);

        //headers
        Map<String, String> headers = new HashMap();
        headers.put("X-Gravitee-Event", hook.name());
        headers.put("X-Gravitee-Event-Scope", hook.getScope().name());

        webNotifierService.request(
            HttpMethod.POST,
            genericNotificationConfig.getConfig(),
            headers,
            body,
            genericNotificationConfig.isUseSystemProxy()
        );
    }

    private String toJson(final Hook hook, final Map<String, Object> params) {
        JsonObject content = new JsonObject();
        //hook
        content.put("event", hook.name());
        content.put("scope", hook.getScope().name());

        // Generalized method to populate JSON objects
        addJsonObject(params, PARAM_API, content, "api", GenericApiModel.class, GenericApiEntity.class, ApiNotificationTemplateData.class);

        addJsonObject(
            params,
            PARAM_APPLICATION,
            content,
            "application",
            ApplicationEntity.class,
            ApplicationNotificationTemplateData.class
        );

        addJsonObject(params, PARAM_OWNER, content, "owner", PrimaryOwnerEntity.class, PrimaryOwnerNotificationTemplateData.class);

        addJsonObject(params, PARAM_PLAN, content, "plan", PlanEntity.class, PlanNotificationTemplateData.class);

        addJsonObject(
            params,
            PARAM_SUBSCRIPTION,
            content,
            "subscription",
            SubscriptionEntity.class,
            SubscriptionNotificationTemplateData.class
        );

        return content.encode();
    }

    private void addJsonObject(Map<String, Object> params, String paramKey, JsonObject content, String jsonKey, Class<?>... dataTypes) {
        if (params.containsKey(paramKey)) {
            Object object = params.get(paramKey);
            JsonObject jsonObject = new JsonObject();

            for (Class<?> dataType : dataTypes) {
                if (dataType.isInstance(object)) {
                    populateJson(jsonObject, object, dataType);
                    content.put(jsonKey, jsonObject);
                    return;
                }
            }
            throw new IllegalArgumentException("Unsupported type for " + jsonKey + ": " + object.getClass().getName());
        }
    }

    private void populateJson(JsonObject jsonObject, Object object, Class<?> dataType) {
        if (dataType == GenericApiModel.class) {
            GenericApiModel model = (GenericApiModel) object;
            jsonObject.put("id", model.getId());
            jsonObject.put("name", model.getName());
            jsonObject.put("version", model.getApiVersion());
        } else if (dataType == GenericApiEntity.class) {
            GenericApiEntity entity = (GenericApiEntity) object;
            jsonObject.put("id", entity.getId());
            jsonObject.put("name", entity.getName());
            jsonObject.put("version", entity.getApiVersion());
        } else if (dataType == ApiNotificationTemplateData.class) {
            ApiNotificationTemplateData data = (ApiNotificationTemplateData) object;
            jsonObject.put("id", data.getId());
            jsonObject.put("name", data.getName());
            jsonObject.put("version", data.getVersion());
        } else if (dataType == ApplicationEntity.class) {
            ApplicationEntity application = (ApplicationEntity) object;
            jsonObject.put("id", application.getId());
            jsonObject.put("name", application.getName());
        } else if (dataType == ApplicationNotificationTemplateData.class) {
            ApplicationNotificationTemplateData notificationData = (ApplicationNotificationTemplateData) object;
            jsonObject.put("id", notificationData.getId());
            jsonObject.put("name", notificationData.getName());
        } else if (dataType == PrimaryOwnerEntity.class) {
            PrimaryOwnerEntity owner = (PrimaryOwnerEntity) object;
            jsonObject.put("id", owner.getId());
            jsonObject.put("username", owner.getDisplayName());
        } else if (dataType == PrimaryOwnerNotificationTemplateData.class) {
            PrimaryOwnerNotificationTemplateData notificationData = (PrimaryOwnerNotificationTemplateData) object;
            jsonObject.put("id", notificationData.getId());
            jsonObject.put("username", notificationData.getDisplayName());
        } else if (dataType == PlanEntity.class) {
            PlanEntity plan = (PlanEntity) object;
            jsonObject.put("id", plan.getId());
            jsonObject.put("name", plan.getName());
            jsonObject.put("security", plan.getSecurity());
        } else if (dataType == PlanNotificationTemplateData.class) {
            PlanNotificationTemplateData notificationData = (PlanNotificationTemplateData) object;
            jsonObject.put("id", notificationData.getId());
            jsonObject.put("name", notificationData.getName());
            jsonObject.put("security", notificationData.getSecurity());
        } else if (dataType == SubscriptionEntity.class) {
            SubscriptionEntity subscription = (SubscriptionEntity) object;
            jsonObject.put("id", subscription.getId());
            jsonObject.put("status", subscription.getStatus());
        } else if (dataType == SubscriptionNotificationTemplateData.class) {
            SubscriptionNotificationTemplateData notificationData = (SubscriptionNotificationTemplateData) object;
            jsonObject.put("id", notificationData.getId());
            jsonObject.put("status", notificationData.getStatus());
        }
    }
}
