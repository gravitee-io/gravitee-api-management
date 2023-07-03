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
package io.gravitee.repository.bridge.server.version;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.repository.bridge.server.utils.VersionUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VersionHandlerTest {

    private VersionHandler cut = new VersionHandler();
    private WebClient client;

    @BeforeEach
    public void beforeEach(Vertx vertx, VertxTestContext testContext) throws Exception {
        cut = spy(new VersionHandler());
        int randomPort = getRandomPort();
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(randomPort));

        vertx.deployVerticle(
            new AbstractVerticle() {
                @Override
                public void start() {
                    Router router = Router.router(vertx);
                    router.route().handler(cut);
                    router.get("/_test_version").handler(RoutingContext::end);
                    vertx.createHttpServer().requestHandler(router).listen(randomPort);
                }
            },
            testContext.succeedingThenComplete()
        );
    }

    private static Stream<Arguments> provideMatchingVersions() {
        return Stream.of(
            // server 1.x.x and client 1.x.x
            Arguments.of("1.0.0", "1.0.0"),
            Arguments.of("1.0.0", "1.1.1"),
            Arguments.of("1.1.1", "1.0.0"),
            // server 3.x.x and client 3.x.x
            Arguments.of("3.0.0", "3.0.0"),
            Arguments.of("3.0.0", "3.1.1"),
            Arguments.of("3.1.1", "3.0.0"),
            // server 4.x.x and client 4.x.x
            Arguments.of("4.0.0", "4.0.0"),
            Arguments.of("4.0.0", "4.1.1"),
            Arguments.of("4.1.1", "4.0.0"),
            // server 4.x.x and client 3.x.x
            Arguments.of("4.0.0", "3.0.0"),
            Arguments.of("4.0.0", "3.1.1"),
            Arguments.of("4.1.1", "3.0.0"),
            // server 3.x.x and client 4.x.x
            Arguments.of("3.0.0", "4.0.0"),
            Arguments.of("3.0.0", "4.1.1"),
            Arguments.of("3.1.1", "4.0.0")
        );
    }

    @ParameterizedTest(name = "server={0} client={1}")
    @MethodSource("provideMatchingVersions")
    void should_return_200_success_with_matching_versions(
        final String serverVersion,
        final String clientVersion,
        final VertxTestContext testContext
    ) {
        doAnswer(invocation -> {
                try (MockedStatic<VersionUtils> mockedStatic = mockStatic(VersionUtils.class)) {
                    mockedStatic.when(() -> VersionUtils.parse(serverVersion)).thenCallRealMethod();
                    mockedStatic.when(() -> VersionUtils.parse(clientVersion)).thenCallRealMethod();
                    mockedStatic.when(VersionUtils::nodeVersion).thenAnswer(nodeVersionInvocation -> VersionUtils.parse(serverVersion));
                    return invocation.callRealMethod();
                }
            })
            .when(cut)
            .handle(any());
        client
            .get("/_test_version")
            .putHeader(HttpHeaders.USER_AGENT, "gio-client-bridge/" + clientVersion)
            .expect(ResponsePredicate.SC_OK)
            .send()
            .onComplete(testContext.succeedingThenComplete())
            .onFailure(testContext::failNow);
    }

    private static Stream<Arguments> provideWrongMatchingVersions() {
        return Stream.of(
            // server 1.x.x and client empty
            Arguments.of("1.0.0", ""),
            Arguments.of("1.0.0", null),
            // server 1.x.x and client 3.x.x
            Arguments.of("1.0.0", "3.0.0"),
            Arguments.of("1.0.0", "3.1.1"),
            Arguments.of("1.1.1", "3.0.0"),
            // server 3.x.x and client 1.x.x
            Arguments.of("3.0.0", "1.0.0"),
            Arguments.of("3.0.0", "1.1.1"),
            Arguments.of("3.1.1", "1.0.0"),
            // server 1.x.x and client 4.x.x
            Arguments.of("1.0.0", "4.0.0"),
            Arguments.of("1.0.0", "4.1.1"),
            Arguments.of("1.1.1", "4.0.0"),
            // server 4.x.x and client 1.x.x
            Arguments.of("4.0.0", "1.0.0"),
            Arguments.of("4.0.0", "1.1.1"),
            Arguments.of("4.1.1", "1.0.0")
        );
    }

    @ParameterizedTest(name = "server={0} client={1}")
    @MethodSource("provideWrongMatchingVersions")
    void should_return_400_bad_request_with_wrong_matching_version(
        final String serverVersion,
        final String clientVersion,
        final VertxTestContext testContext
    ) {
        doAnswer(invocation -> {
                try (MockedStatic<VersionUtils> mockedStatic = mockStatic(VersionUtils.class)) {
                    mockedStatic.when(() -> VersionUtils.parse(serverVersion)).thenCallRealMethod();
                    mockedStatic.when(() -> VersionUtils.parse(clientVersion)).thenCallRealMethod();
                    mockedStatic.when(VersionUtils::nodeVersion).thenAnswer(nodeVersionInvocation -> VersionUtils.parse(serverVersion));
                    return invocation.callRealMethod();
                }
            })
            .when(cut)
            .handle(any());
        client
            .get("/_test_version")
            .putHeader(HttpHeaders.USER_AGENT, "gio-client-bridge/" + clientVersion)
            .expect(ResponsePredicate.SC_BAD_REQUEST)
            .send()
            .onComplete(testContext.succeedingThenComplete())
            .onFailure(testContext::failNow);
    }

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
