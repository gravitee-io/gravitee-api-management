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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.subscription.model.ExpiringSubscription;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledSubscriptionPreExpirationNotificationServiceTest {

    @InjectMocks
    ScheduledSubscriptionPreExpirationNotificationService service = new ScheduledSubscriptionPreExpirationNotificationService();

    @Mock
    UserService userService;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    SubscriptionQueryService subscriptionQueryService;

    @Mock
    ApiKeyService apiKeyService;

    @Mock
    EmailService emailService;

    @Mock
    ApiSearchService apiSearchService;

    @Mock
    PlanSearchService planSearchService;

    @Mock
    ApplicationService applicationService;

    @Test
    void shouldCleanNotificationDays() {
        List<Integer> inputNotificationDays = Arrays.asList(-1, 150, 75, 10, 30, 400, 45);
        List<Integer> cleanedNotificationDays = service.getCleanedNotificationDays(inputNotificationDays);

        assertThat(cleanedNotificationDays).isEqualTo(Arrays.asList(150, 75, 45, 30, 10));
    }

    @Test
    void shouldQueryExpiringSubscriptionsOnceRegardlessOfScheduleLength() throws Exception {
        setNotificationDays(List.of(90, 45, 30));
        when(subscriptionQueryService.findExpiringSubscriptions(any(Instant.class), anyList(), anyLong(), anyList())).thenReturn(List.of());

        service.run();

        verify(subscriptionQueryService, times(1)).findExpiringSubscriptions(
            any(Instant.class),
            eq(List.of(90, 45, 30)),
            anyLong(),
            eq(List.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED))
        );
    }

    @Test
    void shouldBucketSubscriptionsByLargestMatchingDay() throws Exception {
        setNotificationDays(List.of(90, 45, 30));

        // run() uses Instant.now() internally — build endingAts relative to wall clock so they land in the cron-hour window.
        ZonedDateTime ending30 = ZonedDateTime.now().plusDays(30).plusMinutes(5);
        ZonedDateTime ending45 = ZonedDateTime.now().plusDays(45).plusMinutes(5);
        ZonedDateTime ending90 = ZonedDateTime.now().plusDays(90).plusMinutes(5);

        ExpiringSubscription sub30 = new ExpiringSubscription("s30", "api1", "plan1", "app1", "user1", ending30, null);
        ExpiringSubscription sub45 = new ExpiringSubscription("s45", "api1", "plan1", "app1", "user1", ending45, null);
        ExpiringSubscription sub90 = new ExpiringSubscription("s90", "api1", "plan1", "app1", "user1", ending90, null);

        when(subscriptionQueryService.findExpiringSubscriptions(any(Instant.class), anyList(), anyLong(), anyList())).thenReturn(
            List.of(sub30, sub45, sub90)
        );

        when(userService.findById(eq(GraviteeContext.getExecutionContext()), eq("user1"))).thenReturn(mock(UserEntity.class));
        ApplicationEntity app = mock(ApplicationEntity.class);
        when(app.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));
        when(applicationService.findById(any(), eq("app1"))).thenReturn(app);

        service.run();

        verify(subscriptionService, times(1)).updateDaysToExpirationOnLastNotification("s30", 30);
        verify(subscriptionService, times(1)).updateDaysToExpirationOnLastNotification("s45", 45);
        verify(subscriptionService, times(1)).updateDaysToExpirationOnLastNotification("s90", 90);
        verify(subscriptionService, times(3)).updateDaysToExpirationOnLastNotification(anyString(), anyInt());
    }

    @Test
    void bucketingIsInclusiveAtLowerBoundExclusiveAtUpperBound() throws Exception {
        setNotificationDays(List.of(30));

        Instant now = Instant.ofEpochMilli(1_700_000_000_000L);
        long oneDay = 24L * 60L * 60L * 1000L;
        long oneHour = 60L * 60L * 1000L;
        ZonedDateTime atLo = Instant.ofEpochMilli(now.toEpochMilli() + 30 * oneDay).atZone(ZoneOffset.UTC);
        ZonedDateTime justInsideHi = Instant.ofEpochMilli(now.toEpochMilli() + 30 * oneDay + oneHour - 1).atZone(ZoneOffset.UTC);
        ZonedDateTime atHi = Instant.ofEpochMilli(now.toEpochMilli() + 30 * oneDay + oneHour).atZone(ZoneOffset.UTC);
        ZonedDateTime justBeforeLo = Instant.ofEpochMilli(now.toEpochMilli() + 30 * oneDay - 1).atZone(ZoneOffset.UTC);

        ExpiringSubscription subAtLo = new ExpiringSubscription("at-lo", "a", "p", "ap", "u", atLo, null);
        ExpiringSubscription subJustInsideHi = new ExpiringSubscription("just-inside-hi", "a", "p", "ap", "u", justInsideHi, null);
        ExpiringSubscription subAtHi = new ExpiringSubscription("at-hi", "a", "p", "ap", "u", atHi, null);
        ExpiringSubscription subJustBeforeLo = new ExpiringSubscription("just-before-lo", "a", "p", "ap", "u", justBeforeLo, null);

        when(subscriptionQueryService.findExpiringSubscriptions(eq(now), anyList(), anyLong(), anyList())).thenReturn(
            List.of(subAtLo, subJustInsideHi, subAtHi, subJustBeforeLo)
        );

        var buckets = service.bucketExpiringSubscriptions(now);

        assertThat(buckets.get(30)).extracting(ExpiringSubscription::id).containsExactly("at-lo", "just-inside-hi");
    }

    private void setNotificationDays(List<Integer> days) throws Exception {
        Field f = ScheduledSubscriptionPreExpirationNotificationService.class.getDeclaredField("notificationDays");
        f.setAccessible(true);
        f.set(service, days);
    }

    @Test
    void shouldFindEmailToNotifyWithDifferentSubscriberAndPrimaryOwner() {
        String subscriberId = UUID.randomUUID().toString();
        UserEntity subscriber = mock(UserEntity.class);
        when(subscriber.getEmail()).thenReturn("subscriber@gravitee.io");

        when(userService.findById(GraviteeContext.getExecutionContext(), subscriberId)).thenReturn(subscriber);

        PrimaryOwnerEntity primaryOwner = mock(PrimaryOwnerEntity.class);
        when(primaryOwner.getEmail()).thenReturn("primary_owner@gravitee.io");

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(application.getPrimaryOwner()).thenReturn(primaryOwner);

        Collection<String> usersToNotify = service.findEmailsToNotify(subscriberId, application);

        Set<String> expected = new HashSet<>();
        expected.add("subscriber@gravitee.io");
        expected.add("primary_owner@gravitee.io");
        assertThat(usersToNotify).isEqualTo(expected);
    }

    @Test
    void shouldFindEmailToNotifyWithSameSubscriberAndPrimaryOwner() {
        String subscriberId = UUID.randomUUID().toString();
        UserEntity subscriber = mock(UserEntity.class);
        when(subscriber.getEmail()).thenReturn("primary_owner@gravitee.io");

        when(userService.findById(GraviteeContext.getExecutionContext(), subscriberId)).thenReturn(subscriber);

        PrimaryOwnerEntity primaryOwner = mock(PrimaryOwnerEntity.class);
        when(primaryOwner.getEmail()).thenReturn("primary_owner@gravitee.io");

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(application.getPrimaryOwner()).thenReturn(primaryOwner);

        Collection<String> usersToNotify = service.findEmailsToNotify(subscriberId, application);

        Set<String> expected = new HashSet<>();
        expected.add("primary_owner@gravitee.io");
        assertThat(usersToNotify).isEqualTo(expected);
    }

    @Test
    void shouldSendEmail() {
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
    void shouldFindApiKeyExpirationsToNotify() {
        Instant now = Instant.ofEpochMilli(1469022010000L);
        Integer daysBeforeNotification = 10;

        ApiKeyEntity apiKey = mock(ApiKeyEntity.class);

        when(apiKeyService.search(eq(GraviteeContext.getExecutionContext()), any(ApiKeyQuery.class))).thenReturn(
            Collections.singletonList(apiKey)
        );

        Collection<ApiKeyEntity> apiKeysToNotify = service.findApiKeyExpirationsToNotify(now, daysBeforeNotification);

        assertThat(apiKeysToNotify).isEqualTo(Collections.singletonList(apiKey));

        verify(apiKeyService, times(1)).search(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                apiKeyQuery ->
                    !apiKeyQuery.isIncludeRevoked() &&
                    // 1469886010000 -> now + 10 days
                    apiKeyQuery.getExpireAfter() ==
                    1469886010000L &&
                    // 1469889610000 -> now + 10 days + 1h (cron period)
                    apiKeyQuery.getExpireBefore() ==
                    1469889610000L
            )
        );
    }
}
