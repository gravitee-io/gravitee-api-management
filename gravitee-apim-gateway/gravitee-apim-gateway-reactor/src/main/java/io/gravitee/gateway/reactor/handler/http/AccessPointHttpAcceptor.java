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
package io.gravitee.gateway.reactor.handler.http;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.AbstractAccessPointAcceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AccessPointHttpAcceptor extends AbstractAccessPointAcceptor<HttpAcceptor> implements HttpAcceptor {

    private final String path;
    private final Collection<String> serverIds;

    private final HttpAcceptorFactory httpAcceptorFactory;

    public AccessPointHttpAcceptor(
        final EventManager eventManager,
        final HttpAcceptorFactory httpAcceptorFactory,
        final String environmentId,
        final List<ReactableAccessPoint> initialReactableAccessPoints,
        final String path,
        final ReactorHandler reactor,
        final Collection<String> serverIds
    ) {
        super(reactor, eventManager, environmentId);
        this.httpAcceptorFactory = httpAcceptorFactory;
        this.path = path;
        this.serverIds = serverIds;
        if (initialReactableAccessPoints == null || initialReactableAccessPoints.isEmpty()) {
            // Initialize with a default acceptor with null accessPointHost
            var emptyAccessPoint = new ReactableAccessPoint();
            emptyAccessPoint.setTarget(ReactableAccessPoint.Target.GATEWAY);
            this.initializeAcceptors(List.of(emptyAccessPoint));
        } else {
            this.initializeAcceptors(initialReactableAccessPoints);
        }
    }

    @Override
    protected HttpAcceptor createAcceptor(String accessPointHost) {
        return httpAcceptorFactory.create(accessPointHost, path, apiReactor, serverIds);
    }

    @Override
    protected ReactableAccessPoint.Target getTarget() {
        return ReactableAccessPoint.Target.GATEWAY;
    }

    @Override
    public String path() {
        if (!acceptors.isEmpty()) {
            return acceptors.get(0).path();
        } else {
            return path;
        }
    }

    @Override
    public String host() {
        if (!acceptors.isEmpty()) {
            return acceptors.get(0).host();
        } else {
            return null;
        }
    }

    public List<HttpAcceptor> innerHttpsAcceptors() {
        return acceptors;
    }

    @Override
    public int priority() {
        if (!acceptors.isEmpty()) {
            return acceptors.get(0).priority();
        } else {
            return -1;
        }
    }

    @Override
    public boolean accept(final Request request) {
        if (acceptors.isEmpty()) {
            return false;
        } else if (acceptors.size() == 1) {
            return acceptors.get(0).accept(request);
        } else {
            return acceptors.stream().anyMatch(defaultHttpAcceptor -> defaultHttpAcceptor.accept(request));
        }
    }

    @Override
    public boolean accept(final String host, final String path, final String serverId) {
        if (acceptors.isEmpty()) {
            return false;
        } else if (acceptors.size() == 1) {
            return acceptors.get(0).accept(host, path, serverId);
        } else {
            return acceptors.stream().anyMatch(defaultHttpAcceptor -> defaultHttpAcceptor.accept(host, path, serverId));
        }
    }

    @Override
    public int compareTo(final HttpAcceptor other) {
        if (!acceptors.isEmpty()) {
            return acceptors.get(0).compareTo(other);
        } else {
            return -1;
        }
    }
}
