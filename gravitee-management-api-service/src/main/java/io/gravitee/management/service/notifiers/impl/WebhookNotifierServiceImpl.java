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
package io.gravitee.management.service.notifiers.impl;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.notification.Hook;
import io.gravitee.management.service.notifiers.WebNotifierService;
import io.gravitee.management.service.notifiers.WebhookNotifierService;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.gravitee.management.service.notification.NotificationParamsBuilder.*;

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

        webNotifierService.request(HttpMethod.POST, genericNotificationConfig.getConfig(), headers, body, genericNotificationConfig.isUseSystemProxy());
    }

    private String toJson(final Hook hook, final Map<String, Object> params) {
        JsonObject content = new JsonObject();
        //hook
        content.put("event", hook.name());
        content.put("scope", hook.getScope().name());
        //api
        if (params.containsKey(PARAM_API)) {
            Object api = params.get(PARAM_API);
            if (api != null) {
                JsonObject jsonApi = new JsonObject();
                jsonApi.put("id", api instanceof ApiModelEntity ? ((ApiModelEntity) api).getId() : ((ApiEntity) api).getId());
                jsonApi.put("name", api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName());
                jsonApi.put("version", api instanceof ApiModelEntity ? ((ApiModelEntity) api).getVersion() : ((ApiEntity) api).getVersion());
                content.put("api", jsonApi);
            }
        }
        // application
        if (params.containsKey(PARAM_APPLICATION)) {
            ApplicationEntity application = (ApplicationEntity )params.get(PARAM_APPLICATION);
            if (application != null) {
                JsonObject jsonApplication = new JsonObject();
                jsonApplication.put("id", application.getId());
                jsonApplication.put("name", application.getName());
                /*
                if (application.getType() != null) {
                    jsonApplication.put("type", application.getType());
                }
                */
                content.put("application", jsonApplication);
            }
        }
        // owner
        if (params.containsKey(PARAM_OWNER)) {
            PrimaryOwnerEntity owner = (PrimaryOwnerEntity) params.get(PARAM_OWNER);
            if (owner != null) {
                JsonObject jsonOwner = new JsonObject();
                jsonOwner.put("id", owner.getId());
                jsonOwner.put("username", owner.getDisplayName());
                content.put("owner", jsonOwner);
            }
        }
        // plan
        if (params.containsKey(PARAM_PLAN)) {
            PlanEntity plan = (PlanEntity)params.get(PARAM_PLAN);
            if (plan != null) {
                JsonObject jsonPlan = new JsonObject();
                jsonPlan.put("id", plan.getId());
                jsonPlan.put("name", plan.getName());
                jsonPlan.put("security", plan.getSecurity());
                content.put("plan", jsonPlan);
            }
        }
        // subscription
        if (params.containsKey(PARAM_SUBSCRIPTION)) {
            SubscriptionEntity subscription = (SubscriptionEntity) params.get(PARAM_SUBSCRIPTION);
            if (subscription != null) {
                JsonObject jsonSubscription = new JsonObject();
                jsonSubscription.put("id", subscription.getId());
                jsonSubscription.put("status", subscription.getStatus());
                content.put("subscription", jsonSubscription);
            }
        }

        return content.encode();
    }
}
