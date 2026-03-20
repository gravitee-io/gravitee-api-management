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
import static io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker.ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT;

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
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.CustomLog;

@CustomLog
public class FailoverInvoker implements HttpInvoker, Invoker {

    private final HttpInvoker delegate;
    private final Failover failoverConfiguration;
    private final EndpointManager endpointManager;

    @VisibleForTesting
    final CircuitBreaker circuitBreaker;

    @VisibleForTesting
    final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public String getId() {
        return "failover-invoker";
    }

    /**
     * Constructs a new instance of {@code FailoverInvoker}.
     * This constructor is deprecated and marked for removal. Use the alternative constructor
     * that includes an {@code EndpointManager} parameter instead.
     *
     * @param delegate the {@link HttpInvoker} delegate used for HTTP invocations.
     * @param failoverConfiguration the {@link Failover} configuration that specifies the failover behavior and settings.
     * @param apiId the identifier of the API for which this invoker is instantiated.
     */
    @Deprecated(forRemoval = true)
    public FailoverInvoker(HttpInvoker delegate, Failover failoverConfiguration, String apiId) {
        this(delegate, failoverConfiguration, apiId, null);
    }

    public FailoverInvoker(HttpInvoker delegate, Failover failoverConfiguration, String apiId, EndpointManager endpointManager) {
        this.delegate = delegate;
        this.failoverConfiguration = failoverConfiguration;
        this.endpointManager = endpointManager;
        final CircuitBreakerConfig circuitBreakerConfiguration = CircuitBreakerConfig.custom()
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
        final AtomicInteger attemptIndex = new AtomicInteger(0);
        final AtomicReference<List<String>> capturedEndpoints = new AtomicReference<>();

        return Completable.defer(() -> {
            // EndpointInvoker overrides the request endpoint. We need to set it back to original state to retry properly
            ctx.setAttribute(ATTR_REQUEST_ENDPOINT, originalEndpoint);
            // Entrypoint connectors skip response handling if there is an error. In the case of a retry, we need to reset the failure.
            ctx.removeInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);

            forceNextEndpoint(ctx, attemptIndex, capturedEndpoints);

            // Consume body and ignore it. Consuming it with .body() method internally enables caching of chunks, which is mandatory to retry the request in case of failure.
            return ctx.request().body().ignoreElement().andThen(delegate.invoke(ctx)).andThen(evaluateFailureCondition(ctx));
        })
            .timeout(failoverConfiguration.getSlowCallDuration(), TimeUnit.MILLISECONDS)
            .retry(failoverConfiguration.getMaxRetries())
            .compose(CircuitBreakerOperator.of(circuitBreaker(ctx)))
            .onErrorResumeNext(t -> ctx.interruptWith(new ExecutionFailure(502).cause(t)));
    }

    private void forceNextEndpoint(HttpExecutionContext ctx, AtomicInteger attemptIndex, AtomicReference<List<String>> capturedEndpoints) {
        if (!failoverConfiguration.isForceNextEndpointOnFailure()) {
            return;
        }
        if (endpointManager == null) {
            ctx.withLogger(log).warn("Endpoint manager is null, cannot force next endpoint");
            return;
        }

        int attempt = attemptIndex.getAndIncrement();
        if (attempt == 0) {
            // First attempt: let the LB pick the endpoint normally, we'll capture it after invocation
            // Capture happens on retry (attempt > 0), using the ManagedEndpoint set by HttpEndpointInvoker
            return;
        }

        List<String> endpoints = capturedEndpoints.get();
        if (endpoints == null) {
            // Build the ordered list of endpoints starting from the one after the initially selected endpoint
            endpoints = buildEndpointRotation(ctx);
            capturedEndpoints.set(endpoints);
        }

        if (endpoints != null && !endpoints.isEmpty()) {
            // Select the next endpoint in the rotation (attempt-1 because attempt 0 was the LB pick)
            int index = (attempt - 1) % endpoints.size();
            ctx.setAttribute(ATTR_REQUEST_ENDPOINT, endpoints.get(index) + ":");
        }
    }

    private List<String> buildEndpointRotation(HttpExecutionContext ctx) {
        ManagedEndpoint selectedEndpoint = ctx.getInternalAttribute(ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT);
        if (selectedEndpoint == null) {
            return null;
        }

        String selectedGroupName = selectedEndpoint.getGroup().getDefinition().getName();
        String selectedEndpointName = selectedEndpoint.getDefinition().getName();

        // Get all endpoints from the same group
        List<ManagedEndpoint> allEndpoints = endpointManager.all();
        List<String> groupEndpointNames = new ArrayList<>();
        int selectedIndex = -1;

        for (ManagedEndpoint ep : allEndpoints) {
            if (ep.getGroup().getDefinition().getName().equals(selectedGroupName)) {
                String name = ep.getDefinition().getName();
                if (name.equals(selectedEndpointName)) {
                    selectedIndex = groupEndpointNames.size();
                }
                groupEndpointNames.add(name);
            }
        }

        if (groupEndpointNames.size() <= 1 || selectedIndex == -1) {
            return null;
        }

        // Build rotation starting from the endpoint after the selected one
        List<String> rotation = new ArrayList<>(groupEndpointNames.size() - 1);
        for (int i = 1; i < groupEndpointNames.size(); i++) {
            int idx = (selectedIndex + i) % groupEndpointNames.size();
            rotation.add(groupEndpointNames.get(idx));
        }
        return rotation;
    }

    private Completable evaluateFailureCondition(HttpExecutionContext ctx) {
        String condition = failoverConfiguration.getFailureCondition();
        if (condition == null || condition.isEmpty()) {
            return Completable.complete();
        }
        return ctx
            .getTemplateEngine()
            .eval(condition, Boolean.class)
            .flatMapCompletable(isFailure -> {
                if (Boolean.TRUE.equals(isFailure)) {
                    return Completable.error(new FailoverConditionMatchException("Failover condition matched: " + condition));
                }
                return Completable.complete();
            })
            .onErrorComplete(t -> !(t instanceof FailoverConditionMatchException));
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
