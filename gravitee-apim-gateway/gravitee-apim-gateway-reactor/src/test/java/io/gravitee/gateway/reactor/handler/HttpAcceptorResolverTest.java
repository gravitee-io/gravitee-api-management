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
package io.gravitee.gateway.reactor.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactor.handler.impl.DefaultAcceptorResolver;
import io.gravitee.gateway.reactor.handler.impl.HttpAcceptorHandlerComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpAcceptorResolverTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpAcceptorResolverTest.class);

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @InjectMocks
    private DefaultAcceptorResolver handlerResolver;

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private HttpHeaders httpHeaders;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(context.request()).thenReturn(request);
        when(request.headers()).thenReturn(httpHeaders);
    }

    @After
    public void tearDown() {
        reactorHandlerRegistry.clear();
    }

    @Test
    public void test_uniqContextPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));

        final ConcurrentSkipListSet<HttpAcceptorHandler> httpAcceptorHandlers = new ConcurrentSkipListSet<>(
            new HttpAcceptorHandlerComparator()
        );
        httpAcceptorHandlers.addAll(Collections.singletonList(acceptorHandler));
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);
        when(request.path()).thenReturn("/teams");

        assertEquals(acceptorHandler, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), eq(acceptorHandler));
    }

    @Test
    public void test_uniqContextPath_unknownRequestPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));

        final ConcurrentSkipListSet<HttpAcceptorHandler> httpAcceptorHandlers = new ConcurrentSkipListSet<>(
            new HttpAcceptorHandlerComparator()
        );
        httpAcceptorHandlers.addAll(Collections.singletonList(acceptorHandler));
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);
        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_validRequestPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler1 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));
        DummyReactorandlerHttpAcceptorHandler acceptorHandler2 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams2"));

        final List<HttpAcceptorHandler> httpAcceptorHandlers = Arrays.asList(acceptorHandler1, acceptorHandler2);
        httpAcceptorHandlers.sort(new HttpAcceptorHandlerComparator());
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);
        when(request.path()).thenReturn("/teams");

        assertEquals(acceptorHandler1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), eq(acceptorHandler1));
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler1 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));
        DummyReactorandlerHttpAcceptorHandler acceptorHandler2 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams2"));

        final List<HttpAcceptorHandler> httpAcceptorHandlers = Arrays.asList(acceptorHandler1, acceptorHandler2);
        httpAcceptorHandlers.sort(new HttpAcceptorHandlerComparator());
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);

        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath2() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler1 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));
        DummyReactorandlerHttpAcceptorHandler acceptorHandler2 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams2"));

        final ConcurrentSkipListSet<HttpAcceptorHandler> httpAcceptorHandlers = new ConcurrentSkipListSet<>(
            new HttpAcceptorHandlerComparator()
        );
        httpAcceptorHandlers.addAll(Arrays.asList(acceptorHandler1, acceptorHandler2));
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);

        when(request.path()).thenReturn("/teamss");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_extraSeparatorRequestPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler1 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));
        DummyReactorandlerHttpAcceptorHandler acceptorHandler2 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams2"));

        final List<HttpAcceptorHandler> httpAcceptorHandlers = Arrays.asList(acceptorHandler1, acceptorHandler2);
        httpAcceptorHandlers.sort(new HttpAcceptorHandlerComparator());
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);

        when(request.path()).thenReturn("/teams/");

        assertEquals(acceptorHandler1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), eq(acceptorHandler1));
    }

    @Test
    public void test_multipleContextPath_extraSeparatorUnknownRequestPath() {
        DummyReactorandlerHttpAcceptorHandler acceptorHandler1 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams"));
        DummyReactorandlerHttpAcceptorHandler acceptorHandler2 = new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("/teams2"));

        final List<HttpAcceptorHandler> httpAcceptorHandlers = Arrays.asList(acceptorHandler1, acceptorHandler2);
        httpAcceptorHandlers.sort(new HttpAcceptorHandlerComparator());
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);

        when(request.path()).thenReturn("/teamss/");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultAcceptorResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleVhosts() {
        final List<HttpAcceptorHandler> noHosts = new ArrayList<>();
        final List<HttpAcceptorHandler> withHostAndPathABC = new ArrayList<>();
        final List<HttpAcceptorHandler> withHostAndNotPathABC = new ArrayList<>();

        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/a")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/b")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/d")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/e")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/f")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/c1/sub")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/c1/sub2")));
        noHosts.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost(null, "/b/c1/sub3")));

        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api1.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api2.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api3.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api4.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("apiX.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api10.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api11.gravitee.io", "/a/b/c")));

        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api.gravitee.io", "/a/b/a")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api1.gravitee.io", "/a/b/b")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api2.gravitee.io", "/a/b/d")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api3.gravitee.io", "/a/b/e")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api4.gravitee.io", "/a/b/f")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("apiX.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api10.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("api11.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerHttpAcceptorHandler(new VirtualHost("apispecial.gravitee.io", "/a/b/special")));

        final List<HttpAcceptorHandler> httpAcceptorHandlers = new ArrayList<>();
        httpAcceptorHandlers.addAll(noHosts);
        httpAcceptorHandlers.addAll(withHostAndPathABC);
        httpAcceptorHandlers.addAll(withHostAndNotPathABC);
        httpAcceptorHandlers.sort(new HttpAcceptorHandlerComparator());
        when(reactorHandlerRegistry.getHttpAcceptorHandlers()).thenReturn(httpAcceptorHandlers);

        // Cases without host.
        for (final HttpAcceptorHandler expected : noHosts) {
            logger.info("Test case: resolve for host [{}] with unknown path.", expected.host());
            final List<String> pathsShouldNotMatch = Arrays.asList("/unknown", "/A/b/C", "/a/b/c", "/a/b/c/");

            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // "/A/b/C"  -> path with this specific case not declared
            // "/a/b/c" and "/a/b/c/" -> declared but not matching with query without the appropriate host.
            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Trying to resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(null);
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because they are exposed in path mode (no host).
            final List<String> pathsShouldMatch = Arrays.asList(
                expected.path(),
                expected.path() + "/",
                expected.path() + "/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: to resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(null);
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }

        // Cases with host and path "/a/b/c"
        for (final HttpAcceptorHandler expected : withHostAndPathABC) {
            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // "/A/b/C"  -> path with this specific case not declared
            // "/a/b/special" -> declared but not matching for hosts where path is "/a/b/c"
            final List<String> pathsShouldNotMatch = Arrays.asList("/unknown", "/A/b/C", "/a/b/special");

            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because they all starts with "/a/b/c".
            final List<String> pathsShouldMatch = Arrays.asList(
                "/a/b/c",
                "/a/b/c/",
                "/a/b/c/sub",
                "/a/b/c/sub/",
                "/a/b/c/sub/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }

        // Cases with host and path NOT "/a/b/c"
        for (final HttpAcceptorHandler expected : withHostAndNotPathABC) {
            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // All other paths are either on all host ('*') either for an host which is also exposing the paths we are testing (ex: "api1.gravitee.io" exposes multiples paths).
            final List<String> pathsShouldNotMatch = Collections.singletonList("/unknown");

            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because
            final List<String> pathsShouldMatch = Arrays.asList(
                expected.path(),
                expected.path() + "/",
                expected.path() + "/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }
    }

    private class DummyReactorandlerHttpAcceptorHandler implements HttpAcceptorHandler {

        private final HttpAcceptor httpAcceptor;

        public DummyReactorandlerHttpAcceptorHandler(HttpAcceptor httpAcceptor) {
            this.httpAcceptor = httpAcceptor;
        }

        @Override
        public <T extends ReactorHandler> T target() {
            return null;
        }

        @Override
        public ExecutionMode executionMode() {
            return null;
        }

        @Override
        public String path() {
            return httpAcceptor.path();
        }

        @Override
        public String host() {
            return httpAcceptor.host();
        }

        @Override
        public int priority() {
            return httpAcceptor.priority();
        }

        @Override
        public boolean accept(Request request) {
            return httpAcceptor.accept(request);
        }

        @Override
        public boolean accept(String host, String path) {
            return httpAcceptor.accept(host, path);
        }
    }
}
