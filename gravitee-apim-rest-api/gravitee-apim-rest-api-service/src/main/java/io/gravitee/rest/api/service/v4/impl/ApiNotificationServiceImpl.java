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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiNotificationServiceImpl extends AbstractService implements ApiNotificationService {

    private final GenericApiMapper indexableApiMapper;
    private final NotifierService notifierService;
    private final UserService userService;

    public ApiNotificationServiceImpl(
        final GenericApiMapper indexableApiMapper,
        final NotifierService notifierService,
        final UserService userService
    ) {
        this.indexableApiMapper = indexableApiMapper;
        this.notifierService = notifierService;
        this.userService = userService;
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerUpdateNotification(final ExecutionContext executionContext, final Api api) {
        GenericApiEntity indexableApi = indexableApiMapper.toGenericApi(api, null);
        triggerUpdateNotification(executionContext, indexableApi);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerUpdateNotification(final ExecutionContext executionContext, final GenericApiEntity indexableApi) {
        triggerNotification(executionContext, ApiHook.API_UPDATED, indexableApi);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerDeprecatedNotification(final ExecutionContext executionContext, final GenericApiEntity indexableApi) {
        triggerNotification(executionContext, ApiHook.API_DEPRECATED, indexableApi);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerDeployNotification(final ExecutionContext executionContext, final GenericApiEntity indexableApi) {
        triggerNotification(executionContext, ApiHook.API_DEPLOYED, indexableApi);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerStartNotification(final ExecutionContext executionContext, final GenericApiEntity indexableApi) {
        triggerNotification(executionContext, ApiHook.API_STARTED, indexableApi);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerStopNotification(final ExecutionContext executionContext, final GenericApiEntity indexableApi) {
        triggerNotification(executionContext, ApiHook.API_STOPPED, indexableApi);
    }

    private void triggerNotification(final ExecutionContext executionContext, final ApiHook hook, final GenericApiEntity genericApiEntity) {
        String userId = getAuthenticatedUsername();
        if (userId != null && !getAuthenticatedUser().isSystem()) {
            UserEntity userEntity = userService.findById(executionContext, userId);
            notifierService.trigger(
                executionContext,
                hook,
                genericApiEntity.getId(),
                new NotificationParamsBuilder().api(genericApiEntity).user(userEntity).build()
            );
        }
    }
}
