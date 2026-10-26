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

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationActiveException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadySubscribedException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ApplicationService_RestoreTest {

    private static final String APP = "my-app";

    private static final String CLIENT_ID = "myClientId";
    private static final String ENVIRONMENT_ID = "DEFAULT";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService clientCertificateCrudService;

    @BeforeAll
    public static void setup() {
        GraviteeContext.setCurrentEnvironment(GraviteeContext.getDefaultEnvironment());
    }

    @Test
    public void shouldNotRestoreApp_NotExist() throws TechnicalException {
        assertThrows(ApplicationNotFoundException.class, () -> {
            when(applicationRepository.findById(APP)).thenReturn(Optional.empty());

            applicationService.restore(GraviteeContext.getExecutionContext(), APP);
        });
    }

    @Test
    public void shouldNotRestoreApp_NotArchived() throws TechnicalException {
        assertThrows(ApplicationActiveException.class, () -> {
            Application app = fakeApp(false);
            app.setStatus(ApplicationStatus.ACTIVE);

            when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));

            applicationService.restore(GraviteeContext.getExecutionContext(), APP);
        });
    }

    @Test
    public void shouldNotRestoreApp_ClientIdAlreadyExists() throws TechnicalException {
        assertThrows(ClientIdAlreadyExistsException.class, () -> {
            Application appToRestore = fakeApp(false);
            appToRestore.setStatus(ApplicationStatus.ARCHIVED);
            appToRestore.setMetadata(Map.of(Application.METADATA_CLIENT_ID, CLIENT_ID));
            appToRestore.setEnvironmentId(ENVIRONMENT_ID);

            when(applicationRepository.findById(APP)).thenReturn(Optional.of(appToRestore));
            when(applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, CLIENT_ID, "DEFAULT")).thenReturn(true);

            applicationService.restore(GraviteeContext.getExecutionContext(), APP);
        });
    }

    @Test
    public void shouldRestoreApp() throws TechnicalException {
        Application app = fakeApp(false);
        app.setStatus(ApplicationStatus.ARCHIVED);

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));
        when(applicationRepository.update(app)).thenReturn(fakeApp(true));
        when(subscriptionService.findByApplicationAndPlan(eq(GraviteeContext.getExecutionContext()), any(), any())).thenReturn(
            Collections.emptyList()
        );
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        ApplicationEntity result = applicationService.restore(GraviteeContext.getExecutionContext(), APP);

        verify(membershipService, times(1)).deleteReference(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.APPLICATION,
            APP
        );
        verify(membershipService, times(1)).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(genericNotificationConfigService, times(1)).deleteReference(NotificationReferenceType.APPLICATION, APP);
        verify(portalNotificationConfigService, times(1)).deleteReference(NotificationReferenceType.APPLICATION, APP);
        verify(auditService, times(1)).createApplicationAuditLog(eq(GraviteeContext.getExecutionContext()), any(), any());
        Assertions.assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACTIVE.name());
    }

    @Test
    public void should_restore_the_most_recently_closed_subscription_of_a_plan_subscribed_again() throws TechnicalException {
        // The application closed its subscription to the plan, subscribed it again, then was archived: only one of the
        // two closed subscriptions can be active again, the one it had when archived.
        Application app = fakeApp(false);
        app.setStatus(ApplicationStatus.ARCHIVED);
        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));
        when(applicationRepository.update(app)).thenReturn(fakeApp(true));
        when(subscriptionService.findByApplicationAndPlan(eq(GraviteeContext.getExecutionContext()), any(), any())).thenReturn(
            List.of(closedSubscription("sub-closed-first", 1_000L), closedSubscription("sub-closed-on-archive", 2_000L))
        );
        when(subscriptionService.restore(GraviteeContext.getExecutionContext(), "sub-closed-first")).thenThrow(
            new PlanAlreadySubscribedException("plan-1")
        );
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        ApplicationEntity result = applicationService.restore(GraviteeContext.getExecutionContext(), APP);

        var restores = inOrder(subscriptionService);
        restores.verify(subscriptionService).restore(GraviteeContext.getExecutionContext(), "sub-closed-on-archive");
        restores.verify(subscriptionService).restore(GraviteeContext.getExecutionContext(), "sub-closed-first");
        Assertions.assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACTIVE.name());
    }

    private static SubscriptionEntity closedSubscription(String id, long closedAt) {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(id);
        subscription.setPlan("plan-1");
        subscription.setStatus(SubscriptionStatus.CLOSED);
        subscription.setClosedAt(new Date(closedAt));
        return subscription;
    }

    private Application fakeApp(boolean updated) {
        Application app = new Application();
        app.setId(APP);
        if (updated) {
            app.setStatus(ApplicationStatus.ACTIVE);
        }
        return app;
    }
}
