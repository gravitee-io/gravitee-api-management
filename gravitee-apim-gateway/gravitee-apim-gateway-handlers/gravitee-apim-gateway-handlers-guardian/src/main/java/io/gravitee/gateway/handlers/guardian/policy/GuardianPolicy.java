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
package io.gravitee.gateway.handlers.guardian.policy;

import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;

/**
 * Internal policy placed in API flows to execute a deployed Guardian. The chain comes from the Guardian reactor
 * through the {@link GuardianRegistry}; when the Guardian is not deployed, the request goes on untouched.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardianPolicy implements HttpPolicy {

    public static final String POLICY_ID = "guardian-policy";

    private final String id;
    private final GuardianPolicyConfiguration configuration;

    public GuardianPolicy() {
        this(POLICY_ID, null);
    }

    public GuardianPolicy(GuardianPolicyConfiguration configuration) {
        this(POLICY_ID, configuration);
    }

    public GuardianPolicy(String id, GuardianPolicyConfiguration configuration) {
        this.id = id;
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return execute(ctx);
    }

    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return execute(ctx);
    }

    private Completable execute(HttpPlainExecutionContext ctx) {
        String environmentId = ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT);
        GuardianReactor reactor = ctx.getComponent(GuardianRegistry.class).get(configuration.getGuardianId(), environmentId);
        if (reactor == null) {
            ctx.withLogger(log).warn("No Guardian found for id {} on environment {}", configuration.getGuardianId(), environmentId);
            return Completable.complete();
        }
        return reactor.policyChain().execute(ctx);
    }
}
