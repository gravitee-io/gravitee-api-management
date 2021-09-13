/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.repository.management.model.Event;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Stubs {

    public static io.gravitee.definition.model.DebugApi getADebugApiDefinition() {
        final io.gravitee.definition.model.DebugApi debugApi = new io.gravitee.definition.model.DebugApi();

        HttpRequest request = new HttpRequest();
        VirtualHost vHost = new VirtualHost();
        vHost.setPath("/path1");
        vHost.setHost("test.gravitee.io");
        request.setVirtualHost(vHost);
        debugApi.setRequest(request);

        final Proxy proxy = new Proxy();
        proxy.setVirtualHosts(new ArrayList<>());
        proxy.getVirtualHosts().add(new VirtualHost("/path1"));
        proxy.getVirtualHosts().add(new VirtualHost("/path2"));
        proxy.getVirtualHosts().add(new VirtualHost("/path3"));

        debugApi.setProxy(proxy);

        return debugApi;
    }

    public static DebugApi getADebugApi(String evtId, io.gravitee.definition.model.DebugApi debugApiDefinition) {
        return new DebugApi(evtId, debugApiDefinition);
    }

    public static io.gravitee.repository.management.model.Event getAnEvent() {
        final Event event = new Event();
        event.setProperties(new HashMap<>());
        return event;
    }

    public static io.gravitee.common.event.Event getAReactorEvent(ReactorEvent type, Reactable content) {
        return new io.gravitee.common.event.Event() {
            @Override
            public Object content() {
                return content;
            }

            @Override
            public Enum type() {
                return type;
            }
        };
    }
}
