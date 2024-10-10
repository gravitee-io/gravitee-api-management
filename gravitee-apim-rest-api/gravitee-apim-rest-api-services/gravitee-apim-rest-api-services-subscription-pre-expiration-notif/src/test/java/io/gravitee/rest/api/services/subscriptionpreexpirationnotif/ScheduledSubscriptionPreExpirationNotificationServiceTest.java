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
package io.gravitee.rest.api.services.subscriptionpreexpirationnotif;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledSubscriptionPreExpirationNotificationServiceTest {

    @InjectMocks
    ScheduledSubscriptionPreExpirationNotificationService service = new ScheduledSubscriptionPreExpirationNotificationService();

    @Mock
    UserService userService;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    ApiKeyService apiKeyService;

    @Mock
    EmailService emailService;

    @Test
    public void shouldCleanNotificationDays() {
        List<Integer> inputNotificationDays = Arrays.asList(-1, 150, 75, 10, 30, 400, 45);
        List<Integer> cleanedNotificationDays = service.getCleanedNotificationDays(inputNotificationDays);

        assertEquals(Arrays.asList(150, 75, 45, 30, 10), cleanedNotificationDays);
    }

    @Test
    public void shouldFindSubscriptionExpirationsToNotify() {
        Instant now = Instant.ofEpochMilli(1469022010000L);
        Integer daysBeforeNotification = 10;

        SubscriptionEntity subscription = mock(SubscriptionEntity.class);

        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(SubscriptionQuery.class)))
            .thenReturn(Collections.singletonList(subscription));

        Collection<SubscriptionEntity> subscriptionsToNotify = service.findSubscriptionExpirationsToNotify(now, daysBeforeNotification);

        assertEquals(Collections.singletonList(subscription), subscriptionsToNotify);

        verify(subscriptionService, times(1))
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(subscriptionQuery ->
                    Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED).equals(subscriptionQuery.getStatuses()) &&
                    // 1469886010000 -> now + 10 days
                    subscriptionQuery.getEndingAtAfter() ==
                    1469886010000L &&
                    // 1469889610000 -> now + 10 days + 1h (cron pollInterval)
                    subscriptionQuery.getEndingAtBefore() ==
                    1469889610000L
                )
            );
    }

    @Test
    public void shouldFindEmailToNotifyWithDifferentSubscriberAndPrimaryOwner() {
        String subscriberId = UUID.randomUUID().toString();
        UserEntity subscriber = mock(UserEntity.class);
        when(subscriber.getEmail()).thenReturn("subscriber@gravitee.io");

        SubscriptionEntity subscription = mock(SubscriptionEntity.class);
        when(subscription.getSubscribedBy()).thenReturn(subscriberId);

        when(userService.findById(GraviteeContext.getExecutionContext(), subscriberId)).thenReturn(subscriber);

        PrimaryOwnerEntity primaryOwner = mock(PrimaryOwnerEntity.class);
        when(primaryOwner.getEmail()).thenReturn("primary_owner@gravitee.io");

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(application.getPrimaryOwner()).thenReturn(primaryOwner);

        Collection<String> usersToNotify = service.findEmailsToNotify(subscription, application);

        Set<String> expected = new HashSet<>();
        expected.add("subscriber@gravitee.io");
        expected.add("primary_owner@gravitee.io");
        assertEquals(expected, usersToNotify);
    }

    @Test
    public void shouldFindEmailToNotifyWithSameSubscriberAndPrimaryOwner() {
        String subscriberId = UUID.randomUUID().toString();
        UserEntity subscriber = mock(UserEntity.class);
        when(subscriber.getEmail()).thenReturn("primary_owner@gravitee.io");

        SubscriptionEntity subscription = mock(SubscriptionEntity.class);
        when(subscription.getSubscribedBy()).thenReturn(subscriberId);

        when(userService.findById(GraviteeContext.getExecutionContext(), subscriberId)).thenReturn(subscriber);

        PrimaryOwnerEntity primaryOwner = mock(PrimaryOwnerEntity.class);
        when(primaryOwner.getEmail()).thenReturn("primary_owner@gravitee.io");

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(application.getPrimaryOwner()).thenReturn(primaryOwner);

        Collection<String> usersToNotify = service.findEmailsToNotify(subscription, application);

        Set<String> expected = new HashSet<>();
        expected.add("primary_owner@gravitee.io");
        assertEquals(expected, usersToNotify);
    }

    @Test
    public void shouldSendEmail() {
        int day = 30;
        String subscriberEmail = "subscriber@gravitee.io";

        ApiEntity api = mock(ApiEntity.class);

        PlanEntity plan = mock(PlanEntity.class);
        ApplicationEntity application = mock(ApplicationEntity.class);
        ApiKeyEntity apiKey = mock(ApiKeyEntity.class);

        service.sendEmail(subscriberEmail, day, api, plan, application, apiKey);

        EmailNotification emailNotification = new EmailNotificationBuilder()
            .to(subscriberEmail)
            .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_SUBSCRIPTION_PRE_EXPIRATION)
            .param("expirationDelay", day)
            .param("api", api)
            .param("plan", plan)
            .param("application", application)
            .param("apiKey", apiKey)
            .build();

        verify(emailService, times(1)).sendAsyncEmailNotification(eq(GraviteeContext.getExecutionContext()), eq(emailNotification));
    }

    @Test
    public void shouldFindApiKeyExpirationsToNotify() {
        Instant now = Instant.ofEpochMilli(1469022010000L);
        Integer daysBeforeNotification = 10;

        ApiKeyEntity apiKey = mock(ApiKeyEntity.class);

        when(apiKeyService.search(eq(GraviteeContext.getExecutionContext()), any(ApiKeyQuery.class)))
            .thenReturn(Collections.singletonList(apiKey));

        Collection<ApiKeyEntity> apiKeysToNotify = service.findApiKeyExpirationsToNotify(now, daysBeforeNotification);

        assertEquals(Collections.singletonList(apiKey), apiKeysToNotify);

        verify(apiKeyService, times(1))
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiKeyQuery ->
                    !apiKeyQuery.isIncludeRevoked() &&
                    // 1469886010000 -> now + 10 days
                    apiKeyQuery.getExpireAfter() ==
                    1469886010000L &&
                    // 1469889610000 -> now + 10 days + 1h (cron pollInterval)
                    apiKeyQuery.getExpireBefore() ==
                    1469889610000L
                )
            );
    }
}
