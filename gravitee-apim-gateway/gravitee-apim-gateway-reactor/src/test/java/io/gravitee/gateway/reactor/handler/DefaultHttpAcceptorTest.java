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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultHttpAcceptorTest {

    public static final String SERVER_ID = "serverId";

    @Test
    void should_accept_with_matching_path() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor("/test");
        assertThat(cut.accept("localhost", "/test", SERVER_ID)).isTrue();
    }

    @Test
    void should_not_accept_with_non_matching_path() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor("/test");
        assertThat(cut.accept("localhost", "/not_matching", SERVER_ID)).isFalse();
    }

    @Test
    void should_accept_with_matching_path_and_server() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor(null, "/test", List.of("a", "b", SERVER_ID));
        assertThat(cut.accept("localhost", "/test", SERVER_ID)).isTrue();
    }

    @Test
    void should_not_accept_with_matching_path_and_not_matching_server() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor(null, "/test", List.of("a", "b", "c"));
        assertThat(cut.accept("localhost", "/test", SERVER_ID)).isFalse();
    }

    @Test
    void should_accept_with_matching_path_and_server_and_host() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor(
            "localhost",
            "/test",
            mock(ReactorHandler.class),
            List.of("a", "b", SERVER_ID)
        );
        assertThat(cut.accept("localhost", "/test", SERVER_ID)).isTrue();
    }

    @Test
    void should_not_accept_with_matching_path_and_server_and_not_matching_host() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor(
            "localhost",
            "/test",
            mock(ReactorHandler.class),
            List.of("a", "b", SERVER_ID)
        );
        assertThat(cut.accept("other", "/test", SERVER_ID)).isFalse();
    }

    @Test
    void should_not_accept_with_matching_path_and_server_null() {
        final DefaultHttpAcceptor cut = new DefaultHttpAcceptor(
            "localhost",
            "/test",
            mock(ReactorHandler.class),
            List.of("a", "b", SERVER_ID)
        );
        assertThat(cut.accept("localhost", "/test", null)).isFalse();
    }
}
