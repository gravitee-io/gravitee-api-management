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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.reactivex.disposables.Disposable;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscriptionDispatcher.class);

    private final Map<String, Disposable> actives = new ConcurrentHashMap<>();

    private final SubscriptionAcceptorResolver subscriptionAcceptorResolver;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SUBSCRIPTION_TYPE_FIELD = "type";
    private final SubscriptionExecutionRequestFactory subscriptionExecutionRequestFactory;

    public DefaultSubscriptionDispatcher(
        SubscriptionAcceptorResolver subscriptionAcceptorResolver,
        SubscriptionExecutionRequestFactory subscriptionExecutionRequestFactory
    ) {
        this.subscriptionAcceptorResolver = subscriptionAcceptorResolver;
        this.subscriptionExecutionRequestFactory = subscriptionExecutionRequestFactory;
    }

    @Override
    public void dispatch(Subscription subscription) {
        if ("ACCEPTED".equalsIgnoreCase(subscription.getStatus())) {
            if (!actives.containsKey(subscription.getId())) {
                Acceptor<SubscriptionAcceptor> acceptor = subscriptionAcceptorResolver.resolve(subscription);

                if (acceptor != null) {
                    ApiReactor reactorHandler = (ApiReactor) acceptor.reactor();

                    String configuration = subscription.getConfiguration();
                    try {
                        // Extract the type from the configuration
                        // For now, we admit that only webhook is supported
                        String type = mapper.readTree(configuration).path(SUBSCRIPTION_TYPE_FIELD).asText();
                        if (type == null || type.trim().isEmpty()) {
                            LOGGER.error("Unable to handle subscription without known type");
                        } else {
                            MutableExecutionContext context = subscriptionExecutionRequestFactory.create(subscription);

                            // This attribute is used by connectors
                            context.setInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION_TYPE, type);
                            context.setInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION, subscription);

                            // Maintain state depending on subscription status
                            // + timer on end (dispose on timer)
                            // subscribe
                            Disposable subscribe = reactorHandler.handle(context).subscribe();
                            actives.put(subscription.getId(), subscribe);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Unable to dispatch subscription id[{}] api[{}]", subscription.getId(), subscription.getApi(), ex);
                    }
                }
            }
        } else {
            actives.computeIfPresent(
                subscription.getId(),
                (s, disposable) -> {
                    disposable.dispose();
                    // Return null to remove the item from the map
                    return null;
                }
            );
        }
    }

    private void disposeAll() {
        for (Map.Entry<String, Disposable> item : actives.entrySet()) {
            item.getValue().dispose();
            actives.remove(item.getKey());
        }
    }

    @Override
    protected void doStop() throws Exception {
        this.disposeAll();
    }

    public Map<String, Disposable> getActiveSubscriptions() {
        return actives;
    }
}
