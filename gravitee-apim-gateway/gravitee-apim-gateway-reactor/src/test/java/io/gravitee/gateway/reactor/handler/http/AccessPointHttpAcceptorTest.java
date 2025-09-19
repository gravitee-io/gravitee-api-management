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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.gateway.reactor.accesspoint.AccessPointEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccessPointHttpAcceptorTest {

    private EventManager eventManager = new EventManagerImpl();

    final HttpAcceptorFactory httpAcceptorFactory = new HttpAcceptorFactory(false);

    @Mock
    private ReactorHandler reactorHandler;

    public static final String ENV_ID = "environmentId";
    public static final String SERVER_ID = "serverId";

    @Nested
    class WithoutAccessPointTest {

        @Test
        void should_accept_but_with_matching_path() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(),
                "/test",
                null,
                List.of()
            );
            assertThat(cut.accept("localhost", "/test", SERVER_ID)).isTrue();
        }

        @Test
        void should_not_accept_with_non_matching_path() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(),
                "/test",
                reactorHandler,
                List.of()
            );

            assertThat(cut.accept("localhost", "/not_matching", SERVER_ID)).isFalse();
        }

        @Test
        void should_accept_with_matching_path_and_server() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );
            assertThat(cut.accept("localhost", "/test", SERVER_ID)).isTrue();
        }

        @Test
        void should_not_accept_with_matching_path_and_not_matching_server() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(),
                "/test",
                reactorHandler,
                List.of("a", "b", "c")
            );

            assertThat(cut.accept("localhost", "/test", SERVER_ID)).isFalse();
        }
    }

    @Nested
    class WithSingleAccessPointsTest {

        @Test
        void should_accept_with_matching_path_and_server_and_host_and_target() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id")
                        .environmentId(ENV_ID)
                        .host("host")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id")
                        .environmentId(ENV_ID)
                        .host("host")
                        .target(ReactableAccessPoint.Target.TCP_GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("host", "/test", SERVER_ID)).isTrue();
        }

        @Test
        void should_not_accept_with_matching_path_and_server_and_not_matching_host() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(ReactableAccessPoint.builder().id("id").environmentId(ENV_ID).host("host").build()),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("other", "/test", SERVER_ID)).isFalse();
        }

        @Test
        void should_not_accept_with_matching_path_and_host_and_server_null() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(ReactableAccessPoint.builder().id("id").environmentId(ENV_ID).host("host").build()),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("host", "/test", null)).isFalse();
        }

        @Test
        void should_return_single_host_and_path() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id")
                        .environmentId(ENV_ID)
                        .host("host")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            assertThat(cut.host()).isEqualTo("host");
            assertThat(cut.path()).isEqualTo("/test/");
        }
    }

    @Nested
    class WithMultipleAccessPointsTest {

        @Test
        void should_accept_with_matching_path_and_server_and_host_and_target() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id1")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id2")
                        .environmentId(ENV_ID)
                        .host("host2")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id3")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.TCP_GATEWAY)
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id4")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.KAFKA_GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("host1", "/test", SERVER_ID)).isTrue();
        }

        @Test
        void should_not_accept_with_matching_path_and_server_and_not_matching_host() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id1")
                        .environmentId(ENV_ID)
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .host("host1")
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id2")
                        .environmentId(ENV_ID)
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .host("host2")
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("other", "/test", SERVER_ID)).isFalse();
        }

        @Test
        void should_not_accept_with_matching_path_and_host_and_server_null() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder().id("id1").environmentId(ENV_ID).host("host1").build(),
                    ReactableAccessPoint.builder().id("id2").environmentId(ENV_ID).host("host2").build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.accept("host1", "/test", null)).isFalse();
        }

        @Test
        void should_return_first_host_and_path() {
            final AccessPointHttpAcceptor cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id1")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build(),
                    ReactableAccessPoint.builder()
                        .id("id2")
                        .environmentId(ENV_ID)
                        .host("host2")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );

            assertThat(cut.innerHttpsAcceptors()).hasSize(2);
            assertThat(cut.host()).isEqualTo("host1");
            assertThat(cut.path()).isEqualTo("/test/");
        }
    }

    @Nested
    class OnAccessPointEventTest {

        private AccessPointHttpAcceptor cut;

        @BeforeEach
        public void beforeEach() {
            cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id1")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );
        }

        @Test
        void should_add_new_http_acceptors_on_proper_environmentId() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            eventManager.publishEvent(
                AccessPointEvent.DEPLOY,
                ReactableAccessPoint.builder()
                    .id("id")
                    .host("host")
                    .environmentId(ENV_ID)
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(2);
        }

        @Test
        void should_not_add_new_http_acceptors_on_wrong_environmentId() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            eventManager.publishEvent(
                AccessPointEvent.DEPLOY,
                ReactableAccessPoint.builder()
                    .id("id")
                    .host("host")
                    .environmentId("other-env")
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
        }

        @Test
        void should_not_add_new_http_acceptors_on_wrong_target() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            eventManager.publishEvent(
                AccessPointEvent.DEPLOY,
                ReactableAccessPoint.builder()
                    .id("id")
                    .host("host")
                    .environmentId("other-env")
                    .target(ReactableAccessPoint.Target.KAFKA_GATEWAY)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
        }

        @Test
        void should_remove_http_acceptors_on_proper_environmentId() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            eventManager.publishEvent(
                AccessPointEvent.UNDEPLOY,
                ReactableAccessPoint.builder()
                    .id("id1")
                    .host("host1")
                    .environmentId(ENV_ID)
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).isEmpty();
        }

        @Test
        void should_not_remove_http_acceptors_on_wrong_environmentId() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            eventManager.publishEvent(
                AccessPointEvent.UNDEPLOY,
                ReactableAccessPoint.builder()
                    .id("id1")
                    .host("host1")
                    .environmentId("other-env")
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
        }
    }

    @Nested
    class ClearTest {

        private AccessPointHttpAcceptor cut;

        @BeforeEach
        public void beforeEach() {
            cut = new AccessPointHttpAcceptor(
                eventManager,
                httpAcceptorFactory,
                ENV_ID,
                List.of(
                    ReactableAccessPoint.builder()
                        .id("id1")
                        .environmentId(ENV_ID)
                        .host("host1")
                        .target(ReactableAccessPoint.Target.GATEWAY)
                        .build()
                ),
                "/test",
                reactorHandler,
                List.of("a", "b", SERVER_ID)
            );
        }

        @Test
        void should_not_add_new_http_acceptors_after_clear() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            cut.clear();
            eventManager.publishEvent(
                AccessPointEvent.DEPLOY,
                ReactableAccessPoint.builder()
                    .id("id")
                    .host("host")
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .environmentId(ENV_ID)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
        }

        @Test
        void should_not_remove_http_acceptors_after_clear() {
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
            cut.clear();
            eventManager.publishEvent(
                AccessPointEvent.UNDEPLOY,
                ReactableAccessPoint.builder()
                    .id("id1")
                    .host("host1")
                    .target(ReactableAccessPoint.Target.GATEWAY)
                    .environmentId(ENV_ID)
                    .build()
            );
            assertThat(cut.innerHttpsAcceptors()).hasSize(1);
        }
    }
}
