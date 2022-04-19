package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.reactivex.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface FlowResolver {
    /**
     * Resolve the flows against the current context.
     * Each flow of the initial flow list is filtered thanks to the provided evaluator before being returned.
     *
     * Initial flow list must be provided by a concrete implementation of {@link #provideFlows(ExecutionContext)}.
     *
     * @param ctx the current context.
     * @return a {@link Flowable} of {@link Flow} that have passed the filter step.
     */
     Flowable<Flow> resolve(ExecutionContext<?, ?> ctx);

    /**
     * Provides the initial list of flows.
     * It's up to the implementation to decide to use the current execution context or not.
     * The implementation can decide to cache the list of flows or evaluate it against the current context.
     *
     * @param ctx the current context
     * @return a {@link Flowable} of {@link Flow}.
     */
     Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx);
}
