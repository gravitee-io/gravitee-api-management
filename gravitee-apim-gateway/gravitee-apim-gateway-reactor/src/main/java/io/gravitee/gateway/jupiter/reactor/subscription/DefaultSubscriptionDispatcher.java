package io.gravitee.gateway.jupiter.reactor.subscription;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.SubscriptionDispatcher;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
import io.gravitee.gateway.jupiter.reactor.subscription.entrypoint.EntrypointManager;
import io.gravitee.gateway.jupiter.reactor.subscription.entrypoint.PushEntrypoint;
import io.gravitee.gateway.jupiter.reactor.subscription.request.SubscriptionExecutionRequestFactory;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.Entrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultSubscriptionDispatcher
    extends AbstractService<SubscriptionDispatcher>
    implements SubscriptionDispatcher, EventListener<ReactorEvent, Object> {

    private final Map<String, Api> apis = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final EntrypointManager entrypointManager;

    private final ReactorHandlerRegistry reactorHandlerRegistry;

    public DefaultSubscriptionDispatcher(
        EventManager eventManager,
        EntrypointManager entrypointManager,
        ReactorHandlerRegistry reactorHandlerRegistry
    ) {
        this.eventManager = eventManager;
        this.entrypointManager = entrypointManager;
        this.reactorHandlerRegistry = reactorHandlerRegistry;
    }

    @Override
    public void onEvent(Event<ReactorEvent, Object> event) {
        // The push gateway dispatcher can manage
        if (event.type() == ReactorEvent.DEPLOY) {
            Api api = (Api) event.content();
        } else if (event.type() == ReactorEvent.UNDEPLOY) {
            Api api = (Api) event.content();
        } else if (event.type() == ReactorEvent.UPDATE) {
            Api api = (Api) event.content();
        } else if (event.type() == ReactorEvent.SUBSCRIPTION) {
            Subscription subscription = (Subscription) event.content();

            // Check and retrieve the API associated to the subscription
            String api = subscription.getApi();

            String configuration = subscription.getConfiguration();

            // Extract the type from the configuration
            String type = "webhook";

            // Lookup the entrypoint plugin
            PushEntrypoint entrypoint = entrypointManager.getEntrypoint(type);

            // Read the configuration according to the json schema

            ApiReactor target = reactorHandlerRegistry.getEntrypoints().iterator().next().target();

            MutableRequestExecutionContext context = new SubscriptionExecutionRequestFactory().create(subscription);

            target.handle(context);
        }
    }

    protected DefaultRequestExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        return new DefaultRequestExecutionContext(request, request.response());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }
}
