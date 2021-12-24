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
package io.gravitee.rest.api.service;

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Collections;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_ArchiveTest {

    private static final String APPLICATION_ID = "id-app";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Application application;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private ApiKeyEntity apiKeyEntity;

    @Mock
    private AuditService auditService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApplicationAlertService applicationAlertService;

    @Test
    public void shouldArchive() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(subscriptionService.findByApplicationAndPlan(APPLICATION_ID, null)).thenReturn(Collections.singleton(subscription));
        when(subscription.getId()).thenReturn("sub");
        when(apiKeyService.findBySubscription("sub")).thenReturn(Collections.singletonList(apiKeyEntity));
        when(apiKeyEntity.getKey()).thenReturn("key");

        applicationService.archive(APPLICATION_ID);

        verify(apiKeyService, times(1)).delete("key");
        verify(membershipService, times(1))
            .deleteReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.APPLICATION,
                APPLICATION_ID
            );
        verify(subscriptionService, times(1)).close("sub");
        verify(application, times(1)).setStatus(ApplicationStatus.ARCHIVED);
        verify(applicationRepository, times(1)).update(application);
        verify(applicationAlertService, times(1)).deleteAll(APPLICATION_ID);
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotArchiveUnknownApp() throws Exception {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        applicationService.archive(APPLICATION_ID);
        Assert.fail("should not archive unknown app");
    }
}
