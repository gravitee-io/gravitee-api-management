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
package io.gravitee.apim.infra.domain_service.invitation;

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.APPLICATION_INVITATION;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_APPLICATION;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.invitation.domain_service.ApplicationInvitationNotificationDomainService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationInvitationNotificationDomainServiceImpl implements ApplicationInvitationNotificationDomainService {

    private static final String PARAM_ROLE = "role";

    private final ApplicationCrudService applicationCrudService;
    private final UserService userService;
    private final EmailService emailService;

    @Override
    public void dispatchAsync(
        String organizationId,
        String environmentId,
        String applicationId,
        List<ApplicationInvitation> invitations,
        URI confirmationPageUrl
    ) {
        if (invitations == null || invitations.isEmpty()) {
            return;
        }

        var executionContext = new ExecutionContext(organizationId, environmentId);
        var application = applicationCrudService.findById(applicationId, environmentId);

        invitations.forEach(invitation -> {
            var user = new UserEntity();
            user.setEmail(invitation.email());

            var params = new HashMap<>(
                userService.getTokenRegistrationParams(
                    executionContext,
                    user,
                    REGISTRATION_PATH,
                    APPLICATION_INVITATION,
                    confirmationPageUrl == null ? null : confirmationPageUrl.toString()
                )
            );
            params.put(PARAM_APPLICATION, application);
            params.put(PARAM_ROLE, invitation.roleName());

            emailService.sendAsyncEmailNotification(
                executionContext,
                new EmailNotificationBuilder()
                    .to(invitation.email())
                    .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_APPLICATION_INVITATION)
                    .params(params)
                    .build()
            );
        });
    }
}
