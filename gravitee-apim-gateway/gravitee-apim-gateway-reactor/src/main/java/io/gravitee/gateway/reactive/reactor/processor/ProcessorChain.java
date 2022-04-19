package io.gravitee.gateway.reactive.reactor.processor;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessorChain {

    private final Logger log = LoggerFactory.getLogger(ProcessorChain.class);

    protected final String id;
    protected Flowable<Processor> processors;

    public ProcessorChain(String id, List<Processor> processors) {
        this.id = id;
        this.processors = Flowable.fromIterable(processors);
    }

    public Completable execute(ExecutionContext<?, ?> ctx) {
        log.debug("Executing processor chain {}", id);

        return processors.flatMapCompletable(processor -> next(ctx, processor), false, 1);
    }

    private Completable next(ExecutionContext<?, ?> ctx, Processor processor) {
        if (ctx.isInterrupted()) {
            return Completable.complete();
        }

        log.debug("Executing processor {}", processor.getId());

        return processor.execute(ctx);
    }

    public String getId() {
        return id;
    }
}
