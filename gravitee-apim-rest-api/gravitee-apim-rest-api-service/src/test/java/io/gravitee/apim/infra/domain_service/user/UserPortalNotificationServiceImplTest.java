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
package io.gravitee.apim.infra.domain_service.user;

import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserPortalNotificationServiceImplTest {

    NotifierService notifierService = mock(NotifierService.class);
    UserPortalNotificationServiceImpl service = new UserPortalNotificationServiceImpl(notifierService);

    @Nested
    class TriggerUserRegistered {

        @Test
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void should_trigger_user_registered_portal_hook() {
            var executionContext = new ExecutionContext("org-id", "env-id");
            var user = aBaseUserEntity();

            service.triggerUserRegistered(executionContext, user);

            ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notifierService).trigger(eq(executionContext), eq(PortalHook.USER_REGISTERED), paramsCaptor.capture());
            var capturedUser = (UserEntity) paramsCaptor.getValue().get("user");
            assertThat(capturedUser.getEmail()).isEqualTo("jane.doe@gravitee.io");
        }
    }
}
