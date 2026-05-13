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

import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.notification.ApiProductHook;
import io.gravitee.rest.api.service.notification.ApiProductTemplateModel;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiProductNotificationService;
import java.util.NoSuchElementException;
import lombok.CustomLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends API Product update notifications when a user triggers the change: only if the security context has a
 * non-null, non-system authenticated user. Notification params include that user as {@code user} and an
 * {@link ApiProductTemplateModel} whose {@link ApiProductTemplateModel#primaryOwner} is filled when the API Product
 * primary owner can be resolved from membership and user lookup.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ApiProductNotificationServiceImpl extends AbstractService implements ApiProductNotificationService {

    private final NotifierService notifierService;
    private final MembershipService membershipService;
    private final UserService userService;

    public ApiProductNotificationServiceImpl(
        final NotifierService notifierService,
        final MembershipService membershipService,
        final UserService userService
    ) {
        this.notifierService = notifierService;
        this.membershipService = membershipService;
        this.userService = userService;
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerUpdateNotification(final ExecutionContext executionContext, final ApiProduct apiProduct) {
        triggerNotification(executionContext, ApiProductHook.API_PRODUCT_UPDATED, apiProduct);
    }

    @Override
    @Async("asyncNotificationThreadPoolTaskExecutor")
    public void triggerDeployNotification(final ExecutionContext executionContext, final ApiProduct apiProduct) {
        triggerNotification(executionContext, ApiProductHook.API_PRODUCT_DEPLOYED, apiProduct);
    }

    private void triggerNotification(final ExecutionContext executionContext, final ApiProductHook hook, final ApiProduct apiProduct) {
        UserDetails auth = getAuthenticatedUser();
        if (auth != null && !auth.isSystem()) {
            UserEntity userEntity = userService.findById(executionContext, auth.getUsername());
            PrimaryOwnerEntity primaryOwner = resolvePrimaryOwner(executionContext, apiProduct);
            ApiProductTemplateModel model = ApiProductTemplateModel.builder()
                .id(apiProduct.getId())
                .name(apiProduct.getName())
                .version(apiProduct.getVersion())
                .primaryOwner(primaryOwner)
                .build();
            notifierService.trigger(
                executionContext,
                hook,
                apiProduct.getId(),
                new NotificationParamsBuilder().apiProduct(model).user(userEntity).build()
            );
        }
    }

    private PrimaryOwnerEntity resolvePrimaryOwner(final ExecutionContext executionContext, final ApiProduct apiProduct) {
        try {
            String ownerUserId = membershipService.getPrimaryOwnerUserId(
                executionContext.getOrganizationId(),
                MembershipReferenceType.API_PRODUCT,
                apiProduct.getId()
            );
            if (ownerUserId != null) {
                return new PrimaryOwnerEntity(userService.findById(executionContext, ownerUserId));
            }
        } catch (NoSuchElementException e) {
            log.debug("Could not resolve primary owner for API Product {} notification", apiProduct.getId(), e);
        }
        return null;
    }
}
