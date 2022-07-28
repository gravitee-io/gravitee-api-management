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
package io.gravitee.gateway.jupiter.handlers.api.security.plan;

import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_API;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_SUBSCRIPTION_ID;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SecurityPlan} allows to wrap a {@link Policy} implementing {@link SecurityPolicy} and make it working in a security chain.
 * Security plan is responsible to
 * <ul>
 *     <li>Check if a policy can handle the security or not</li>
 *     <li>Check the eventual selection rule matches (useful when dealing with multiple plans relying on the same security scheme such as <code>Authorization: bearer xxx</code> for JWT and OAuth)</li>
 *     <li>Check if the policy requires to validate there is an associated subscription or not and validate the subscription accordingly</li>
 *     <li>Invoke the <code>onSubscriptionInvalid</code> method when necessary</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPlan {

    protected static final String CONTEXT_ATTRIBUTE_CLIENT_ID = "oauth.client_id";
    protected static final Single<Boolean> TRUE = Single.just(true);
    protected static final Single<Boolean> FALSE = Single.just(false);
    private static final Logger log = LoggerFactory.getLogger(SecurityPlan.class);
    private final Plan plan;
    private final SecurityPolicy policy;
    private final String selectionRule;

    SecurityPlan(@Nonnull final Plan plan, @Nonnull final SecurityPolicy policy) {
        this.plan = plan;
        this.policy = policy;
        this.selectionRule = getSelectionRule(plan.getSelectionRule());
    }

    public String id() {
        return policy.id();
    }

    /**
     * Make sure the security plan can be executed for the current request.
     *
     * @param ctx the current execution context.
     * @return <code>true</code> if this security plan can be executed for the request, <code>false</code> otherwise.
     */
    public Single<Boolean> canExecute(RequestExecutionContext ctx) {
        return policy
            .support(ctx)
            .flatMap(
                support -> {
                    if (support) {
                        return matchSelectionRule(ctx);
                    }

                    return FALSE;
                }
            );
    }

    /**
     * Invokes the policy's <code>onRequest</code> method and eventually validates the associate subscription.
     * It's up to the policy to implement the behavior to adapt when the subscription is not valid (usually, interrupts the execution with a 401).
     *
     * @param ctx the current execution context.
     * @return a {@link Completable} that completes when the security policy has been successfully executed or returns an error otherwise.
     */
    public Completable execute(RequestExecutionContext ctx) {
        return policy
            .onRequest(ctx)
            .andThen(validateSubscription(ctx))
            .doOnSubscribe(disposable -> ctx.setAttribute(ATTR_PLAN, plan.getId()));
    }

    public int order() {
        return policy.order();
    }

    private String getSelectionRule(String selectionRule) {
        if (selectionRule == null) {
            return null;
        }

        if (selectionRule.startsWith("#")) {
            // Backward compatibility. In V3 mode selection rule EL expression based can be defined with "#something" while it is usually defined with "{#something}" everywhere else.
            return "{" + selectionRule + "}";
        }
        return selectionRule;
    }

    private Single<Boolean> matchSelectionRule(RequestExecutionContext ctx) {
        if (selectionRule == null || selectionRule.isEmpty()) {
            return TRUE;
        }

        return ctx.getTemplateEngine().eval(selectionRule, Boolean.class).toSingle();
    }

    private Completable validateSubscription(RequestExecutionContext ctx) {
        if (!policy.requireSubscription()) {
            return Completable.complete();
        }

        return Completable.defer(
            () -> {
                try {
                    SubscriptionService subscriptionService = ctx.getComponent(SubscriptionService.class);

                    Optional<Subscription> subscriptionOpt = Optional.empty();
                    String subscriptionId = ctx.getAttribute(ATTR_SUBSCRIPTION_ID);
                    String api = ctx.getAttribute(ATTR_API);
                    String clientId = ctx.getAttribute(CONTEXT_ATTRIBUTE_CLIENT_ID);
                    if (subscriptionId != null) {
                        subscriptionOpt = subscriptionService.getById(subscriptionId);
                    } else if (api != null && clientId != null) {
                        subscriptionOpt = subscriptionService.getByApiAndClientIdAndPlan(api, clientId, plan.getId());
                    }

                    if (subscriptionOpt.isPresent()) {
                        Subscription subscription = subscriptionOpt.get();

                        if (subscription.getPlan().equals(plan.getId()) && subscription.isTimeValid(ctx.request().timestamp())) {
                            ctx.setAttribute(ATTR_APPLICATION, subscription.getApplication());
                            ctx.setAttribute(ATTR_SUBSCRIPTION_ID, subscription.getId());
                            return Completable.complete();
                        }
                    }

                    return policy.onInvalidSubscription(ctx);
                } catch (Throwable t) {
                    log.warn("An error occurred during subscription validation", t);
                    return policy.onInvalidSubscription(ctx);
                }
            }
        );
    }
}
