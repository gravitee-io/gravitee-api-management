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
package io.gravitee.gateway.reactive.core.failover;

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;

import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.rxjava3.circuitbreaker.operator.CircuitBreakerOperator;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class FailoverInvoker implements HttpInvoker, Invoker {

    private final HttpInvoker delegate;
    private final Failover failoverConfiguration;

    @VisibleForTesting
    final CircuitBreaker circuitBreaker;

    @VisibleForTesting
    final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public String getId() {
        return "failover-invoker";
    }

    public FailoverInvoker(HttpInvoker delegate, Failover failoverConfiguration, String apiId) {
        this.delegate = delegate;
        this.failoverConfiguration = failoverConfiguration;
        final CircuitBreakerConfig circuitBreakerConfiguration = CircuitBreakerConfig
            .custom()
            .permittedNumberOfCallsInHalfOpenState(1)
            .slowCallDurationThreshold(Duration.of(failoverConfiguration.getSlowCallDuration(), ChronoUnit.MILLIS))
            .minimumNumberOfCalls(failoverConfiguration.getMaxFailures())
            .slidingWindowSize(failoverConfiguration.getMaxFailures())
            .failureRateThreshold(100)
            .slowCallRateThreshold(100)
            .waitDurationInOpenState(Duration.of(failoverConfiguration.getOpenStateDuration(), ChronoUnit.MILLIS))
            .build();
        // Instantiate one global circuit breaker for the API or a circuit breaker registry based on failover configuration
        if (!failoverConfiguration.isPerSubscription()) {
            circuitBreaker = CircuitBreaker.of(apiId, circuitBreakerConfiguration);
            circuitBreakerRegistry = null;
        } else {
            // Prepare the circuit breaker registry with a default configuration
            circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfiguration);
            circuitBreaker = null;
        }
    }

    @Override
    public Completable invoke(ExecutionContext executionContext) {
        return invoke((HttpExecutionContext) executionContext);
    }

    @Override
    public Completable invoke(HttpExecutionContext ctx) {
        final String originalEndpoint = ctx.getAttribute(ATTR_REQUEST_ENDPOINT);
        return Completable
            .defer(() -> {
                // EndpointInvoker overrides the request endpoint. We need to set it back to original state to retry properly
                ctx.setAttribute(ATTR_REQUEST_ENDPOINT, originalEndpoint);
                // Entrypoint connectors skip response handling if there is an error. In the case of a retry, we need to reset the failure.
                ctx.removeInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);
                // Consume body and ignore it. Consuming it with .body() method internally enables caching of chunks, which is mandatory to retry the request in case of failure.
                return ctx.request().body().ignoreElement().andThen(delegate.invoke(ctx));
            })
            .timeout(failoverConfiguration.getSlowCallDuration(), TimeUnit.MILLISECONDS)
            .retry(failoverConfiguration.getMaxRetries())
            .compose(CircuitBreakerOperator.of(circuitBreaker(ctx)))
            .onErrorResumeNext(t -> ctx.interruptWith(new ExecutionFailure(502)));
    }

    private CircuitBreaker circuitBreaker(HttpExecutionContext ctx) {
        if (failoverConfiguration.isPerSubscription()) {
            final String subscription = ctx.getAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID);
            return circuitBreakerRegistry.circuitBreaker(subscription);
        } else {
            return circuitBreaker;
        }
    }
}
