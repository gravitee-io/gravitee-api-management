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

import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.notification.model.*;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.gravitee.rest.api.service.notifiers.impl.WebhookNotifierServiceImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WebhookNotifierServiceImplTest {

    @Mock
    private WebNotifierService webNotifierService;

    @InjectMocks
    private WebhookNotifierServiceImpl webhookNotifierService;

    @Test
    public void shouldCallWebNotifierServiceWithCreatedJsonFromParamsWithNotificationData() {
        Hook hook = ApiHook.SUBSCRIPTION_NEW;

        GenericNotificationConfig genericNotificationConfig = new GenericNotificationConfig();
        genericNotificationConfig.setId("123");
        genericNotificationConfig.setName("Default Webhook");
        genericNotificationConfig.setNotifier("default-webhook");
        genericNotificationConfig.setConfig("http://example.com/webhook");

        Map<String, Object> params = new HashMap<>();

        ApiNotificationTemplateData apiNotificationTemplateData = ApiNotificationTemplateData
            .builder()
            .id("apiNotifId")
            .name("apiNotifName")
            .apiVersion("apiNotifVersion")
            .build();
        params.put("api", apiNotificationTemplateData);

        ApplicationNotificationTemplateData applicationNotificationTemplateData = ApplicationNotificationTemplateData
            .builder()
            .id("appNotifTempDataId")
            .name("appNotifTempName")
            .build();
        params.put("application", applicationNotificationTemplateData);

        PrimaryOwnerNotificationTemplateData primaryOwnerNotificationTemplateData = PrimaryOwnerNotificationTemplateData
            .builder()
            .id("primaryId")
            .displayName("displayNamePrimary")
            .build();
        params.put("owner", primaryOwnerNotificationTemplateData);

        PlanNotificationTemplateData planNotificationTemplateData = PlanNotificationTemplateData
            .builder()
            .id("planNotifId")
            .name("planNotifName")
            .security("API_KEY")
            .build();
        params.put("plan", planNotificationTemplateData);

        SubscriptionNotificationTemplateData subscriptionNotificationTemplateData = SubscriptionNotificationTemplateData
            .builder()
            .id("subsNotifId")
            .reason("subsNotifReason")
            .status("PENDING")
            .build();
        params.put("subscription", subscriptionNotificationTemplateData);

        webhookNotifierService.trigger(hook, genericNotificationConfig, params);

        // toJson method should make JSON body from params
        String expectedBody =
            "{\"event\":\"SUBSCRIPTION_NEW\",\"scope\":\"API\",\"api\":{\"id\":\"apiNotifId\",\"name\":\"apiNotifName\",\"version\":\"apiNotifVersion\"}," +
            "\"application\":{\"id\":\"appNotifTempDataId\",\"name\":\"appNotifTempName\"}," +
            "\"owner\":{\"id\":\"primaryId\",\"username\":\"displayNamePrimary\"}," +
            "\"plan\":{\"id\":\"planNotifId\",\"name\":\"planNotifName\",\"security\":\"API_KEY\"}," +
            "\"subscription\":{\"id\":\"subsNotifId\",\"status\":\"PENDING\"}}";

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Gravitee-Event", hook.name());
        expectedHeaders.put("X-Gravitee-Event-Scope", "API");

        verify(webNotifierService).request(HttpMethod.POST, "http://example.com/webhook", expectedHeaders, expectedBody, false);
    }

    @Test
    public void shouldCallWebNotifierServiceWithCreatedJsonFromParamsWithEntityData() {
        Hook hook = ApiHook.SUBSCRIPTION_NEW;

        GenericNotificationConfig genericNotificationConfig = new GenericNotificationConfig();
        genericNotificationConfig.setId("123");
        genericNotificationConfig.setName("Default Webhook");
        genericNotificationConfig.setNotifier("default-webhook");
        genericNotificationConfig.setConfig("http://example.com/webhook");

        Map<String, Object> params = new HashMap<>();

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("apiEntityId");
        apiEntity.setName("apiEntityName");
        apiEntity.setApiVersion("apiEntityVersion");
        params.put("api", apiEntity);

        ApplicationEntity applicationEntity = ApplicationEntity.builder().id("appId").name("appName").build();
        params.put("application", applicationEntity);

        PrimaryOwnerEntity primaryOwner = PrimaryOwnerEntity.builder().id("ownerId").displayName("displayName").build();
        params.put("owner", primaryOwner);

        PlanEntity plan = PlanEntity.builder().id("planId").name("planName").security(PlanSecurityType.API_KEY).build();
        params.put("plan", plan);

        SubscriptionEntity subscription = SubscriptionEntity.builder().id("subsId").status(SubscriptionStatus.ACCEPTED).build();
        params.put("subscription", subscription);

        webhookNotifierService.trigger(hook, genericNotificationConfig, params);

        // toJson method should make JSON body from params
        String expectedBody =
            "{\"event\":\"SUBSCRIPTION_NEW\",\"scope\":\"API\"," +
            "\"api\":{\"id\":\"apiEntityId\",\"name\":\"apiEntityName\",\"version\":\"apiEntityVersion\"}," +
            "\"application\":{\"id\":\"appId\",\"name\":\"appName\"}," +
            "\"owner\":{\"id\":\"ownerId\",\"username\":\"displayName\"}," +
            "\"plan\":{\"id\":\"planId\",\"name\":\"planName\",\"security\":\"API_KEY\"}," +
            "\"subscription\":{\"id\":\"subsId\",\"status\":\"ACCEPTED\"}}";

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Gravitee-Event", hook.name());
        expectedHeaders.put("X-Gravitee-Event-Scope", "API");

        verify(webNotifierService).request(HttpMethod.POST, "http://example.com/webhook", expectedHeaders, expectedBody, false);
    }
}
