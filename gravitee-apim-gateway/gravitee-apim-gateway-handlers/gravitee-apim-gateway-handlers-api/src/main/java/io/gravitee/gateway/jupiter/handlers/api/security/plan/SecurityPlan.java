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
import io.gravitee.gateway.jupiter.api.policy.SecurityToken;
import io.reactivex.Completable;
import io.reactivex.Maybe;
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

    protected static final Maybe<Boolean> TRUE = Maybe.just(true);
    protected static final Maybe<Boolean> FALSE = Maybe.just(false);
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
            .extractSecurityToken(ctx)
            .flatMap(
                securityToken ->
                    matchSelectionRule(ctx)
                        .flatMap(
                            matches -> {
                                if (!matches) {
                                    return FALSE;
                                }
                                return validateSubscription(ctx, securityToken);
                            }
                        )
            )
            .defaultIfEmpty(false)
            .toSingle();
    }

    /**
     * Invokes the policy's <code>onRequest</code> method.
     *
     * @param ctx the current execution context.
     * @return a {@link Completable} that completes when the security policy has been successfully executed or returns an error otherwise.
     */
    public Completable execute(RequestExecutionContext ctx) {
        return policy.onRequest(ctx).doOnSubscribe(disposable -> ctx.setAttribute(ATTR_PLAN, plan.getId()));
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

    private Maybe<Boolean> matchSelectionRule(RequestExecutionContext ctx) {
        if (selectionRule == null || selectionRule.isEmpty()) {
            return TRUE;
        }

        return ctx.getTemplateEngine().eval(selectionRule, Boolean.class);
    }

    private Maybe<Boolean> validateSubscription(RequestExecutionContext ctx, SecurityToken securityToken) {
        if (!policy.requireSubscription()) {
            return TRUE;
        }

        return Maybe.defer(
            () -> {
                try {
                    SubscriptionService subscriptionService = ctx.getComponent(SubscriptionService.class);
                    String api = ctx.getAttribute(ATTR_API);

                    Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(api, securityToken, plan.getId());

                    if (subscriptionOpt.isPresent()) {
                        Subscription subscription = subscriptionOpt.get();

                        if (plan.getId().equals(subscription.getPlan()) && subscription.isTimeValid(ctx.request().timestamp())) {
                            ctx.setAttribute(ATTR_APPLICATION, subscription.getApplication());
                            ctx.setAttribute(ATTR_SUBSCRIPTION_ID, subscription.getId());
                            return TRUE;
                        }
                    }
                    return FALSE;
                } catch (Throwable t) {
                    log.warn("An error occurred during subscription validation", t);
                    return FALSE;
                }
            }
        );
    }
}
