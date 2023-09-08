package io.gravitee.apim.infra.domain_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import inmemory.AuditCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.infra.domain_service.audit.AuditDomainServiceImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseSubscriptionDomainServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private TriggerNotificationDomainServiceInMemory triggerNotificationService;

    private AuditCrudServiceInMemory auditCrudServiceInMemory;
    private CloseSubscriptionDomainService service;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        GraviteeContext.setCurrentOrganization("organization-id");
        GraviteeContext.setCurrentEnvironment("environment-id");

        triggerNotificationService = new TriggerNotificationDomainServiceInMemory();

        // TODO  Use GraviteeJsonMapper instance
        auditCrudServiceInMemory = new AuditCrudServiceInMemory();
        service =
            new CloseSubscriptionDomainServiceImpl(
                subscriptionRepository,
                triggerNotificationService,
                new AuditDomainServiceImpl(auditCrudServiceInMemory, new UserCrudServiceInMemory(), new GraviteeMapper())
            );

        allowSubscriptionRepositorySave();
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_throw_when_close_unknown_subscription() {
        // Given no subscription
        Throwable throwable = catchThrowable(() ->
            service.closeSubscription(
                GraviteeContext.getExecutionContext(),
                "subscription-id",
                AuditActor.builder().userId("user-id").build()
            )
        );
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining("subscription-id");
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "CLOSED", "REJECTED", "PENDING" })
    void should_throw_when_close_subscription(Subscription.Status status) {
        // Given
        givenExistingSubscription(
            Subscription.builder().id("subscription-id").application("application-id").api("api-id").plan("plan-id").status(status).build()
        );

        Throwable throwable = catchThrowable(() ->
            service.closeSubscription(
                GraviteeContext.getExecutionContext(),
                "subscription-id",
                AuditActor.builder().userId("user-id").build()
            )
        );
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot close subscription with status " + status.name());
    }

    @ParameterizedTest
    @EnumSource(value = Subscription.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_close_accepted_or_paused_subscription(Subscription.Status status) {
        // Given
        givenExistingSubscription(
            Subscription.builder().id("subscription-id").application("application-id").api("api-id").plan("plan-id").status(status).build()
        );

        // When
        service.closeSubscription(GraviteeContext.getExecutionContext(), "subscription-id", AuditActor.builder().userId("user-id").build());

        // Then
        assertThat(triggerNotificationService.getApiNotifications())
            .containsExactly(new SubscriptionClosedApiHookContext("api-id", "application-id", "plan-id"));

        assertThat(triggerNotificationService.getApplicationNotifications())
            .containsExactly(new SubscriptionClosedApplicationHookContext("application-id", "api-id", "plan-id"));

        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .containsExactly(
                new AuditEntity(
                    "audit-id",
                    "organization-id",
                    "environment-id",
                    AuditEntity.AuditReferenceType.API,
                    "api-id",
                    "user-id",
                    Map.of("APPLICATION", "application-id"),
                    "SUBSCRIPTION_CLOSED",
                    ZonedDateTime.now(),
                    ""
                ),
                new AuditEntity(
                    "audit-id",
                    "organization-id",
                    "environment-id",
                    AuditEntity.AuditReferenceType.APPLICATION,
                    "application-id",
                    "user-id",
                    Map.of("API", "api-id"),
                    "SUBSCRIPTION_CLOSED",
                    ZonedDateTime.now(),
                    ""
                )
            );
    }

    @SneakyThrows
    private void givenExistingSubscription(Subscription subscription) {
        lenient().when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(subscriptionRepository.findById(eq(subscription.getId()))).thenReturn(Optional.of(subscription));
    }

    @SneakyThrows
    private void allowSubscriptionRepositorySave() {
        lenient().when(subscriptionRepository.update(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    }
}
