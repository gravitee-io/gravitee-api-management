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
package io.gravitee.gateway.debug.utils;

import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.repository.management.model.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Stubs {

    public static DebugApiV2 getADebugApiDefinition() {
        final DebugApiV2 debugApi = new DebugApiV2();

        final Proxy proxy = new Proxy();
        proxy.setVirtualHosts(new ArrayList<>());
        proxy.getVirtualHosts().add(new VirtualHost("/path1"));
        proxy.getVirtualHosts().add(new VirtualHost("/path2"));
        proxy.getVirtualHosts().add(new VirtualHost("/path3"));

        debugApi.setProxy(proxy);

        return debugApi;
    }

    public static DebugApiV4 aDebugApiV4Definition(HttpRequest debugRequest) {
        Api api = Api.builder()
            .name("api")
            .listeners(List.of(HttpListener.builder().paths(List.of(new Path("/path1"), new Path("/path2"), new Path("/path3"))).build()))
            .plans(Map.of("plan", Plan.builder().name("plan").status(PlanStatus.PUBLISHED).build()))
            .build();

        return new DebugApiV4(api, debugRequest);
    }

    public static io.gravitee.repository.management.model.Event getAnEvent(String id, String payload) {
        return getAnEvent(id, payload, new HashMap<>());
    }

    public static io.gravitee.repository.management.model.Event getAnEvent(String id, String payload, Map<String, String> properties) {
        final Event event = new Event();
        event.setProperties(properties);
        event.setId(id);
        event.setPayload(payload);
        return event;
    }

    public static io.gravitee.common.event.Event<ReactorEvent, Reactable> getAReactorEvent(ReactorEvent type, Reactable content) {
        return new io.gravitee.common.event.Event<>() {
            @Override
            public Reactable content() {
                return content;
            }

            @Override
            public ReactorEvent type() {
                return type;
            }
        };
    }
}
