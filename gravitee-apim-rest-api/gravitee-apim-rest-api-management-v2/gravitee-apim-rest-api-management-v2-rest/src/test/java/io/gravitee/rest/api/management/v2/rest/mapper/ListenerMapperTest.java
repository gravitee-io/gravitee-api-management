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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fixtures.ListenerFixtures;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ListenerMapperTest {

    private final ListenerMapper listenerMapper = Mappers.getMapper(ListenerMapper.class);

    @Test
    void shouldMapToListenerEntityV4() {
        var httpListener = ListenerFixtures.aHttpListener();
        var subscriptionListener = ListenerFixtures.aSubscriptionListener();
        var tcpListener = ListenerFixtures.aTcpListener();

        var listenerEntityV4List = listenerMapper.mapToListenerEntityV4List(
            List.of(new Listener(httpListener), new Listener(subscriptionListener), new Listener(tcpListener))
        );

        assertThat(listenerEntityV4List).isNotNull();
        assertThat(listenerEntityV4List).asList().hasSize(3);

        var httpListenerEntityV4 = listenerEntityV4List.get(0);
        assertThat(httpListenerEntityV4.getType()).isEqualTo(ListenerType.HTTP);
        assertThat(httpListenerEntityV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(httpListenerEntityV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListener.getServers().size())
            .contains(httpListener.getServers().toArray());
        assertThat(httpListenerEntityV4).isNotNull().isInstanceOf(HttpListener.class);

        assertThat(((HttpListener) httpListenerEntityV4).getPaths()).isNotNull().asList().hasSize(1);
        assertThat(((HttpListener) httpListenerEntityV4).getPaths().get(0)).isNotNull();
        assertThat(((HttpListener) httpListenerEntityV4).getPaths().get(0).getHost()).isEqualTo(httpListener.getPaths().get(0).getHost());
        assertThat(((HttpListener) httpListenerEntityV4).getPaths().get(0).getPath()).isEqualTo(httpListener.getPaths().get(0).getPath());
        assertThat(((HttpListener) httpListenerEntityV4).getPaths().get(0).isOverrideAccess())
            .isEqualTo(httpListener.getPaths().get(0).getOverrideAccess());
        assertThat(((HttpListener) httpListenerEntityV4).getPathMappings()).isEqualTo(new HashSet<>(httpListener.getPathMappings()));
        assertThat(((HttpListener) httpListenerEntityV4).getPathMappingsPattern()).isNotNull();
        assertThat(((HttpListener) httpListenerEntityV4).getPathMappingsPattern().get("/test")).isNotNull().isInstanceOf(Pattern.class);
        assertThat(((HttpListener) httpListenerEntityV4).getCors()).isNotNull(); // Tested in CorsMapperTest

        var subscriptionEntityV4 = listenerEntityV4List.get(1);
        assertThat(subscriptionEntityV4.getType()).isEqualTo(ListenerType.SUBSCRIPTION);
        assertThat(subscriptionEntityV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(subscriptionEntityV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListener.getServers().size())
            .contains(httpListener.getServers().toArray());
        assertThat(subscriptionEntityV4).isNotNull().isInstanceOf(SubscriptionListener.class);

        var tcpListenerEntityV4 = listenerEntityV4List.get(2);
        assertThat(tcpListenerEntityV4.getType()).isEqualTo(ListenerType.TCP);
        assertThat(tcpListenerEntityV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(tcpListenerEntityV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListener.getServers().size())
            .contains(httpListener.getServers().toArray());
        assertThat(tcpListenerEntityV4).isNotNull().isInstanceOf(TcpListener.class);
    }

    @Test
    void shouldMapFromListenerEntityV4() {
        var httpListenerEntityV4 = ListenerFixtures.aModelHttpListener();
        var subscriptionListenerEntityV4 = ListenerFixtures.aModelSubscriptionListener();
        var tcpListenerEntotyV4 = ListenerFixtures.aModelTcpListener();

        var listenerV4List = listenerMapper.mapFromListenerEntityV4List(
            List.of(httpListenerEntityV4, subscriptionListenerEntityV4, tcpListenerEntotyV4)
        );

        assertThat(listenerV4List).isNotNull();
        assertThat(listenerV4List).asList().hasSize(3);

        var httpListenerV4 = listenerV4List.get(0).getHttpListener();
        assertThat(httpListenerV4.getType()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.ListenerType.HTTP);
        assertThat(httpListenerV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(httpListenerV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListenerEntityV4.getServers().size())
            .contains(httpListenerEntityV4.getServers().toArray());
        assertThat(httpListenerV4.getPaths()).isNotNull().asList().hasSize(1);
        assertThat(httpListenerV4.getPaths().get(0)).isNotNull();
        assertThat(httpListenerV4.getPaths().get(0).getHost()).isEqualTo(httpListenerEntityV4.getPaths().get(0).getHost());
        assertThat(httpListenerV4.getPaths().get(0).getPath()).isEqualTo(httpListenerEntityV4.getPaths().get(0).getPath());
        assertThat(httpListenerV4.getPaths().get(0).getOverrideAccess())
            .isEqualTo(httpListenerEntityV4.getPaths().get(0).isOverrideAccess());
        assertThat(httpListenerV4.getPathMappings()).isEqualTo(new ArrayList<>(httpListenerEntityV4.getPathMappings()));
        assertThat(httpListenerV4.getCors()).isNotNull(); // Tested in CorsMapperTest

        var subscriptionListenerV4 = listenerV4List.get(1).getSubscriptionListener();
        assertThat(subscriptionListenerV4.getType()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.ListenerType.SUBSCRIPTION);
        assertThat(subscriptionListenerV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(subscriptionListenerV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListenerEntityV4.getServers().size())
            .contains(httpListenerEntityV4.getServers().toArray());

        var tcpListenerV4 = listenerV4List.get(2).getTcpListener();
        assertThat(tcpListenerV4.getType()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.ListenerType.TCP);
        assertThat(tcpListenerV4.getEntrypoints()).isNotNull(); // Tested in EntrypointMapperTest
        assertThat(tcpListenerV4.getServers())
            .isNotNull()
            .asList()
            .hasSize(httpListenerEntityV4.getServers().size())
            .contains(httpListenerEntityV4.getServers().toArray());
    }
}
