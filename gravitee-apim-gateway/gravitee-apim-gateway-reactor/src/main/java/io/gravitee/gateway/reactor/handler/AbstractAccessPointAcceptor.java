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
package io.gravitee.gateway.reactor.handler;

import static java.util.stream.Collectors.toCollection;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.accesspoint.AccessPointEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author GraviteeSource Team
 */
public abstract class AbstractAccessPointAcceptor<T extends Acceptor<T>>
    implements Acceptor<T>, EventListener<AccessPointEvent, ReactableAccessPoint> {

    protected final ReactorHandler apiReactor;
    private final String environmentId;
    private final EventManager eventManager;
    protected List<T> acceptors;

    public AbstractAccessPointAcceptor(ReactorHandler apiReactor, final EventManager eventManager, final String environmentId) {
        this.apiReactor = apiReactor;
        this.environmentId = environmentId;
        this.eventManager = eventManager;
        this.eventManager.subscribeForEvents(this, AccessPointEvent.class);
    }

    protected void initializeAcceptors(final List<ReactableAccessPoint> initialReactableAccessPoints) {
        this.acceptors = initialReactableAccessPoints
            .stream()
            .filter(reactableAccessPoint -> reactableAccessPoint.getTarget() == getTarget())
            .map((ReactableAccessPoint reactableAccessPoint) -> createAcceptor(reactableAccessPoint.getHost()))
            .collect(toCollection(CopyOnWriteArrayList::new));
    }

    protected abstract T createAcceptor(String accessPointHost);

    protected abstract ReactableAccessPoint.Target getTarget();

    @Override
    public void clear() {
        this.eventManager.unsubscribeForEvents(this, AccessPointEvent.class);
        if (this.acceptors != null) {
            this.acceptors.forEach(Acceptor::clear);
        }
    }

    @Override
    public void onEvent(Event<AccessPointEvent, ReactableAccessPoint> event) {
        if (!environmentId.equals(event.content().getEnvironmentId())) {
            return;
        }
        if (event.content().getTarget() != getTarget()) {
            return;
        }

        T acceptor = createAcceptor(event.content().getHost());
        AccessPointEvent type = event.type();

        if (Objects.requireNonNull(type) == AccessPointEvent.DEPLOY) {
            if (acceptors == null) {
                acceptors = new CopyOnWriteArrayList<>();
            }
            acceptors.add(acceptor);
        } else if (type == AccessPointEvent.UNDEPLOY && acceptors != null) {
            acceptors.remove(acceptor);
        }
    }

    @Override
    public ReactorHandler reactor() {
        return apiReactor;
    }
}
