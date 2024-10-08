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
package io.gravitee.gateway.reactive.handlers.api.processor.subscription;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_API;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_CLIENT_IDENTIFIER;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.handlers.api.context.SubscriptionTemplateVariableProvider;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

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
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            String planId = ctx.getAttribute(ATTR_PLAN);
            String applicationId = ctx.getAttribute(ATTR_APPLICATION);
            String subscriptionId = ctx.getAttribute(ATTR_SUBSCRIPTION_ID);
            String transactionId = ctx.request().transactionId();
            if (Objects.equals(true, ctx.getInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP))) {
                // Fixes consuming application and subscription which are data that can be used by policies (ie. rate-limit).
                if (applicationId == null) {
                    applicationId = APPLICATION_ANONYMOUS;
                    ctx.setAttribute(ATTR_APPLICATION, applicationId);
                }
                if (planId == null) {
                    planId = PLAN_ANONYMOUS;
                    ctx.setAttribute(ATTR_PLAN, planId);
                }
                if (subscriptionId == null) {
                    subscriptionId = ctx.request().remoteAddress();
                    ctx.setAttribute(ATTR_SUBSCRIPTION_ID, subscriptionId);
                }
            }

            String requestClientIdentifier = ctx.request().headers().get(clientIdentifierHeader);
            if (requestClientIdentifier == null) {
                requestClientIdentifier = ctx.request().parameters().getFirst(clientIdentifierHeader);
            }
            String ctxClientIdentifier;

            // If request doesn't contain ClientIdentifier header, generate new one
            if (requestClientIdentifier == null || requestClientIdentifier.isBlank()) {
                ctxClientIdentifier = computeCtxClientIdentifier(ctx, subscriptionId, transactionId);
                requestClientIdentifier = ctxClientIdentifier;
            } else {
                // Make sure given ClientIdentifier header, is ctx from subscription
                if ((!requestClientIdentifier.endsWith(subscriptionId) && !requestClientIdentifier.endsWith(transactionId))) {
                    ctxClientIdentifier = requestClientIdentifier + "-" + computeCtxClientIdentifier(ctx, subscriptionId, transactionId);
                } else {
                    ctxClientIdentifier = requestClientIdentifier;
                }
            }
            ctx.response().headers().set(clientIdentifierHeader, requestClientIdentifier);
            ctx.setAttribute(ATTR_CLIENT_IDENTIFIER, ctxClientIdentifier);
            ctx.request().clientIdentifier(ctxClientIdentifier);

            final Metrics metrics = ctx.metrics();
            // Stores information about the resolved plan (according to the incoming request)
            metrics.setPlanId(planId);
            metrics.setApplicationId(ctx.getAttribute(ATTR_APPLICATION));
            metrics.setSubscriptionId(subscriptionId);
            metrics.setClientIdentifier(requestClientIdentifier);
            if (metrics.getLog() != null) {
                metrics.getLog().setClientIdentifier(requestClientIdentifier);
            }

            Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
            if (subscription == null) {
                subscription = new Subscription();
                subscription.setId(subscriptionId);
                subscription.setApi(ctx.getAttribute(ATTR_API));
                subscription.setPlan(planId);
                subscription.setApplication(applicationId);
                subscription.setStatus(ACCEPTED.name());
                ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);
            }
            Collection<TemplateVariableProvider> templateVariableProviders = ctx.templateVariableProviders();
            if (templateVariableProviders == null) {
                templateVariableProviders = new ArrayList<>();
            }
            templateVariableProviders.add(new SubscriptionTemplateVariableProvider(subscription));
            ctx.templateVariableProviders(templateVariableProviders);
        });
    }

    /**
     * Compute the client identifier from the incoming requests :
     * <ol>
     *     <li>Use the subscription id if different <code>null</code> and NON EQUALS to remote address</ul>
     *     <li>Use a hash of the subscription id if different <code>null</code> and EQUALS to remote address</ul>
     *     <li>Use a fallback id if subscription id is null</ul>
     * </li>
     * @param ctx
     * @param subscriptionId
     * @param fallbackId
     * @return client identifier
     */
    private String computeCtxClientIdentifier(
        final HttpExecutionContextInternal ctx,
        final String subscriptionId,
        final String fallbackId
    ) {
        if (subscriptionId != null && !subscriptionId.equals(ctx.request().remoteAddress())) {
            return subscriptionId;
        } else if (subscriptionId != null && subscriptionId.equals(ctx.request().remoteAddress())) {
            return computeHashOrDefault(subscriptionId, fallbackId);
        } else {
            return fallbackId;
        }
    }

    private String computeHashOrDefault(final String valueToHash, final String fallback) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(valueToHash.getBytes());
            byte[] digest = md.digest();
            return Hex.toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            return fallback;
        }
    }

    private static class Holder {

        private static final SubscriptionProcessor INSTANCE = new SubscriptionProcessor();
    }
}
