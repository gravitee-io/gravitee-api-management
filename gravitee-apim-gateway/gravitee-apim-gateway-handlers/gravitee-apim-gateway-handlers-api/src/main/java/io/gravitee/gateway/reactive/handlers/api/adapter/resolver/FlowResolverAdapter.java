package io.gravitee.gateway.reactive.handlers.api.adapter.resolver;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.adapter.context.ExecutionContextAdapter;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolver;
import io.reactivex.Flowable;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowResolverAdapter implements FlowResolver {

    private final io.gravitee.gateway.flow.FlowResolver legacyResolver;

    public FlowResolverAdapter(io.gravitee.gateway.flow.FlowResolver legacyResolver) {
        this.legacyResolver = legacyResolver;
    }

    @Override
    public Flowable<Flow> resolve(ExecutionContext<?, ?> ctx) {
        final List<Flow> flows = legacyResolver.resolve(ExecutionContextAdapter.create(ctx));

        if (flows == null || flows.isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(flows);
    }

    @Override
    public Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx) {
        return Flowable.empty();
    }
}
