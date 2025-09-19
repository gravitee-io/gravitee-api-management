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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.accesspoint.AccessPointEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AccessPointHttpAcceptor implements HttpAcceptor, EventListener<AccessPointEvent, ReactableAccessPoint> {

    private final String environmentId;
    private final String path;
    private final ReactorHandler reactor;
    private final Collection<String> serverIds;
    private List<HttpAcceptor> httpAcceptors;
    private EventManager eventManager;

    public AccessPointHttpAcceptor(
        final EventManager eventManager,
        final String environmentId,
        final List<ReactableAccessPoint> reactableAccessPoints,
        final String path,
        final ReactorHandler reactor,
        final Collection<String> serverIds
    ) {
        this.environmentId = environmentId;
        this.path = path;
        this.reactor = reactor;
        this.serverIds = serverIds;
        if (reactableAccessPoints == null || reactableAccessPoints.isEmpty()) {
            this.httpAcceptors = new CopyOnWriteArrayList<>();
            this.httpAcceptors.add(new DefaultHttpAcceptor(null, path, reactor, serverIds));
        } else {
            this.httpAcceptors = reactableAccessPoints
                .stream()
                .<HttpAcceptor>map(reactableAccessPoint ->
                    new DefaultHttpAcceptor(reactableAccessPoint.getHost(), path, reactor, serverIds)
                )
                .collect(toCollection(CopyOnWriteArrayList::new));
        }
        this.eventManager = eventManager;
        this.eventManager.subscribeForEvents(this, AccessPointEvent.class);
    }

    @Override
    public void clear() {
        this.eventManager.unsubscribeForEvents(this, AccessPointEvent.class);
        if (this.httpAcceptors != null) {
            this.httpAcceptors.forEach(Acceptor::clear);
        }
    }

    @Override
    public void onEvent(final Event<AccessPointEvent, ReactableAccessPoint> event) {
        if (environmentId.equals(event.content().getEnvironmentId())) {
            DefaultHttpAcceptor httpAcceptor = new DefaultHttpAcceptor(event.content().getHost(), path, reactor, serverIds);
            AccessPointEvent type = event.type();
            if (Objects.requireNonNull(type) == AccessPointEvent.DEPLOY) {
                if (httpAcceptors == null) {
                    httpAcceptors = new CopyOnWriteArrayList<>();
                }
                httpAcceptors.add(httpAcceptor);
            } else if (type == AccessPointEvent.UNDEPLOY && httpAcceptors != null) {
                httpAcceptors.remove(httpAcceptor);
            }
        }
    }

    @Override
    public ReactorHandler reactor() {
        return reactor;
    }

    @Override
    public String path() {
        if (!httpAcceptors.isEmpty()) {
            return httpAcceptors.get(0).path();
        } else {
            return path;
        }
    }

    @Override
    public String host() {
        if (!httpAcceptors.isEmpty()) {
            return httpAcceptors.get(0).host();
        } else {
            return null;
        }
    }

    public List<HttpAcceptor> innerHttpsAcceptors() {
        return httpAcceptors;
    }

    @Override
    public int priority() {
        if (!httpAcceptors.isEmpty()) {
            return httpAcceptors.get(0).priority();
        } else {
            return -1;
        }
    }

    @Override
    public boolean accept(final Request request) {
        if (httpAcceptors.isEmpty()) {
            return false;
        } else if (httpAcceptors.size() == 1) {
            return httpAcceptors.get(0).accept(request);
        } else {
            return httpAcceptors.stream().anyMatch(defaultHttpAcceptor -> defaultHttpAcceptor.accept(request));
        }
    }

    @Override
    public boolean accept(final String host, final String path, final String serverId) {
        if (httpAcceptors.isEmpty()) {
            return false;
        } else if (httpAcceptors.size() == 1) {
            return httpAcceptors.get(0).accept(host, path, serverId);
        } else {
            return httpAcceptors.stream().anyMatch(defaultHttpAcceptor -> defaultHttpAcceptor.accept(host, path, serverId));
        }
    }

    @Override
    public int compareTo(final HttpAcceptor other) {
        if (!httpAcceptors.isEmpty()) {
            return httpAcceptors.get(0).compareTo(other);
        } else {
            return -1;
        }
    }
}
