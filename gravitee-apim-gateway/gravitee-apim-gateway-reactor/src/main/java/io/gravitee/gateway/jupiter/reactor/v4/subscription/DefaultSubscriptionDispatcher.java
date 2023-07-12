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
package io.gravitee.gateway.jupiter.reactor.v4.subscription;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Predicate;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultSubscriptionDispatcher extends AbstractService<SubscriptionDispatcher> implements SubscriptionDispatcher {

    public static final int ON_SUBSCRIPTION_ERROR_RETRY_COUNT = 5;
    public static final int ON_SUBSCRIPTION_ERROR_RETRY_DELAY_MS = 3_000;
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscriptionDispatcher.class);
    private static final String SUBSCRIPTION_ENTRYPOINT_FIELD = "entrypointId";
    private final Map<String, Subscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Disposable> activeDisposables = new ConcurrentHashMap<>();

    private final SubscriptionAcceptorResolver subscriptionAcceptorResolver;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubscriptionExecutionRequestFactory subscriptionExecutionRequestFactory;

    public DefaultSubscriptionDispatcher(
        SubscriptionAcceptorResolver subscriptionAcceptorResolver,
        SubscriptionExecutionRequestFactory subscriptionExecutionRequestFactory
    ) {
        this.subscriptionAcceptorResolver = subscriptionAcceptorResolver;
        this.subscriptionExecutionRequestFactory = subscriptionExecutionRequestFactory;
    }

    private static boolean statusIsAccepted(Subscription subscriptionToDispatch) {
        return "ACCEPTED".equalsIgnoreCase(subscriptionToDispatch.getStatus());
    }

    private static boolean consumerStatusIsActive(Subscription subscriptionToDispatch) {
        return Subscription.ConsumerStatus.STARTED.equals(subscriptionToDispatch.getConsumerStatus());
    }

    private static Predicate<Throwable> manageErrors() {
        return throwable -> {
            if (throwable instanceof SubscriptionExpiredException) {
                LOGGER.debug(throwable.getMessage());
                return true;
            }
            // manage functional error to complete normally here
            return false;
        };
    }

    @Override
    public void dispatch(Subscription subscriptionToDispatch) {
        if (
            statusIsAccepted(subscriptionToDispatch) && consumerStatusIsActive(subscriptionToDispatch) && !isExpired(subscriptionToDispatch)
        ) {
            activeSubscriptions.compute(
                subscriptionToDispatch.getId(),
                (subscriptionId, activeSubscription) -> {
                    // activate subscription if not yet active
                    if (activeSubscription == null) {
                        return activateSubscription(subscriptionToDispatch);
                    }
                    // if subscription already active modified, update it
                    else if (!subscriptionToDispatch.equals(activeSubscription) || subscriptionToDispatch.isForceDispatch()) {
                        return updateSubscription(subscriptionToDispatch);
                    }
                    // otherwise, keep active subscription as it is
                    return activeSubscription;
                }
            );
        }
        // dispose subscription if it's not accepted, or already expired
        else {
            disposeSubscription(subscriptionToDispatch.getId());
        }
    }

    private Subscription activateSubscription(Subscription subscription) {
        Acceptor<SubscriptionAcceptor> acceptor = subscriptionAcceptorResolver.resolve(subscription);

        if (acceptor != null) {
            ApiReactor reactorHandler = (ApiReactor) acceptor.reactor();

            String configuration = subscription.getConfiguration();
            try {
                // Extract the type from the configuration
                String type = mapper.readTree(configuration).path(SUBSCRIPTION_ENTRYPOINT_FIELD).asText();
                if (type == null || type.trim().isEmpty()) {
                    LOGGER.error("Unable to handle subscription without known entrypoint id");
                } else {
                    Completable subscriptionObs = buildSubscriptionObservable(subscription, reactorHandler, type)
                        .doOnComplete(() -> activeDisposables.remove(subscription.getId()));

                    activeDisposables.put(subscription.getId(), subscriptionObs.subscribe());

                    return subscription;
                }
            } catch (Exception ex) {
                LOGGER.error("Unable to dispatch subscription id[{}] api[{}]", subscription.getId(), subscription.getApi(), ex);
            }
        }
        return null;
    }

    private Completable buildSubscriptionObservable(Subscription subscription, ApiReactor reactorHandler, String type) {
        return Single
            .fromCallable(() -> {
                MutableExecutionContext context = subscriptionExecutionRequestFactory.create(subscription);

                // This attribute is used by connectors
                context.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE, type);
                context.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);
                context.setAttribute(ContextAttributes.ATTR_PLAN, subscription.getPlan());
                context.setAttribute(ContextAttributes.ATTR_APPLICATION, subscription.getApplication());
                context.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, subscription.getId());

                // Skip the security chain (currently requires to be an attribute as long as we support v3).
                context.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);

                context.setInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE, ListenerType.SUBSCRIPTION);
                return context;
            })
            .flatMapCompletable(reactorHandler::handle)
            // Apply a delay before starting the subscription if it has a starting date
            .compose(delayToStartDate(subscription))
            // Apply a timeout to the subscription if it has an ending date
            .compose(timeoutAtEndingDate(subscription))
            // Manage functional errors, for example when a subscription expires
            .onErrorComplete(manageErrors())
            // In case of unhandled exception, retry ON_SUBSCRIPTION_ERROR_RETRY_COUNT times with a delay of ON_SUBSCRIPTION_ERROR_RETRY_DELAY_MS between attempts
            .compose(RxHelper.retry(ON_SUBSCRIPTION_ERROR_RETRY_COUNT, ON_SUBSCRIPTION_ERROR_RETRY_DELAY_MS, MILLISECONDS))
            .onErrorResumeNext(t -> {
                // Here, manage ERROR status for subscription and send command to repository
                return Completable.complete();
            });
    }

    private CompletableTransformer delayToStartDate(Subscription subscription) {
        return upstream -> {
            if (subscription.getStartingAt() != null) {
                long subscriptionStartDelay = getMillisecondsTo(subscription.getStartingAt());
                return upstream.delaySubscription(subscriptionStartDelay, MILLISECONDS);
            }
            return upstream;
        };
    }

    private CompletableTransformer timeoutAtEndingDate(Subscription subscription) {
        return upstream -> {
            if (subscription.getEndingAt() != null) {
                long subscriptionEndDelay = getMillisecondsTo(subscription.getEndingAt());
                return upstream.timeout(
                    subscriptionEndDelay,
                    MILLISECONDS,
                    Completable.error(new SubscriptionExpiredException(subscription))
                );
            }
            return upstream;
        };
    }

    /**
     * Dispose all active subscriptions.
     */
    private void disposeAllSubscriptions() {
        for (String subscriptionId : activeDisposables.keySet()) {
            disposeSubscription(subscriptionId);
        }
    }

    /**
     * Dispose the specified subscription.
     *
     * @param subscriptionId
     */
    private void disposeSubscription(String subscriptionId) {
        Disposable disposable = activeDisposables.remove(subscriptionId);
        if (disposable != null) {
            try {
                disposable.dispose();
            } catch (Exception e) {
                LOGGER.warn("Unexpected exception while disposing subscription [{}]: {}", subscriptionId, e.getMessage());
            }
        }
        activeSubscriptions.remove(subscriptionId);
    }

    /**
     * Update the specified subscription.
     *
     * @param subscription The subscription to update
     * @return updated subscription
     */
    private Subscription updateSubscription(Subscription subscription) {
        // dispose old subscription, but keep it in map
        // as it will be replaced by subsequent call to activateSubscription
        if (activeDisposables.containsKey(subscription.getId())) {
            activeDisposables.get(subscription.getId()).dispose();
        }
        return activateSubscription(subscription);
    }

    /**
     * Get delay from current time to specified date, in milliseconds.
     *
     * @param to
     * @return delay in milliseconds. 0 if to is before current time.
     */
    private long getMillisecondsTo(Date to) {
        long delay = to.getTime() - Calendar.getInstance().getTimeInMillis();
        return delay > 0 ? delay : 0;
    }

    /**
     * Is the specified subscription expired at current time ?
     *
     * @param subscription
     * @return true if it's expired, false otherwise
     */
    private boolean isExpired(Subscription subscription) {
        return subscription.getEndingAt() != null && subscription.getEndingAt().before(Calendar.getInstance().getTime());
    }

    @Override
    protected void doStop() throws Exception {
        this.disposeAllSubscriptions();
    }

    protected Map<String, Subscription> getActiveSubscriptions() {
        return activeSubscriptions;
    }

    protected Map<String, Disposable> getActiveDisposables() {
        return activeDisposables;
    }
}
