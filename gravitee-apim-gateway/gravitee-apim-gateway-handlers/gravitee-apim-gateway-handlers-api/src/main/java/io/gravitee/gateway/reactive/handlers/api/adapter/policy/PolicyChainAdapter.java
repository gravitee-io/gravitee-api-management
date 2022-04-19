package io.gravitee.gateway.reactive.handlers.api.adapter.policy;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.policy.api.PolicyResult;
import io.reactivex.CompletableEmitter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyChainAdapter implements io.gravitee.policy.api.PolicyChain {

    private final ExecutionContext<?, ?> ctx;
    private final CompletableEmitter emitter;

    public PolicyChainAdapter(ExecutionContext<?, ?> ctx, CompletableEmitter emitter) {
        this.ctx = ctx;
        this.emitter = emitter;
    }

    @Override
    public void doNext(Request request, Response response) {
        emitter.onComplete();
    }

    @Override
    public void streamFailWith(PolicyResult policyResult) {
        failWith(policyResult);
    }

    @Override
    public void failWith(PolicyResult policyResult) {
        // TODO: ExecutionFailure.
        ctx.response().status(policyResult.statusCode());

       // ctx.response().content(Buffer.buffer(policyResult.message()));
        ctx.interrupt();
        emitter.onComplete();
    }
}
