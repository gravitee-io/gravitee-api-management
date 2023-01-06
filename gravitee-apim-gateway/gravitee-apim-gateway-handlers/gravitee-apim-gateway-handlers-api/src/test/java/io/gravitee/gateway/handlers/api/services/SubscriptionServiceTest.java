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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.command.SubscriptionFailureCommand;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final String PLAN_ID = "my-test-plan-id";
    private static final String API_ID = "my-test-api-id";
    private static final String SUB_ID = "my-test-subscription-id";
    private static final String SUB_ID_2 = "my-test-subscription-id-2";
    private static final String API_KEY = "my-test-api-key";
    private static final String CLIENT_ID = "my-test-client-id";

    private static final BiConsumer<SubscriptionService, Subscription> SAVE_ONLY_STRATEGY = SubscriptionService::save;

    private static final BiConsumer<SubscriptionService, Subscription> SAVE_THEN_DISPATCH_STRATEGY = (service, subscription) -> {
        service.save(subscription);
        service.dispatchFor(List.of(API_ID));
    };

    private static final BiConsumer<SubscriptionService, Subscription> SAVE_OR_DISPATCH_STRATEGY = SubscriptionService::saveOrDispatch;

    /**
     * Regarding subscription of type {@link Subscription.Type#SUBSCRIPTION}, they can be dispatch with two different strategies:
     * - {@link this#SAVE_THEN_DISPATCH_STRATEGY} Subscriptions can be saved in a cache, then deployed when belonging apis are deployed
     * - {@link this#SAVE_OR_DISPATCH_STRATEGY} Subscriptions are directly dispatched.
     * @return a stream of dispatching strategies
     */
    private static Stream<Arguments> provideDispatchingStrategies() {
        return Stream.of(
            Arguments.of(SAVE_THEN_DISPATCH_STRATEGY, "Save subscription first, then dispatch it for the related api"),
            Arguments.of(SAVE_OR_DISPATCH_STRATEGY, "Dispatch a subscription directly")
        );
    }

    /**
     * Regarding subscription of type {@link Subscription.Type#STANDARD}, they can be saved with two different strategies.
     * Both strategies do the same thing, they cache the subscription. This stream allows to test two different entry points for the same result.
     * - {@link this#SAVE_ONLY_STRATEGY}
     * - {@link this#SAVE_OR_DISPATCH_STRATEGY}
     * @return a stream of dispatching strategies
     */
    private static Stream<Arguments> provideStandardSubscriptionSavingStrategies() {
        return Stream.of(
            Arguments.of(SAVE_ONLY_STRATEGY, "Save subscription first, then dispatch it for the related api"),
            Arguments.of(SAVE_OR_DISPATCH_STRATEGY, "Dispatch a subscription directly")
        );
    }

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionDispatcher subscriptionDispatcher;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private Node node;

    @Spy
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

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should add and get by Id accepted subscription in cache")
    void should_add_and_getById_accepted_subscription_in_cache(
        BiConsumer<SubscriptionService, Subscription> savingStrategy,
        String testName
    ) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        savingStrategy.accept(subscriptionService, subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getById(SUB_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getById("sub2").isPresent());
        assertFalse(subscriptionService.getById("sub3").isPresent());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should add and get by id and client id accepted subscription in cache")
    void should_add_and_getByApiAndClientId_accepted_subscription_in_cache(
        BiConsumer<SubscriptionService, Subscription> savingStrategy,
        String testName
    ) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        savingStrategy.accept(subscriptionService, subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan("another-api", CLIENT_ID, PLAN_ID).isPresent());
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "another-client-id", PLAN_ID).isPresent());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should add and get by security token with client id")
    void should_add_and_getBySecurityToken_with_clientId(BiConsumer<SubscriptionService, Subscription> savingStrategy, String testName) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
        savingStrategy.accept(subscriptionService, subscription);

        // should find subscription in cache with relevant security token
        SecurityToken securityToken = SecurityToken.forClientId(CLIENT_ID);
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find subscription in cache with another security token
        securityToken = SecurityToken.forClientId("another-client-id");
        assertFalse(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID).isPresent());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should add and get by security token with api key")
    void should_add_and_getBySecurityToken_with_apiKey(BiConsumer<SubscriptionService, Subscription> savingStrategy, String testName) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, null, PLAN_ID);
        savingStrategy.accept(subscriptionService, subscription);

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

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should remove rejected subscription from cache")
    void should_remove_rejected_subscription_from_cache(BiConsumer<SubscriptionService, Subscription> savingStrategy, String testName) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        savingStrategy.accept(subscriptionService, subscription);

        subscription.setStatus("REJECTED");

        savingStrategy.accept(subscriptionService, subscription);

        // rejected subscription has been removed from cache
        assertFalse(subscriptionService.getById(SUB_ID).isPresent());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should update subscription client id in cache")
    void should_update_subscription_clientId_in_cache(BiConsumer<SubscriptionService, Subscription> savingStrategy, String testName) {
        Subscription oldSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "old-client-id", PLAN_ID);

        savingStrategy.accept(subscriptionService, oldSubscription);

        Subscription newSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "new-client-id", PLAN_ID);

        savingStrategy.accept(subscriptionService, newSubscription);

        // should find subscription in cache with its new client id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, "new-client-id", PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(newSubscription, foundSubscription.get());

        // should not find it with its old client id
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "old-client-id", PLAN_ID).isPresent());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideDispatchingStrategies")
    @DisplayName("Should dispatch subscription to dispatcher")
    void should_dispatch_subscription_to_dispatcher(BiConsumer<SubscriptionService, Subscription> dispatchingStrategy, String testName) {
        Subscription subscription = fakeSubscription("sub-id", API_ID);
        when(subscriptionDispatcher.dispatch(subscription)).thenReturn(Completable.complete());

        dispatchingStrategy.accept(subscriptionService, subscription);

        verify(subscriptionDispatcher).dispatch(subscription);

        Optional<Subscription> optSubscription = subscriptionService.getById("sub-id");
        assertTrue(optSubscription.isEmpty());
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideDispatchingStrategies")
    @DisplayName("Should create a command when an error occurs during dispatching of subscription")
    void should_send_command_when_dispatching_failure(BiConsumer<SubscriptionService, Subscription> dispatchingStrategy, String testName)
        throws TechnicalException, InterruptedException {
        Subscription subscription = fakeSubscription("sub-id", API_ID);

        final CountDownLatch latch = new CountDownLatch(1);
        // When create(Command) is called, then the method is over, so we can count down the latch
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

        dispatchingStrategy.accept(subscriptionService, subscription);

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

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideDispatchingStrategies")
    @DisplayName("Should create a command when an error occurs during dispatching of subscription")
    void should_send_command_when_dispatching_failure_json_object_fallback(
        BiConsumer<SubscriptionService, Subscription> dispatchingStrategy,
        String testName
    ) throws TechnicalException, InterruptedException, JsonProcessingException {
        Subscription subscription = fakeSubscription("sub-id", API_ID);

        final CountDownLatch latch = new CountDownLatch(1);
        // When create(Command) is called, then the method is over, so we can count down the latch
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
        when(objectMapper.writeValueAsString(any(SubscriptionFailureCommand.class))).thenThrow(JsonProcessingException.class);

        dispatchingStrategy.accept(subscriptionService, subscription);

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
    @DisplayName("Should save then dispatch multiple subscriptions linked to different apis to dispatcher")
    void should_save_then_dispatch_multiple_subscription_to_dispatcher() {
        Subscription subscription1Api1 = fakeSubscription("sub-1", "api-1");
        Subscription subscription2Api1 = fakeSubscription("sub-2", "api-1");
        Subscription subscription1Api2 = fakeSubscription("sub-1", "api-2");
        Subscription subscription2Api2 = fakeSubscription("sub-2", "api-2");
        Subscription subscription1Api3 = fakeSubscription("sub-1", "api-3");
        when(subscriptionDispatcher.dispatch(any(Subscription.class))).thenReturn(Completable.complete());

        // All subscriptions are saved as it would be done in FullSubscriptionRefresher
        subscriptionService.save(subscription1Api1);
        subscriptionService.save(subscription2Api1);
        subscriptionService.save(subscription1Api2);
        subscriptionService.save(subscription2Api2);
        subscriptionService.save(subscription1Api3);

        // Then, when related apis are deployed, we dispatch subscriptions for those apis
        subscriptionService.dispatchFor(List.of("api-1", "api-2", "api-3"));

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionDispatcher, times(5)).dispatch(subscriptionCaptor.capture());

        final List<Subscription> dispatchedSubscriptions = subscriptionCaptor.getAllValues();
        assertThat(dispatchedSubscriptions)
            .hasSize(5)
            .containsExactly(subscription1Api1, subscription2Api1, subscription1Api2, subscription2Api2, subscription1Api3);
    }

    @ParameterizedTest(name = "[{index}] - {1}.")
    @MethodSource("provideStandardSubscriptionSavingStrategies")
    @DisplayName("Should evict subscription when ids match")
    void should_evict_subscriptions_when_ids_match(BiConsumer<SubscriptionService, Subscription> savingStrategy, String testName) {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
        Subscription subscription2 = buildAcceptedSubscription(SUB_ID_2, API_ID, CLIENT_ID, PLAN_ID);

        savingStrategy.accept(subscriptionService, subscription);

        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription.getId(), foundSubscription.get().getId());

        savingStrategy.accept(subscriptionService, subscription2);

        foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription2.getId(), foundSubscription.get().getId());

        subscription.setStatus("CLOSED");
        savingStrategy.accept(subscriptionService, subscription);

        foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertEquals(subscription2.getId(), foundSubscription.get().getId());
    }

    private static Subscription fakeSubscription(String id, String apiId) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setType(Subscription.Type.SUBSCRIPTION);
        subscription.setApi(apiId);
        return subscription;
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
