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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.mapper.IndexableApiMapper;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiNotificationServiceImplTest {

    @Mock
    private IndexableApiMapper indexableApiMapper;

    @Mock
    private NotifierService notifierService;

    @Mock
    private UserService userService;

    private ApiNotificationService apiNotificationService;

    @Before
    public void before() {
        apiNotificationService = new ApiNotificationServiceImpl(indexableApiMapper, notifierService, userService);

        when(indexableApiMapper.toGenericApi(any(), any())).thenReturn(new ApiEntity());
        when(userService.findById(any(), any())).thenReturn(new UserEntity());
    }

    @Test
    public void shouldNotNotifyUpdateWhenUserNotAuthenticated() {
        Api api = new Api();

        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(final Authentication authentication) {}
            }
        );
        apiNotificationService.triggerUpdateNotification(GraviteeContext.getExecutionContext(), api);
        verifyNoInteractions(notifierService);
    }

    @Test
    public void shouldNotNotifyDeprecatedWhenUserNotAuthenticated() {
        Api api = new Api();
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(final Authentication authentication) {}
            }
        );

        apiNotificationService.triggerUpdateNotification(GraviteeContext.getExecutionContext(), api);
        verifyNoInteractions(notifierService);
    }

    @Test
    public void shouldNotifyUpdateEvent() {
        UserEntity user = new UserEntity();
        user.setId("username");
        authenticateAs(user);

        Api api = new Api();
        api.setId("id");
        apiNotificationService.triggerUpdateNotification(GraviteeContext.getExecutionContext(), api);
        verify(notifierService).trigger(any(), eq(ApiHook.API_UPDATED), any(), any());
    }

    @Test
    public void shouldNotifyUpdateEventWithIndexableApi() {
        UserEntity user = new UserEntity();
        user.setId("username");
        authenticateAs(user);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("id");
        apiNotificationService.triggerUpdateNotification(GraviteeContext.getExecutionContext(), apiEntity);
        verify(notifierService).trigger(any(), eq(ApiHook.API_UPDATED), any(), any());
    }

    @Test
    public void shouldNotifyDeprecatedEvent() {
        UserEntity user = new UserEntity();
        user.setId("username");
        authenticateAs(user);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("id");
        apiNotificationService.triggerDeprecatedNotification(GraviteeContext.getExecutionContext(), apiEntity);
        verify(notifierService).trigger(any(), eq(ApiHook.API_DEPRECATED), any(), any());
    }
}
