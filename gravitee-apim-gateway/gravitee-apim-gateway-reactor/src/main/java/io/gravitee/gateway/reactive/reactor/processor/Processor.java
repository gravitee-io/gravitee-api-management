package io.gravitee.gateway.reactive.reactor.processor;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.reactivex.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Processor {

    String getId();

    Completable execute(ExecutionContext<?, ?> ctx);
}
