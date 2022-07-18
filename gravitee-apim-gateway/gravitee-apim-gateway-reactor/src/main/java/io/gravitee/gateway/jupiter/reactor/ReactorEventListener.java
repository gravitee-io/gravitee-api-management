package io.gravitee.gateway.jupiter.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorEventListener extends AbstractService<ReactorEventListener> implements EventListener<ReactorEvent, Reactable> {

    private final ReactorHandlerRegistry reactorHandlerRegistry;

    public ReactorEventListener(EventManager eventManager, ReactorHandlerRegistry reactorHandlerRegistry) {
        this.reactorHandlerRegistry = reactorHandlerRegistry;

        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                reactorHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                reactorHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                reactorHandlerRegistry.remove(event.content());
                break;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        reactorHandlerRegistry.clear();
    }
}
