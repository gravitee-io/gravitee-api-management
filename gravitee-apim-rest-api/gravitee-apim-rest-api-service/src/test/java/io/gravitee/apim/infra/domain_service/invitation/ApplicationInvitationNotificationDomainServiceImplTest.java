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

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.APPLICATION_INVITATION;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_APPLICATION;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_REGISTRATION_URL;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationInvitationNotificationDomainServiceImplTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String ROLE_NAME = "USER";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
    private static final String PARAM_ROLE = "role";
    private static final URI CONFIRMATION_PAGE_URL = URI.create("https://portal.example.com/user/registration/confirm");

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    private ApplicationInvitationNotificationDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new ApplicationInvitationNotificationDomainServiceImpl(applicationCrudService, userService, emailService);
    }

    @Test
    void should_dispatch_application_invitation_email_for_each_invitation() {
        var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        var application = BaseApplicationEntity.builder().id(APPLICATION_ID).name("My Application").environmentId(ENVIRONMENT_ID).build();
        var invitations = List.of(
            anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE_NAME),
            anApplicationInvitation(INVITATION_ID_2, APPLICATION_ID, "bob@example.com", ROLE_NAME)
        );
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(application);
        when(
            userService.getTokenRegistrationParams(
                eq(executionContext),
                any(UserEntity.class),
                eq(REGISTRATION_PATH),
                eq(APPLICATION_INVITATION),
                eq(CONFIRMATION_PAGE_URL.toString())
            )
        ).thenAnswer(invocation -> {
            var user = invocation.getArgument(1, UserEntity.class);
            return Map.of(PARAM_REGISTRATION_URL, "https://example.com/registration/" + user.getEmail());
        });

        cut.dispatchAsync(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, invitations, CONFIRMATION_PAGE_URL);

        verify(applicationCrudService).findById(APPLICATION_ID, ENVIRONMENT_ID);
        verify(userService, times(2)).getTokenRegistrationParams(
            eq(executionContext),
            any(UserEntity.class),
            eq(REGISTRATION_PATH),
            eq(APPLICATION_INVITATION),
            eq(CONFIRMATION_PAGE_URL.toString())
        );
        var notificationCaptor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailService, times(2)).sendAsyncEmailNotification(eq(executionContext), notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
            .extracting(notification -> notification.getTo()[0])
            .containsExactly("alice@example.com", "bob@example.com");
        assertThat(notificationCaptor.getAllValues())
            .extracting(EmailNotification::getTemplate)
            .containsOnly(
                EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_APPLICATION_INVITATION.getLinkedHook().getTemplate()
            );
        assertThat(notificationCaptor.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.getParams()).containsEntry(PARAM_APPLICATION, application).containsEntry(PARAM_ROLE, ROLE_NAME);
            assertThat(notification.getParams()).containsKey(PARAM_REGISTRATION_URL);
        });
    }

    @Test
    void should_do_nothing_when_invitations_are_empty() {
        cut.dispatchAsync(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, List.of(), null);

        verifyNoInteractions(applicationCrudService, userService, emailService);
    }

    @Test
    void should_do_nothing_when_invitations_are_null() {
        cut.dispatchAsync(ORGANIZATION_ID, ENVIRONMENT_ID, APPLICATION_ID, null, null);

        verifyNoInteractions(applicationCrudService, userService, emailService);
    }
}
