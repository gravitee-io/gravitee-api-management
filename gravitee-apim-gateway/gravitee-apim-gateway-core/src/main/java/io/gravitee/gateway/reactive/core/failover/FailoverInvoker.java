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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.FailoverRequestAdapter;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
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
import org.springframework.util.StringUtils;

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
    @Deprecated(since = "4.11.0", forRemoval = true)
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
        final AtomicInteger totalAttempts = new AtomicInteger(0);
        final AtomicReference<List<String>> endpointRotation = new AtomicReference<>();
        final AtomicReference<String> firstFailedEndpoint = new AtomicReference<>();
        final AtomicReference<RequestSnapshot> snapshotRef = new AtomicReference<>();

        return captureRequestState(ctx, snapshotRef).andThen(
            Completable.defer(() -> {
                int attempt = totalAttempts.getAndIncrement();

                // On retry, capture the endpoint that failed in the previous attempt and restore the request to its initial state
                if (attempt > 0) {
                    firstFailedEndpoint.compareAndSet(null, resolveCurrentEndpointName(ctx));
                    restoreContextState(ctx, snapshotRef.get());
                }

                // EndpointInvoker overrides the request endpoint. We need to set it back to original state to retry properly
                ctx.setAttribute(ATTR_REQUEST_ENDPOINT, originalEndpoint);
                // Entrypoint connectors skip response handling if there is an error. In the case of a retry, we need to reset the failure.
                ctx.removeInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);

                forceNextEndpoint(ctx, attempt, endpointRotation);

                return delegate.invoke(ctx).andThen(evaluateFailureCondition(ctx));
            })
                .timeout(failoverConfiguration.getSlowCallDuration(), TimeUnit.MILLISECONDS)
                .retry(failoverConfiguration.getMaxRetries())
                .compose(CircuitBreakerOperator.of(circuitBreaker(ctx)))
                .onErrorResumeNext(t -> ctx.interruptWith(new ExecutionFailure(502).cause(t)))
                .doFinally(() -> recordFailoverMetrics(ctx, totalAttempts.get(), firstFailedEndpoint.get()))
        );
    }

    /**
     * Forces the invocation to switch to the next available endpoint in the rotation if the configured
     * failover settings permit this behavior, and the current attempt is not the first one.
     * If the rotation cannot be computed (e.g., no managed endpoint in context, or single endpoint in the group),
     * the default load balancer selection is preserved.
     *
     * @param ctx the {@link HttpExecutionContext} of the current invocation, which provides access
     *            to request and plugin execution details.
     * @param attempt the current invocation attempt count. A value of 0 indicates the first attempt,
     *                and no endpoint rotation will occur.
     * @param endpointRotation a mutable {@link AtomicReference} holding the list of available
     *                         endpoints for failover. The list is lazily initialized on the first
     *                         invocation.
     */
    private void forceNextEndpoint(HttpExecutionContext ctx, int attempt, AtomicReference<List<String>> endpointRotation) {
        if (!failoverConfiguration.isForceNextEndpointOnFailure()) {
            return;
        }
        if (endpointManager == null) {
            ctx.withLogger(log).warn("Endpoint manager is null, cannot force next endpoint");
            return;
        }
        if (attempt == 0) {
            // First attempt: let the LB pick the endpoint normally
            ctx.withLogger(log).debug("First attempt, using load balancer to pick endpoint");
            return;
        }

        List<String> rotation = initEndpointRotation(ctx, endpointRotation);

        if (rotation != null && !rotation.isEmpty()) {
            int index = (attempt - 1) % rotation.size();
            ctx.setAttribute(ATTR_REQUEST_ENDPOINT, rotation.get(index) + ":");
        }
    }

    /**
     * Initializes the endpoint rotation for failover handling. If the endpoint rotation list is not
     * already initialized, this method builds the endpoint rotation from the provided execution context
     * and sets it in the given atomic reference. The rotation ensures the endpoints are processed
     * cyclically in failover scenarios.
     *
     * @param ctx the {@link HttpExecutionContext} providing the invocation context,
     *            including attributes and execution details.
     * @param endpointRotation a mutable {@link AtomicReference} holding the list of available
     *                         endpoints for failover. The list is lazily initialized if null.
     * @return the initialized or existing list of endpoint names in rotation order, or {@code null}
     *         if no endpoints are available or could be determined from the context.
     */
    private List<String> initEndpointRotation(HttpExecutionContext ctx, AtomicReference<List<String>> endpointRotation) {
        List<String> rotation = endpointRotation.get();
        if (rotation == null) {
            rotation = buildEndpointRotation(ctx);
            endpointRotation.set(rotation);
        }
        return rotation;
    }

    /**
     * Builds the rotation of endpoints for failover handling based on the provided execution context.
     * The rotation is calculated by starting from the endpoint immediately following the currently
     * selected one, cycling through the list of endpoints, and including the selected endpoint at the end.
     * If the currently selected endpoint cannot be determined, or if there's only one endpoint available,
     * the rotation will not be created.
     *
     * @param ctx the {@link HttpExecutionContext} providing the invocation context and attributes.
     *            It should contain the currently selected endpoint as an internal attribute.
     * @return a list of endpoint names in the calculated rotation order, or {@code null} if no
     *         valid endpoints are available or the rotation could not be determined.
     */
    private List<String> buildEndpointRotation(HttpExecutionContext ctx) {
        ManagedEndpoint selectedEndpoint = ctx.getInternalAttribute(ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT);
        if (selectedEndpoint == null) {
            return null;
        }

        String selectedEndpointName = selectedEndpoint.getDefinition().getName();

        // Use the group definition to get endpoints in their declaration order
        List<? extends io.gravitee.definition.model.v4.endpointgroup.Endpoint> definedEndpoints = selectedEndpoint
            .getGroup()
            .getDefinition()
            .getEndpoints();
        if (definedEndpoints == null || definedEndpoints.size() <= 1) {
            return null;
        }

        List<String> endpointNames = new ArrayList<>(definedEndpoints.size());
        int selectedIndex = -1;
        for (int i = 0; i < definedEndpoints.size(); i++) {
            String name = definedEndpoints.get(i).getName();
            endpointNames.add(name);
            if (name.equals(selectedEndpointName)) {
                selectedIndex = i;
            }
        }

        if (selectedIndex == -1) {
            return null;
        }

        // Build full rotation starting from the endpoint after the selected one, cycling back to include it
        List<String> rotation = new ArrayList<>(endpointNames.size());
        for (int i = 1; i <= endpointNames.size(); i++) {
            int idx = (selectedIndex + i) % endpointNames.size();
            rotation.add(endpointNames.get(idx));
        }
        return rotation;
    }

    private Completable evaluateFailureCondition(HttpExecutionContext ctx) {
        String condition = failoverConfiguration.getFailureCondition();
        if (!StringUtils.hasText(condition)) {
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

    private String resolveCurrentEndpointName(HttpExecutionContext ctx) {
        ManagedEndpoint endpoint = ctx.getInternalAttribute(ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT);
        return endpoint != null ? endpoint.getDefinition().getName() : null;
    }

    private void recordFailoverMetrics(HttpExecutionContext ctx, int totalAttempts, String firstFailed) {
        int failoverCount = totalAttempts - 1;
        if (failoverCount <= 0) {
            return;
        }
        ctx.metrics().putAdditionalMetric("long_failover_count", (long) failoverCount);
        if (firstFailed != null) {
            ctx.metrics().putAdditionalKeywordMetric("keyword_failover_first-failed-endpoint", firstFailed);
        }
        ManagedEndpoint successfulEndpoint = ctx.getInternalAttribute(ATTR_INTERNAL_FAILOVER_MANAGED_ENDPOINT);
        if (successfulEndpoint != null) {
            ctx.metrics().putAdditionalKeywordMetric("keyword_failover_successful-endpoint", successfulEndpoint.getDefinition().getName());
        }
    }

    private CircuitBreaker circuitBreaker(HttpExecutionContext ctx) {
        if (failoverConfiguration.isPerSubscription()) {
            final String subscription = ctx.getAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID);
            return circuitBreakerRegistry.circuitBreaker(subscription);
        } else {
            return circuitBreaker;
        }
    }

    /**
     * Captures the current request state (path, headers, body) so it can be restored on retry.
     * Calling {@code body()} also activates internal chunk caching, which is mandatory to replay the request.
     */
    private Completable captureRequestState(HttpExecutionContext ctx, AtomicReference<RequestSnapshot> snapshotRef) {
        return FailoverRequestAdapter.forRequest(ctx.request())
            .body()
            // Body present: capture path, headers, and body for full replay
            .doOnSuccess(body -> snapshotRef.compareAndSet(null, RequestSnapshot.withBody(ctx, body)))
            // No body (e.g. GET): capture path and headers only
            .doOnComplete(() -> snapshotRef.compareAndSet(null, RequestSnapshot.headersOnly(ctx)))
            .ignoreElement();
    }

    /**
     * Restores the request to its initial state captured by {@link #captureRequestState}.
     * Removes response headers to avoid interference with subsequent response processing.
     */
    private void restoreContextState(HttpExecutionContext ctx, RequestSnapshot snapshot) {
        if (ctx.request() instanceof HttpRequestInternal httpRequestInternal) {
            httpRequestInternal.pathInfo(snapshot.pathInfo());
        }

        HttpHeaders currentHeaders = ctx.request().headers();
        currentHeaders.clear();
        for (var entry : snapshot.headers()) {
            currentHeaders.add(entry.getKey(), entry.getValue());
        }

        if (snapshot.body() != null) {
            ctx.request().body(snapshot.body());
        }

        if (ctx.response() != null && ctx.response().headers() != null) {
            ctx.response().headers().clear();
        }
    }

    @VisibleForTesting
    record RequestSnapshot(String pathInfo, HttpHeaders headers, Buffer body) {
        /** Captures request state including the body content for replay on retry. */
        static RequestSnapshot withBody(HttpExecutionContext ctx, Buffer body) {
            return new RequestSnapshot(ctx.request().pathInfo(), HttpHeaders.create(ctx.request().headers()), body);
        }

        /** Captures request state when no body is present (e.g. GET requests). */
        static RequestSnapshot headersOnly(HttpExecutionContext ctx) {
            return new RequestSnapshot(ctx.request().pathInfo(), HttpHeaders.create(ctx.request().headers()), null);
        }
    }
}
