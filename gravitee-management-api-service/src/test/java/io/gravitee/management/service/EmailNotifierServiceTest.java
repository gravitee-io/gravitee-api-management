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

import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.PrimaryOwnerEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.ApplicationHook;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.notification.PortalHook;
import io.gravitee.management.service.notifiers.impl.EmailNotifierServiceImpl;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailNotifierServiceTest {

    @InjectMocks
    private EmailNotifierServiceImpl service = new EmailNotifierServiceImpl();

    @Mock
    private EmailService mockEmailService;

    @Test
    public void shouldNotSendEmailIfNoConfig() {
        service.trigger(ApiHook.API_STARTED, null, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());

        service.trigger(ApiHook.API_STARTED, new GenericNotificationConfig(), null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());

        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("");
        service.trigger(ApiHook.API_STARTED, cfg, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());
    }

    @Test
    public void shouldNotSendEmailIfNoHook() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        service.trigger(null, cfg, null);
        verify(mockEmailService, never()).sendAsyncEmailNotification(any());
        verify(mockEmailService, never()).sendEmailNotification(any());
    }

    @Test
    public void shouldHaveATemplateForApiHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        PlanEntity plan = new PlanEntity();
        plan.setId("plan-12345");
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApiHook hook : ApiHook.values()) {
            if (!ApiHook.MESSAGE.equals(hook)) {
                reset(mockEmailService);
                service.trigger(hook, cfg, params);
                verify(mockEmailService, times(1)).sendAsyncEmailNotification(argThat(notification ->
                        notification.getSubject() != null
                                && !notification.getSubject().isEmpty()
                                && notification.getTo() != null
                                && notification.getTo().length == 1
                                && notification.getTo()[0].equals("test@mail.com")
                ));
                verify(mockEmailService, never()).sendEmailNotification(any());
            }
        }
    }

    @Test
    public void shouldHaveATemplateForApplicationHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        PlanEntity plan = new PlanEntity();
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApplicationHook hook : ApplicationHook.values()) {
            reset(mockEmailService);
            service.trigger(hook, cfg, params);
            verify(mockEmailService, times(1)).sendAsyncEmailNotification(argThat(notification ->
                    notification.getSubject() != null
                            && !notification.getSubject().isEmpty()
                            && notification.getTo() != null
                            && notification.getTo().length == 1
                            && notification.getTo()[0].equals("test@mail.com")
            ));
            verify(mockEmailService, never()).sendEmailNotification(any());
        }
    }

    @Test
    public void shouldHaveATemplateForPortalHooks() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com");
        for (PortalHook hook : PortalHook.values()) {
            if (!PortalHook.MESSAGE.equals(hook) && !PortalHook.GROUP_INVITATION.equals(hook)) {
                reset(mockEmailService);
                service.trigger(hook, cfg, Collections.emptyMap());
                verify(mockEmailService, times(1)).sendAsyncEmailNotification(argThat(notification ->
                        notification.getSubject() != null
                                && !notification.getSubject().isEmpty()
                                && notification.getTo() != null
                                && notification.getTo().length == 1
                                && notification.getTo()[0].equals("test@mail.com")
                ));
                verify(mockEmailService, never()).sendEmailNotification(any());
            }
        }
    }


    @Test
    public void shouldHaveATemplateForApplicationHooksWithFreemarker() {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setConfig("test@mail.com, ${api.primaryOwner.email} ");
        ApiEntity api = new ApiEntity();
        api.setName("api-name");
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("primary@owner.com");
        api.setPrimaryOwner(new PrimaryOwnerEntity(userEntity));
        PlanEntity plan = new PlanEntity();
        plan.setId("plan-id");
        plan.setName("plan-name");
        Map<String, Object> params = new HashMap<>();
        params.put((NotificationParamsBuilder.PARAM_API), api);
        params.put((NotificationParamsBuilder.PARAM_PLAN), plan);
        for (ApplicationHook hook : ApplicationHook.values()) {
            reset(mockEmailService);
            service.trigger(hook, cfg, params);
            verify(mockEmailService, times(1)).sendAsyncEmailNotification(argThat(notification ->
                    notification.getSubject() != null
                            && !notification.getSubject().isEmpty()
                            && notification.getTo() != null
                            && notification.getTo().length == 2
                            && notification.getTo()[0].equals("test@mail.com")
                            && notification.getTo()[1].equals("primary@owner.com")
            ));
            verify(mockEmailService, never()).sendEmailNotification(any());
        }
    }
}
