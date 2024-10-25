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
package io.gravitee.gateway.reactive.handlers.api.security;

import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_FLOW_STAGE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_TOKEN;
import static io.reactivex.rxjava3.core.Completable.defer;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BaseSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.plan.AbstractSecurityPlan;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AbstractSecurityChain} is a special chain dedicated to execute policy associated with plans.
 * The security chain is responsible to create {@link AbstractSecurityPlan} for each plan of the api and executed them in order.
 * Only the first {@link AbstractSecurityPlan} that can handle the current request is executed.
 * The result of the security chain execution depends on this {@link AbstractSecurityPlan} execution.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractSecurityChain<
    P extends AbstractSecurityPlan<? extends BaseSecurityPolicy, C>, C extends BaseExecutionContext
> {

    protected static final String PLAN_UNRESOLVABLE = "GATEWAY_PLAN_UNRESOLVABLE";
    protected static final String PLAN_RESOLUTION_FAILURE = "GATEWAY_PLAN_RESOLUTION_FAILURE";
    protected static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    protected static final String TEMPORARILY_UNAVAILABLE_MESSAGE = "Temporarily Unavailable";
    protected static final String ATTR_INTERNAL_PLAN_RESOLUTION_FAILURE = "securityChain.planResolutionFailure";

    protected static final Single<Boolean> TRUE = Single.just(true);
    protected static final Single<Boolean> FALSE = Single.just(false);
    private final Flowable<P> chain;

    public AbstractSecurityChain(Flowable<P> securityPlans) {
        this.chain = securityPlans;
    }

    protected abstract Completable sendError(C ctx, ExecutionFailure failure);

    protected abstract Single<Boolean> executePlan(P securityPlan, C ctx);

    /**
     * Executes the security chain by executing all the {@link AbstractSecurityPlan}s in an ordered sequence.
     * It's up to each {@link AbstractSecurityPlan} to provide its order. The lower is the order, the highest priority is.
     * The result of the security chain execution depends on the first {@link AbstractSecurityPlan} able to execute the request.
     * If no {@link AbstractSecurityPlan} has been executed because there is no {@link AbstractSecurityPlan} in the chain or none of them can execute the request,
     * then the `sendError` method is called  and the {@link Completable} returns an error.
     *
     * @param ctx the current execution context.
     * @return a {@link Completable} that completes if the request has been successfully handled by a {@link AbstractSecurityPlan} or returns
     * an error if no {@link AbstractSecurityPlan} can execute the request or the {@link AbstractSecurityPlan} failed.
     */
    public Completable execute(C ctx) {
        return defer(() -> {
            if (!Objects.equals(true, ctx.getInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP))) {
                return chain
                    .concatMapSingle(securityPlan -> continueChain(ctx, securityPlan))
                    .any(Boolean::booleanValue)
                    .flatMapCompletable(securityHandled -> {
                        if (Boolean.FALSE.equals(securityHandled)) {
                            Throwable throwable = ctx.getInternalAttribute(ATTR_INTERNAL_PLAN_RESOLUTION_FAILURE);
                            if (throwable != null) {
                                return sendError(
                                    ctx,
                                    new ExecutionFailure(SERVICE_UNAVAILABLE_503)
                                        .key(PLAN_RESOLUTION_FAILURE)
                                        .message(TEMPORARILY_UNAVAILABLE_MESSAGE)
                                );
                            }
                            return sendError(
                                ctx,
                                new ExecutionFailure(UNAUTHORIZED_401).key(PLAN_UNRESOLVABLE).message(UNAUTHORIZED_MESSAGE)
                            );
                        }
                        return Completable.complete();
                    })
                    .doOnSubscribe(disposable -> {
                        log.debug("Executing security chain");
                        ctx.putInternalAttribute(ATTR_INTERNAL_FLOW_STAGE, "security");
                    })
                    .doOnTerminate(() -> {
                        ctx.removeInternalAttribute(ATTR_INTERNAL_FLOW_STAGE);
                        ctx.removeInternalAttribute(ATTR_INTERNAL_PLAN_RESOLUTION_FAILURE);
                        ctx.removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
                    });
            }

            log.debug("Skipping security chain because it has been explicitly required");
            return Completable.complete();
        });
    }

    private Single<Boolean> continueChain(C ctx, P securityPlan) {
        return securityPlan
            .canExecute(ctx)
            .onErrorResumeNext(throwable -> {
                log.error("An error occurred while checking if security plan {} can be executed", securityPlan.id(), throwable);
                ctx.setInternalAttribute(ATTR_INTERNAL_PLAN_RESOLUTION_FAILURE, throwable);
                return FALSE;
            })
            .flatMap(canExecute -> {
                if (Boolean.TRUE.equals(canExecute)) {
                    return executePlan(securityPlan, ctx);
                }
                return FALSE;
            });
    }
}
