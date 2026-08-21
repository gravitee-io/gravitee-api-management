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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.PolicyWarmupException;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.api.policy.http.WarmablePolicy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.policy.api.PolicyConfiguration;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;

/**
 * @author Guillaume Lamirand (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpPolicyFactory implements PolicyFactory {

    private static final long WARMUP_TIMEOUT_SECONDS = 30;

    /**
     * Holds the {@link DeploymentContext} of the API currently being deployed, so that policies
     * implementing {@link WarmablePolicy} can be warmed up inline while their policy chain is built.
     * Deployment is single-threaded, so a {@code ThreadLocal} is sufficient and avoids changing the
     * factory's public contract.
     */
    private static final ThreadLocal<DeploymentContext> WARMUP_CONTEXT = new ThreadLocal<>();

    /**
     * Marks the start of a deploy-time warmup phase on the current thread. Policies created while a
     * context is bound are warmed up immediately.
     */
    public static void beginWarmup(final DeploymentContext deploymentContext) {
        WARMUP_CONTEXT.set(deploymentContext);
    }

    /**
     * Ends the deploy-time warmup phase on the current thread.
     */
    public static void endWarmup() {
        WARMUP_CONTEXT.remove();
    }

    protected final Configuration configuration;
    protected final PolicyPluginFactory policyPluginFactory;
    protected final io.gravitee.gateway.policy.PolicyFactory v3PolicyFactory;
    protected final ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter;

    private final long policyTimeoutMs;

    public HttpPolicyFactory(
        final Configuration configuration,
        final PolicyPluginFactory policyPluginFactory,
        final ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter
    ) {
        this(configuration, policyPluginFactory, filter, 0L);
    }

    public HttpPolicyFactory(
        final Configuration configuration,
        final PolicyPluginFactory policyPluginFactory,
        final ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter,
        final long policyTimeoutMs
    ) {
        this.configuration = configuration;
        this.policyPluginFactory = policyPluginFactory;
        this.filter = filter;
        this.policyTimeoutMs = policyTimeoutMs;
        // V3 policy factory doesn't need condition evaluator anymore as condition is directly handled by v4 engine.
        this.v3PolicyFactory = new io.gravitee.gateway.policy.impl.PolicyFactoryImpl(policyPluginFactory);
    }

    @Override
    public boolean accept(PolicyManifest policyManifest) {
        // DefaultPolicyFactory accept any kind of policy
        return true;
    }

    @Override
    public HttpPolicy create(
        final ExecutionPhase executionPhase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        return createPolicy(executionPhase, policyManifest, policyConfiguration, policyMetadata);
    }

    protected HttpPolicy createPolicy(
        final ExecutionPhase phase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        HttpPolicy policy = null;

        if (HttpPolicy.class.isAssignableFrom(policyManifest.policy())) {
            policy = (HttpPolicy) policyPluginFactory.create(policyManifest.policy(), policyConfiguration);
        } else if (phase == ExecutionPhase.REQUEST || phase == ExecutionPhase.RESPONSE) {
            StreamType streamType = phase == ExecutionPhase.REQUEST ? StreamType.ON_REQUEST : StreamType.ON_RESPONSE;
            if (policyManifest.accept(streamType)) {
                io.gravitee.gateway.policy.Policy v3Policy = v3PolicyFactory.create(
                    streamType,
                    policyManifest,
                    policyConfiguration,
                    policyMetadata
                );
                // PEN-88: evaluate Schedulers.io() lazily here (at API-deploy time, after
                // HttpProtocolVerticle has registered its RxJavaPlugins handler) so that
                // Schedulers.io() correctly resolves to RxHelper.blockingScheduler(vertx),
                // which preserves the Vert.x context through executeBlocking.
                Scheduler workerScheduler = policyTimeoutMs > 0 ? Schedulers.io() : null;
                policy = new PolicyAdapter(v3Policy, workerScheduler);
            }
        } else {
            throw new IllegalArgumentException(
                String.format("Cannot create policy instance with [phase=%s, policy=%s]", phase, policyManifest.id())
            );
        }

        warmupIfNeeded(policyManifest, policy);

        policy = decoratePolicy(policyMetadata, policy);

        return policy;
    }

    private void warmupIfNeeded(final PolicyManifest policyManifest, final HttpPolicy policy) {
        final DeploymentContext deploymentContext = WARMUP_CONTEXT.get();
        if (deploymentContext == null || !(policy instanceof WarmablePolicy warmable)) {
            return;
        }

        final String policyId = policyManifest.id();
        try {
            log.debug("Warming up policy '{}'", policyId);
            warmable
                .warmup(deploymentContext)
                .subscribeOn(Schedulers.io())
                .timeout(WARMUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .blockingAwait();
            log.info("Policy '{}' warmed up", policyId);
        } catch (Exception e) {
            log.error("Failed to warm up policy '{}'", policyId, e);
            throw new PolicyWarmupException(policyId, "Failed to warm up policy '" + policyId + "': " + e.getMessage(), e);
        }
    }

    protected HttpPolicy decoratePolicy(PolicyMetadata policyMetadata, HttpPolicy policy) {
        if (policy != null) {
            final String condition = policyMetadata.getCondition();

            // Avoid creating a conditional policy if no condition or message condition is defined.
            if (isNotBlank(condition)) {
                policy = new HttpConditionalPolicy(policy, condition, filter);
            }
        }
        return policy;
    }

    @Override
    public void cleanup(PolicyManifest policyManifest) {
        policyPluginFactory.cleanup(policyManifest);
    }

    protected boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
