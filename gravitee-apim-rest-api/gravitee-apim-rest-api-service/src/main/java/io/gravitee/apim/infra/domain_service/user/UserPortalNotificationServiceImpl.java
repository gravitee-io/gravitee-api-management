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

import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.service_provider.UserPortalNotificationService;
import io.gravitee.apim.infra.adapter.UserAdapter;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserPortalNotificationServiceImpl implements UserPortalNotificationService {

    private final NotifierService notifierService;

    public UserPortalNotificationServiceImpl(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @Override
    public void triggerUserRegistered(ExecutionContext executionContext, BaseUserEntity user) {
        notifierService.trigger(executionContext, PortalHook.USER_REGISTERED, Map.of("user", UserAdapter.INSTANCE.toUserEntity(user)));
    }
}
