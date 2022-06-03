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
package io.gravitee.gateway.jupiter.debug.policy;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.hook.PolicyHook;
import io.gravitee.gateway.jupiter.api.hook.SecurityPlanHook;
import io.gravitee.gateway.jupiter.debug.hook.AbstractDebugHook;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.reactivex.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugPolicyHook extends AbstractDebugHook implements PolicyHook, SecurityPlanHook {

    @Override
    public String id() {
        return "hook-debug-policy";
    }

    @Override
    protected Completable pre(final String id, final DebugRequestExecutionContext debugCtx, final ExecutionPhase executionPhase) {
        return debugCtx.prePolicyExecution(id, executionPhase);
    }

    @Override
    protected Completable post(final String id, final DebugRequestExecutionContext debugCtx, final ExecutionPhase executionPhase) {
        return debugCtx.postPolicyExecution();
    }

    @Override
    protected Completable error(
        final String id,
        final DebugRequestExecutionContext debugCtx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        return debugCtx.postPolicyExecution(throwable);
    }

    @Override
    protected Completable interrupt(final String id, final DebugRequestExecutionContext debugCtx, final ExecutionPhase executionPhase) {
        return debugCtx.postPolicyExecution();
    }

    @Override
    protected Completable interruptWith(
        final String id,
        final DebugRequestExecutionContext debugCtx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    ) {
        return debugCtx.postPolicyExecution(failure);
    }
}
