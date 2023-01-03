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
package io.gravitee.gateway.handlers.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.command.SubscriptionFailureCommand;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.ApiKeyService;
import io.gravitee.gateway.handlers.api.services.SubscriptionService;
import io.gravitee.gateway.jupiter.api.policy.SecurityToken;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.node.api.Node;
import io.gravitee.node.cache.standalone.StandaloneCacheManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    private static final String PLAN_ID = "my-test-plan-id";
    private static final String API_ID = "my-test-api-id";
    private static final String SUB_ID = "my-test-subscription-id";
    private static final String SUB_ID_2 = "my-test-subscription-id-2";
    private static final String API_KEY = "my-test-api-key";
    private static final String CLIENT_ID = "my-test-client-id";

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionDispatcher subscriptionDispatcher;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private Node node;

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private SubscriptionService subscriptionService;

    @BeforeEach
    public void setup() {
        subscriptionService =
            new SubscriptionService(
                new StandaloneCacheManager(),
                apiKeyService,
                subscriptionDispatcher,
                commandRepository,
                node,
                objectMapper
            );
    }

    @Test
    void should_add_and_getById_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getById(SUB_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getById("sub2").isPresent());
        assertFalse(subscriptionService.getById("sub3").isPresent());
    }

    @Test
    void should_add_and_getByApiAndClientId_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan("another-api", CLIENT_ID, PLAN_ID).isPresent());
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "another-client-id", PLAN_ID).isPresent());
    }

    @Test
    void should_add_and_getBySecurityToken_with_clientId() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
        subscriptionService.save(subscription);

        // should find subscription in cache with relevant security token
        SecurityToken securityToken = SecurityToken.forClientId(CLIENT_ID);
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find subscription in cache with another security token
        securityToken = SecurityToken.forClientId("another-client-id");
        assertFalse(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID).isPresent());
    }

    @Test
    void should_add_and_getBySecurityToken_with_apiKey() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, null, PLAN_ID);
        subscriptionService.save(subscription);

        // mock apiKeyService : it finds related Apikey, linked to subscription
        ApiKey apiKey = new ApiKey();
        apiKey.setSubscription(SUB_ID);
        when(apiKeyService.getByApiAndKey(API_ID, API_KEY)).thenReturn(Optional.of(apiKey));

        // should find subscription in cache with relevant security token
        SecurityToken securityToken = SecurityToken.forApiKey(API_KEY);
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find subscription in cache with another security token
        securityToken = SecurityToken.forApiKey("another-api-key");
        assertFalse(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID).isPresent());
    }

    @Test
    void should_remove_rejected_subscription_from_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        subscription.setStatus("REJECTED");

        subscriptionService.save(subscription);

        // rejected subscription has been removed from cache
        assertFalse(subscriptionService.getById(SUB_ID).isPresent());
    }

    @Test
    void should_update_subscription_clientId_in_cache() {
        Subscription oldSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "old-client-id", PLAN_ID);

        subscriptionService.save(oldSubscription);

        Subscription newSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "new-client-id", PLAN_ID);

        subscriptionService.save(newSubscription);

        // should find subscription in cache with its new client id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, "new-client-id", PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(newSubscription, foundSubscription.get());

        // should not find it with its old client id
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "old-client-id", PLAN_ID).isPresent());
    }

    @Test
    void should_dispatch_subscription_to_dispatcher() {
        Subscription subscription = mock(Subscription.class);
        lenient().when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getType()).thenReturn(Subscription.Type.SUBSCRIPTION);
        when(subscriptionDispatcher.dispatch(subscription)).thenReturn(Completable.complete());

        subscriptionService.save(subscription);

        verify(subscriptionDispatcher).dispatch(subscription);

        Optional<Subscription> optSubscription = subscriptionService.getById("sub-id");
        assertTrue(optSubscription.isEmpty());
    }

    @Test
    @DisplayName("Should create a command when an error occurs during dispatching of subscription")
    void should_send_command_when_dispatching_failure() throws TechnicalException, InterruptedException {
        Subscription subscription = mock(Subscription.class);
        lenient().when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getType()).thenReturn(Subscription.Type.SUBSCRIPTION);

        final CountDownLatch latch = new CountDownLatch(1);
        // When create(Command) is called, then the method is down, so we can count down the latch
        doAnswer(
                invocation -> {
                    latch.countDown();
                    return null;
                }
            )
            .when(commandRepository)
            .create(any());

        when(subscriptionDispatcher.dispatch(subscription)).thenReturn(Completable.error(new RuntimeException("Error! 💥")));
        when(node.id()).thenReturn("node-id");

        subscriptionService.save(subscription);

        latch.await();
        verify(subscriptionDispatcher).dispatch(subscription);

        final ArgumentCaptor<Command> commandArgumentCaptor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(commandArgumentCaptor.capture());

        final Command savedCommand = commandArgumentCaptor.getValue();
        assertThat(savedCommand)
            .satisfies(
                c -> {
                    assertThat(c.getTags()).hasSize(1).allMatch(t -> t.equals(CommandTags.SUBSCRIPTION_FAILURE.name()));
                    assertThat(c.getCreatedAt()).isEqualTo(c.getUpdatedAt());
                    assertThat(c.getFrom()).isEqualTo("node-id");
                    assertThat(c.getTo()).isEqualTo(MessageRecipient.MANAGEMENT_APIS.name());
                    final SubscriptionFailureCommand subscriptionCommand = objectMapper.readValue(
                        c.getContent(),
                        SubscriptionFailureCommand.class
                    );
                    assertThat(subscriptionCommand.getSubscriptionId()).isEqualTo("sub-id");
                    assertThat(subscriptionCommand.getFailureCause()).isEqualTo("Error! 💥");
                }
            );

        Optional<Subscription> optSubscription = subscriptionService.getById("sub-id");
        assertTrue(optSubscription.isEmpty());
    }

    @Test
    void should_evict_subscriptions_when_ids_match() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
        Subscription subscription2 = buildAcceptedSubscription(SUB_ID_2, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription.getId(), foundSubscription.get().getId());

        subscriptionService.save(subscription2);

        foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription2.getId(), foundSubscription.get().getId());

        subscription.setStatus("CLOSED");
        subscriptionService.save(subscription);

        foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription2.getId(), foundSubscription.get().getId());
    }

    private Subscription buildAcceptedSubscription(String id, String api, String clientId, String plan) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(api);
        subscription.setPlan(plan);
        subscription.setClientId(clientId);
        subscription.setStatus("ACCEPTED");
        return subscription;
    }
}
