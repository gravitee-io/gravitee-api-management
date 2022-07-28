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
package io.gravitee.rest.api.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationActiveException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
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

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotRestoreApp_NotExist() throws TechnicalException {
        when(applicationRepository.findById(APP)).thenReturn(Optional.empty());

        applicationService.restore(APP);
    }

    @Test(expected = ApplicationActiveException.class)
    public void shouldNotRestoreApp_NotArchived() throws TechnicalException {
        Application app = fakeApp(false);
        app.setStatus(ApplicationStatus.ACTIVE);

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));

        applicationService.restore(APP);
    }

    @Test(expected = ClientIdAlreadyExistsException.class)
    public void shouldNotRestoreApp_ClientIdAlreadyExists() throws TechnicalException {
        Application appToRestore = fakeApp(false);
        appToRestore.setStatus(ApplicationStatus.ARCHIVED);
        appToRestore.setMetadata(Map.of(Application.METADATA_CLIENT_ID, CLIENT_ID));
        appToRestore.setEnvironmentId(ENVIRONMENT_ID);

        Application anotherApp = fakeApp(true);
        anotherApp.setMetadata(Map.of(Application.METADATA_CLIENT_ID, CLIENT_ID));

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(appToRestore));
        when(applicationRepository.findAllByEnvironment(ENVIRONMENT_ID, ApplicationStatus.ACTIVE)).thenReturn(Set.of(anotherApp));

        applicationService.restore(APP);
    }

    @Test
    public void shouldRestoreApp() throws TechnicalException {
        Application app = fakeApp(false);
        app.setStatus(ApplicationStatus.ARCHIVED);

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));
        when(applicationRepository.update(app)).thenReturn(fakeApp(true));
        when(subscriptionService.findByApplicationAndPlan(any(), any())).thenReturn(Collections.emptyList());
        when(userService.findById(any())).thenReturn(new UserEntity());

        ApplicationEntity result = applicationService.restore(APP);

        verify(membershipService, times(1))
            .deleteReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.APPLICATION,
                APP
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                any(),
                any(),
                any()
            );
        verify(genericNotificationConfigService, times(1)).deleteReference(NotificationReferenceType.APPLICATION, APP);
        verify(portalNotificationConfigService, times(1)).deleteReference(NotificationReferenceType.APPLICATION, APP);
        verify(auditService, times(1)).createApplicationAuditLog(any(), any(), any(), any(), any(), any());
        Assertions.assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACTIVE.name());
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
