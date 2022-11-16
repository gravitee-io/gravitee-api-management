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
package io.gravitee.gateway.jupiter.handlers.api.processor.subscription;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_API;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_CLIENT_IDENTIFIER;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.handlers.api.context.SubscriptionTemplateVariableProvider;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.bouncycastle.asn1.x509.Holder;

/**
 * This processor will add template variable provide for the resolved {@link Subscription} if any.
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionProcessor implements Processor {

    public static final String ID = "processor-subscription";
    public static final String DEFAULT_CLIENT_IDENTIFIER_HEADER = "X-Gravitee-Client-Identifier";
    static final String APPLICATION_ANONYMOUS = "1";
    static final String PLAN_ANONYMOUS = "1";
    private String clientIdentifierHeader = DEFAULT_CLIENT_IDENTIFIER_HEADER;

    public static SubscriptionProcessor instance(final String clientIdentifierHeader) {
        SubscriptionProcessor subscriptionProcessor = Holder.INSTANCE;
        if (clientIdentifierHeader != null) {
            subscriptionProcessor.clientIdentifierHeader = clientIdentifierHeader;
        }
        return subscriptionProcessor;
    }

    String clientIdentifierHeader() {
        return clientIdentifierHeader;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                String clientIdentifier = ctx.request().headers().get(clientIdentifierHeader);
                Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
                if (subscription == null) {
                    final String application = APPLICATION_ANONYMOUS;
                    final String plan = PLAN_ANONYMOUS;
                    final String subscriptionId = ctx.request().remoteAddress();
                    if (clientIdentifier == null) {
                        clientIdentifier = ctx.request().transactionId();
                        ctx.request().headers().set(clientIdentifierHeader, clientIdentifier);
                    }

                    // Fixes consuming application and subscription which are data that can be used by policies (ie. rate-limit).
                    ctx.setAttribute(ATTR_APPLICATION, application);
                    ctx.setAttribute(ATTR_PLAN, plan);
                    ctx.setAttribute(ATTR_SUBSCRIPTION_ID, subscriptionId);

                    subscription = new Subscription();
                    subscription.setId(subscriptionId);
                    subscription.setApi(ctx.getAttribute(ATTR_API));
                    subscription.setPlan(plan);
                    subscription.setApplication(application);
                    subscription.setStatus(ACCEPTED.name());
                    ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);
                } else {
                    if (clientIdentifier == null) {
                        clientIdentifier = subscription.getId();
                    }
                }
                ctx.setAttribute(ATTR_CLIENT_IDENTIFIER, clientIdentifier);
                ctx.request().clientIdentifier(clientIdentifier);
                ctx.request().headers().set(clientIdentifierHeader, clientIdentifier);
                ctx.response().headers().set(clientIdentifierHeader, clientIdentifier);

                final Metrics metrics = ctx.request().metrics();
                // Stores information about the resolved plan (according to the incoming request)
                metrics.setPlan(ctx.getAttribute(ATTR_PLAN));
                metrics.setApplication(ctx.getAttribute(ATTR_APPLICATION));
                metrics.setSubscription(ctx.getAttribute(ATTR_SUBSCRIPTION_ID));
                metrics.setClientIdentifier(clientIdentifier);

                Collection<TemplateVariableProvider> templateVariableProviders = ctx.templateVariableProviders();
                if (templateVariableProviders == null) {
                    templateVariableProviders = new ArrayList<>();
                }
                templateVariableProviders.add(new SubscriptionTemplateVariableProvider(subscription));
                ctx.templateVariableProviders(templateVariableProviders);
            }
        );
    }

    private static class Holder {

        private static final SubscriptionProcessor INSTANCE = new SubscriptionProcessor();
    }
}
